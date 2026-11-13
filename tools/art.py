"""
Shrinks the pictures drawn by tools/gen.py (art/items, art/icons, art/places, art/room) into the sprites the app
reads, and pastes figures drawn anew into the pet sheets.

    python tools/art.py            # everything
    python tools/art.py items      # one group: items, icons, places, room, pets

Each group becomes one atlas, app/src/main/resources/art/<group>.bin, read by ui/art/Atlas.kt: an int count, then
for each sprite its key (Java modified UTF-8), width, height and ARGB pixels, all big-endian. A preview of every
group lands in art/preview/<group>.png.
"""
import os
import struct
import sys

import numpy as np
from PIL import Image
from scipy import ndimage

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cut  # noqa: E402  (the crisp shrink, the outline and the transparency finder)

ROOT = cut.ROOT
ART = os.path.join(ROOT, "art")
OUT = os.path.join(ROOT, "app", "src", "main", "resources", "art")
PREVIEW = os.path.join(ART, "preview")

ITEM = 24       # Items.S and Icons.S
PLACE = (96, 42)
NOOK = (192, 224)
# The nook's bands: (top, bottom) rows in the app, and the share of the drawn room each is cut from, picked by eye.
ROOM_BANDS = {
    "beam": ((0, 15), (0.0, 0.07)),
    "wall": ((15, 152), (0.07, 0.68)),
    "skirting": ((152, 176), (0.68, 0.79)),
    "floor": ((176, 224), (0.79, 1.0)),
}
ROOM_CUT = os.path.join(ART, "room", "bands.txt")

# Figures drawn anew for a kind's sheet: (sheet, new figure, which figure it replaces, its height in sheet pixels).
PETS = {
    "fox": ("fox_lie", 1, 520),
}


def figure(path):
    """The drawn thing alone, cropped to what is solid."""
    im, solid = cut.load(path)
    ys = np.nonzero(solid.any(axis=1))[0]
    xs = np.nonzero(solid.any(axis=0))[0]
    im = im[ys[0]:ys[-1] + 1, xs[0]:xs[-1] + 1].copy()
    im[..., 3] = np.where(solid[ys[0]:ys[-1] + 1, xs[0]:xs[-1] + 1], 255, 0)
    return im


def fit(im, w, h, pad=1, colors=40, ground=False):
    """The figure shrunk crisply into a w x h box with [pad] free round it, centred (on the ground when asked)."""
    ph, pw = im.shape[:2]
    k = min((w - 2 * pad) / pw, (h - 2 * pad) / ph)
    sw, sh = max(1, round(pw * k)), max(1, round(ph * k))
    small = cut.crisp(im, sw, sh, colors)
    out = np.zeros((h, w, 4), np.uint8)
    ox = (w - sw) // 2
    oy = h - pad - sh if ground else (h - sh) // 2
    out[oy:oy + sh, ox:ox + sw] = small
    return cut.outline(out)


def scene(path, w, h, colors=64):
    """A full-bleed picture cropped to w:h about its middle and shrunk crisply."""
    im = np.array(Image.open(path).convert("RGBA"))
    im[..., 3] = 255
    ph, pw = im.shape[:2]
    if pw / ph > w / h:
        cw = round(ph * w / h); x0 = (pw - cw) // 2; im = im[:, x0:x0 + cw]
    else:
        ch = round(pw * h / w); y0 = (ph - ch) // 2; im = im[y0:y0 + ch]
    return cut.crisp(im, w, h, colors)


def write(group, sprites):
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, group + ".bin"), "wb") as f:
        f.write(struct.pack(">i", len(sprites)))
        for key, a in sprites.items():
            k = key.encode("utf-8")
            f.write(struct.pack(">H", len(k)) + k)
            h, w = a.shape[:2]
            f.write(struct.pack(">ii", w, h))
            argb = (a[..., 3].astype(np.uint32) << 24) | (a[..., 0].astype(np.uint32) << 16) | (a[..., 1].astype(np.uint32) << 8) | a[..., 2].astype(np.uint32)
            f.write(argb.astype(">u4").tobytes())
    # A preview: every sprite at 4x on paper.
    os.makedirs(PREVIEW, exist_ok=True)
    z = 4
    cols = max(1, min(len(sprites), 10 if max(a.shape[1] for a in sprites.values()) <= 32 else 3))
    cw = max(a.shape[1] for a in sprites.values()) + 2
    ch = max(a.shape[0] for a in sprites.values()) + 2
    rows = (len(sprites) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cw * z, rows * ch * z), (243, 232, 207, 255))
    for i, a in enumerate(sprites.values()):
        im = Image.fromarray(a, "RGBA").resize((a.shape[1] * z, a.shape[0] * z), Image.NEAREST)
        sheet.alpha_composite(im, ((i % cols) * cw * z + z, (i // cols) * ch * z + z))
    sheet.save(os.path.join(PREVIEW, group + ".png"))
    print(group, len(sprites), "sprites")


def pngs(group):
    d = os.path.join(ART, group)
    return sorted(f[:-4] for f in os.listdir(d) if f.endswith(".png")) if os.path.isdir(d) else []


def items():
    write("items", {k: fit(figure(os.path.join(ART, "items", k + ".png")), ITEM, ITEM, ground=True) for k in pngs("items")})


def icons():
    write("icons", {k: fit(figure(os.path.join(ART, "icons", k + ".png")), ITEM, ITEM) for k in pngs("icons")})


def places():
    write("places", {k: scene(os.path.join(ART, "places", k + ".png"), *PLACE) for k in pngs("places")})


def room():
    path = os.path.join(ART, "room", "room.png")
    if not os.path.exists(path):
        return
    bands = dict(ROOM_BANDS)
    # Shares picked by eye for the room as drawn: "name top bottom" per line.
    if os.path.exists(ROOM_CUT):
        for line in open(ROOM_CUT, encoding="utf-8"):
            p = line.split()
            if len(p) == 3 and not line.startswith("#"):
                bands[p[0]] = (bands[p[0]][0], (float(p[1]), float(p[2])))
    im = np.array(Image.open(path).convert("RGBA"))
    im[..., 3] = 255
    ph, pw = im.shape[:2]
    out = {}
    for name, ((t, b), (s0, s1)) in bands.items():
        part = im[round(ph * s0):round(ph * s1)]
        out[name] = cut.crisp(part, NOOK[0], b - t, 64)
    write("room", out)


def pets():
    for sp, (new, which, height) in PETS.items():
        src = os.path.join(ART, "pets_new", new + ".png")
        if not os.path.exists(src):
            continue
        orig = os.path.join(ART, "sheets", sp + ".orig.png")
        sheet_path = os.path.join(ART, "sheets", sp + ".png")
        if not os.path.exists(orig):
            os.replace(sheet_path, orig)
        im, solid = cut.load(orig)
        runs = cut.figures(solid)
        s, e = runs[which]
        m = solid[:, s:e]
        ys = np.nonzero(m.any(axis=1))[0]
        bottom = ys[-1] + 1
        cx = (s + e) // 2
        # Out with the old figure, in with the new one standing on the same ground.
        im[:, s:e, 3] = 0
        fig = figure(src)
        k = height / fig.shape[0]
        fig = np.array(Image.fromarray(fig, "RGBA").resize((round(fig.shape[1] * k), height), Image.NEAREST))
        x0 = cx - fig.shape[1] // 2
        y0 = bottom - fig.shape[0]
        out = Image.fromarray(im, "RGBA")
        out.alpha_composite(Image.fromarray(fig, "RGBA"), (max(0, x0), max(0, y0)))
        out.save(sheet_path)
        print(sp, "sheet: figure", which, "drawn anew from", new)


def main():
    groups = sys.argv[1:] or ["pets", "items", "icons", "places", "room"]
    for g in groups:
        globals()[g]()


if __name__ == "__main__":
    main()
