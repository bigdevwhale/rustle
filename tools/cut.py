"""
Cuts the drawn pet sheets (art/sheets/<species>.png: him sitting, then curled up asleep, made with image
generation through Codex) into 48x44 sprites, and works out what the app needs to dress and recolour them.
The cropping and the crisp shrink come from Songling's tools/cut.py.

    python tools/cut.py [species ...]

For each kind it writes app/src/main/resources/pets/<species>.bin (read by ui/art/Sprites.kt) and a line
in pets/faces.txt; art/pets/<species>.png shows the cut sprites, and app/build/art/sheets.png boxes the eyes
and tints the fur that takes the owner's colour.

The .bin holds two sprites, sitting and lying, each 48x44: the ARGB pixels, then one byte a pixel for what
it is (0 keep, 1 fur, 2 second colour, 3 an eye), then one signed byte a pixel for how light the fur is
there against its own colour (-127 deep shade .. 0 the colour itself .. 127 the brightest light).
"""
import os
import struct
import sys

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "art", "sheets")
OUT = os.path.join(ROOT, "app", "src", "main", "resources", "pets")
PREVIEW = os.path.join(ROOT, "art", "pets")
DEBUG = os.path.join(ROOT, "app", "build", "art", "sheets.png")

# Name in the app, and the two colours the sheet was drawn in (Species.body / accent).
SPECIES = {
    "mouse": ("MOUSE", 0xC8946A, 0xF2DCC0),
    "hedgehog": ("HEDGEHOG", 0x8A6A52, 0xF0DCC0),
    "bunny": ("BUNNY", 0xE8DED0, 0xFFF4EC),
    "moth": ("MOTH", 0xB8A890, 0xF4ECDA),
    "fox": ("FOX", 0xD87A40, 0xFFF0E0),
    "owl": ("OWL", 0x9A7A5A, 0xF0E0C8),
    "cat": ("CAT", 0x8A8A92, 0xE8E8EE),
    "bat": ("BAT", 0x6A5A6A, 0xD8C8D0),
}
W, H = 48, 44
GROUND = 42           # the bottom row he stands on
MAX_W, MAX_H = 46, 36

# Eyes the finder gets wrong, by hand: (x0, y0, x1, y1) of each eye in sprite pixels.
EYES = {
    "fox": [(10, 21, 15, 25), (20, 21, 25, 25)],
    "owl": [(16, 18, 23, 25), (26, 18, 34, 25)],
    "bat": [(19, 26, 23, 29), (26, 26, 30, 29)],
}
# The closed eyes of the sleeping sprite, by hand (patterns round the eyes follow them).
LIE_EYES = {
    "mouse": [(14, 30, 19, 32), (25, 30, 30, 32)],
    "hedgehog": [(16, 34, 21, 36), (28, 34, 33, 36)],
    "bunny": [(15, 33, 20, 35), (24, 33, 29, 35)],
    "moth": [(19, 27, 23, 29), (26, 27, 30, 29)],
    "fox": [(17, 26, 22, 28), (25, 26, 30, 28)],
    "owl": [(16, 31, 23, 34), (27, 31, 33, 34)],
    "cat": [(11, 33, 16, 35), (27, 33, 32, 35)],
    "bat": [(17, 33, 21, 35), (25, 33, 29, 35)],
}
# Where the crown and the neck are, when the guess is off: (crown y, neck y).
BODY = {}


def lum(a):
    return (0.3 * a[..., 0] + 0.59 * a[..., 1] + 0.11 * a[..., 2]) / 255.0


def unchecker(rgb):
    """A transparency checkerboard (or a flat white) painted into the sheet: what joins the edge goes."""
    mn = rgb.min(axis=2).astype(int)
    mx = rgb.max(axis=2).astype(int)
    grey = (mn >= 200) & (mx - mn <= 22)
    lab, n = ndimage.label(grey)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    return ~np.isin(lab, list(edge))


def load(path):
    im = np.array(Image.open(path).convert("RGBA"))
    a = im[..., 3]
    if (a == 0).mean() > 0.05:
        solid = a >= 128
    else:
        solid = unchecker(im[..., :3])
    lab, n = ndimage.label(solid)
    sizes = ndimage.sum(solid, lab, range(1, n + 1))
    for i, s in enumerate(sizes, start=1):
        if s < 400:
            solid[lab == i] = False
    im[..., 3] = np.where(solid, 255, 0)
    return im, solid


def figures(solid, count=2):
    """Column ranges of the figures, left to right: runs of filled columns, the closest merged."""
    on = solid.any(axis=0)
    runs, x = [], 0
    while x < len(on):
        if on[x]:
            s = x
            while x < len(on) and on[x]:
                x += 1
            runs.append([s, x])
        else:
            x += 1
    while len(runs) > count:
        gaps = [runs[i + 1][0] - runs[i][1] for i in range(len(runs) - 1)]
        i = int(np.argmin(gaps))
        runs[i][1] = runs[i + 1][1]
        del runs[i + 1]
    return runs


def find_eyes(img, solid):
    """Each eye is a dark bead with a white shine in it. Returns two boxes (x0, y0, x1, y1) or None."""
    rgb = img[..., :3].astype(int)
    L = lum(rgb)
    area = solid.sum()
    white = solid & (rgb.min(axis=2) >= 225)
    dark = solid & (L < 0.32)
    lab, n = ndimage.label(white)
    cands = []
    for i, sl in enumerate(ndimage.find_objects(lab), start=1):
        m = lab == i
        a = m.sum()
        if not (area * 0.0002 <= a <= area * 0.012):
            continue
        r = max(3, int(np.sqrt(a)))
        ring = ndimage.binary_dilation(m, iterations=r) & ~m
        if dark[ring].mean() < 0.45:
            continue
        cy, cx = ndimage.center_of_mass(m)
        half = int(np.sqrt(a) * 2.6) + 4
        y0, y1 = max(0, int(cy) - half), min(img.shape[0], int(cy) + half)
        x0, x1 = max(0, int(cx) - half), min(img.shape[1], int(cx) + half)
        win = (dark | m)[y0:y1, x0:x1]
        wl, _ = ndimage.label(win)
        ids = set(np.unique(wl[(m[y0:y1, x0:x1])])) - {0}
        bead = np.isin(wl, list(ids))
        yy, xx = np.mgrid[y0:y1, x0:x1]
        bead &= (yy - cy) ** 2 + (xx - cx) ** 2 <= (np.sqrt(a) * 2.4) ** 2
        ys, xs = np.nonzero(bead)
        cands.append((a, cx, cy, (x0 + xs.min(), y0 + ys.min(), x0 + xs.max() + 1, y0 + ys.max() + 1)))
    best = None
    for i in range(len(cands)):
        for j in range(i + 1, len(cands)):
            a, b = cands[i], cands[j]
            size = np.sqrt(max(a[0], b[0]))
            if max(a[0], b[0]) / min(a[0], b[0]) > 2.6 or abs(a[2] - b[2]) > size * 2.0 or abs(a[1] - b[1]) < size * 2.5:
                continue
            # Eyes sit in the upper middle: prefer a big pair close to the middle.
            score = a[0] + b[0] - abs((a[1] + b[1]) / 2 - img.shape[1] / 2) * 0.5
            if best is None or score > best[0]:
                best = (score, sorted([a, b], key=lambda c: c[1]))
    return None if best is None else [c[3] for c in best[1]]


def crisp(part, w, h, colors=48):
    """Shrinks a drawn figure to w x h without blending: the figure is brought down to a small palette,
    then each sprite pixel takes the commonest palette colour in its block (the middle counting double)."""
    ph, pw = part.shape[:2]
    opaque = part[..., 3] >= 128
    pal = Image.fromarray(part[..., :3], "RGB").quantize(colors=colors, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    idx = np.array(pal)
    palette = np.array(pal.getpalette()[:colors * 3], np.uint8).reshape(-1, 3)
    out = np.zeros((h, w, 4), np.uint8)
    xs = np.linspace(0, pw, w + 1).astype(int)
    ys = np.linspace(0, ph, h + 1).astype(int)
    for j in range(h):
        for i in range(w):
            ys0, ys1, xs0, xs1 = ys[j], max(ys[j + 1], ys[j] + 1), xs[i], max(xs[i + 1], xs[i] + 1)
            o = opaque[ys0:ys1, xs0:xs1]
            if o.mean() < 0.45:
                continue
            block = idx[ys0:ys1, xs0:xs1]
            cy0, cy1 = (ys1 - ys0) // 4, ys1 - ys0 - (ys1 - ys0) // 4
            cx0, cx1 = (xs1 - xs0) // 4, xs1 - xs0 - (xs1 - xs0) // 4
            counts = np.bincount(block[o], minlength=colors).astype(float)
            inner = block[cy0:cy1, cx0:cx1][o[cy0:cy1, cx0:cx1]]
            if inner.size:
                counts += np.bincount(inner, minlength=colors)
            out[j, i, :3] = palette[int(np.argmax(counts))]
            out[j, i, 3] = 255
    return out


INK = np.array([26.0, 16.0, 24.0])


def outline(sprite):
    """An even dark edge: every pixel touching the outside is darkened."""
    a = sprite[..., 3] > 0
    edge = a & ndimage.binary_dilation(~a, iterations=1)
    rgb = sprite[..., :3].astype(float)
    L = lum(rgb)
    for y, x in zip(*np.nonzero(edge)):
        if L[y, x] > 0.16:
            sprite[y, x, :3] = (rgb[y, x] * 0.2 + INK * 0.8).astype(np.uint8)
    return sprite


def hexrgb(c):
    return np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], float)


def chroma(rgb):
    """Colour without its lightness: the share of each channel."""
    s = rgb.sum(axis=-1, keepdims=True)
    return rgb / np.maximum(s, 1)


def hsv(rgb):
    """Hue in degrees, saturation and value, per pixel."""
    r, g, b = rgb[..., 0] / 255.0, rgb[..., 1] / 255.0, rgb[..., 2] / 255.0
    mx, mn = np.maximum(np.maximum(r, g), b), np.minimum(np.minimum(r, g), b)
    d = np.maximum(mx - mn, 1e-6)
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) * 60
    return h, np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0), mx


def hdist(a, b):
    d = np.abs(a - b) % 360
    return np.minimum(d, 360 - d)


def classify(sprite, body, accent, keep=None):
    """Which pixels are fur (1) and which the second colour (2). Pixels in the fur's family of hues (or
    neutral, when the fur is) are split between the two colours by lightness and saturation, the centres
    settled by a few rounds of averaging; then lone pixels take their neighbours' side. The outline, eyes,
    pink noses, ears and blush, beaks and anything else of another hue stay as drawn."""
    rgb = sprite[..., :3].astype(float)
    a = sprite[..., 3] > 0
    L = lum(rgb)
    h, s, v = hsv(rgb)
    refs = []
    for c in (body, accent):
        ch, cs, cv = hsv(hexrgb(c)[None, None, :])
        refs.append([float(ch[0, 0]), float(cs[0, 0]), float(lum(hexrgb(c)))])
    edge = a & ndimage.binary_dilation(~a, iterations=1)
    cand = a & ~edge & (L > 0.13)
    if keep is not None:
        cand &= ~keep
    pink = (rgb[..., 0] - rgb[..., 1] > 45) & ((h > 325) | (h < 10)) & (s > 0.22)
    cand &= ~pink
    hb = refs[0][0]
    sb = refs[0][1]
    near_hue = (s < 0.16) | (hdist(h, hb) < 22) | ((hdist(h, refs[1][0]) < 28) & (s < 0.45))
    if sb < 0.16:
        near_hue = (s < 0.3) | (hdist(h, hb) < 35)
    cand &= near_hue & (np.abs(s - sb) < 0.32) | (cand & (s < 0.2) & (L > refs[1][2] - 0.2))
    cand &= a & ~edge & ~pink
    for _ in range(4):
        d = [((L - r[2]) / 0.18) ** 2 + ((s - r[1]) / 0.2) ** 2 for r in refs]
        cls = np.where(d[1] < d[0], 2, 1)
        for k in (1, 2):
            m = cand & (cls == k)
            if m.sum() > 10:
                refs[k - 1][1] = float(np.median(s[m]))
                refs[k - 1][2] = float(np.median(L[m]))
    out = np.where(cand, cls, 0).astype(np.uint8)
    # The in-between tones where fur meets the second colour (the fox's peach between orange and cream) are
    # neither: a pixel left out but ringed by fur, in a related hue, joins the nearer of the two.
    loose = a & ~edge & ~pink & (L > 0.25) & ((hdist(h, hb) < 45) | (hdist(h, refs[1][0]) < 45) | (s < 0.25))
    if keep is not None:
        loose &= ~keep
    d = [((L - r[2]) / 0.18) ** 2 + ((s - r[1]) / 0.2) ** 2 for r in refs]
    near = np.where(d[1] < d[0], 2, 1)
    for _ in range(3):
        n = ndimage.convolve((out > 0).astype(int), np.ones((3, 3)), mode="constant")
        grow = loose & (out == 0) & (n >= 4)
        if not grow.any():
            break
        out[grow] = near[grow]
    # Lone pixels take their neighbours' side.
    for _ in range(2):
        n1 = ndimage.convolve((out == 1).astype(int), np.ones((3, 3)), mode="constant")
        n2 = ndimage.convolve((out == 2).astype(int), np.ones((3, 3)), mode="constant")
        flip1 = (out == 1) & (n1 <= 2) & (n2 >= 5)
        flip2 = (out == 2) & (n2 <= 2) & (n1 >= 5)
        out[flip1] = 2
        out[flip2] = 1
    shade = np.zeros(a.shape, np.int8)
    for k in (1, 2):
        m = out == k
        ref = refs[k - 1][2]
        r = np.log(np.maximum(L[m], 0.02) / max(ref, 0.02)) / np.log(2.2)
        shade[m] = np.clip(np.round(r * 127), -127, 127).astype(np.int8)
    return out, shade


def place(im, solid, box, k):
    x0, y0, x1, y1 = box
    part = im[y0:y1, x0:x1].copy()
    part[..., 3] = np.where(solid[y0:y1, x0:x1], 255, 0)
    w, h = max(1, round((x1 - x0) * k)), max(1, round((y1 - y0) * k))
    arr = crisp(part, w, h)
    ox, oy = (W - w) // 2, GROUND + 1 - h
    sprite = np.zeros((H, W, 4), np.uint8)
    sprite[oy:oy + h, ox:ox + w] = arr
    return outline(sprite), part, (ox, oy)


def crown_and_neck(sprite, eyes):
    """The top of his head over the middle of his face (between the ears), and the row a scarf goes on."""
    a = sprite[..., 3] > 0
    cx = (eyes[0][0] + eyes[1][2]) // 2
    ey1 = max(eyes[0][3], eyes[1][3])
    col = a[:, cx - 1:cx + 2].all(axis=1)
    rows = np.nonzero(col)[0]
    crown = int(rows.min()) if len(rows) else 4
    bottom = int(np.nonzero(a.any(axis=1))[0].max())
    neck = min(bottom - 4, ey1 + max(4, (bottom - ey1) // 3))
    return crown, neck


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(PREVIEW, exist_ok=True)
    os.makedirs(os.path.dirname(DEBUG), exist_ok=True)
    names = sys.argv[1:] or [sp for sp in SPECIES if os.path.exists(os.path.join(SRC, sp + ".png"))]
    old = {}
    fp = os.path.join(OUT, "faces.txt")
    if os.path.exists(fp):
        for line in open(fp, encoding="utf-8"):
            if line.strip() and not line.startswith("#"):
                old[line.split()[0]] = line.strip()
    Z = 6
    sheet = Image.new("RGBA", (W * 3 * Z, H * max(1, len(names)) * Z), (30, 74, 58, 255))
    for row, sp in enumerate(names):
        key, body, accent = SPECIES[sp]
        im, solid = load(os.path.join(SRC, sp + ".png"))
        boxes = []
        for s, e in figures(solid):
            m = solid[:, s:e]
            ys = np.nonzero(m.any(axis=1))[0]
            xs = np.nonzero(m.any(axis=0))[0]
            boxes.append((s + xs[0], ys[0], s + xs[-1] + 1, ys[-1] + 1))
        if len(boxes) != 2:
            print(sp, "expected two figures, found", len(boxes), file=sys.stderr)
            continue
        k = min(min(MAX_W / (b[2] - b[0]), MAX_H / (b[3] - b[1])) for b in boxes)
        sit, part, (ox, oy) = place(im, solid, boxes[0], k)
        lie, _, _ = place(im, solid, boxes[1], k)
        eyes = EYES.get(sp)
        if eyes is None:
            found = find_eyes(part, part[..., 3] > 0)
            if not found:
                print(sp, "no eyes found", file=sys.stderr)
                continue
            eyes = [(int(ox + a * k), int(oy + b * k), int(round(ox + c * k)), int(round(oy + d * k))) for a, b, c, d in found]
        crown, neck = BODY.get(sp) or crown_and_neck(sit, eyes)
        blobs = []
        with open(os.path.join(OUT, sp + ".bin"), "wb") as f:
            for i, spr in enumerate((sit, lie)):
                keep = np.zeros((H, W), bool)
                if i == 0:
                    for x0, y0, x1, y1 in eyes:
                        keep[y0:y1, x0:x1] = True
                cls, shade = classify(spr, body, accent, keep)
                if i == 0:
                    # The eyes: everything inside each box but the fur and the outline (a coloured iris too).
                    a = spr[..., 3] > 0
                    edge = a & ndimage.binary_dilation(~a, iterations=1)
                    for x0, y0, x1, y1 in eyes:
                        for y in range(y0, y1):
                            for x in range(x0, x1):
                                if a[y, x] and not edge[y, x] and cls[y, x] == 0:
                                    cls[y, x] = 3
                argb = (spr[..., 3].astype(np.uint32) << 24) | (spr[..., 0].astype(np.uint32) << 16) | (spr[..., 1].astype(np.uint32) << 8) | spr[..., 2].astype(np.uint32)
                f.write(argb.astype(">u4").tobytes())
                f.write(cls.tobytes())
                f.write(shade.astype(np.int8).tobytes())
                blobs.append((spr, cls))
        # The fur around the eyes, to paint them shut with.
        rgb = sit[..., :3].astype(int)
        lid = []
        for x0, y0, x1, y1 in eyes:
            for y in range(max(0, y0 - 2), min(H, y1 + 2)):
                for x in range(max(0, x0 - 2), min(W, x1 + 2)):
                    if blobs[0][1][y, x] in (1, 2):
                        lid.append(tuple(rgb[y, x]))
        lc = max(set(lid), key=lid.count) if lid else (200, 150, 110)
        lidc = (lc[0] << 16) | (lc[1] << 8) | lc[2]
        old[key] = f"{key} " + " ".join(f"{a} {b} {c} {d}" for a, b, c, d in eyes) + f" {crown} {neck} {lidc:06X} " + " ".join(f"{a} {b} {c} {d}" for a, b, c, d in LIE_EYES[sp])
        Image.fromarray(np.concatenate([sit, lie], axis=1), "RGBA").resize((W * 2 * 4, H * 4), Image.NEAREST).save(os.path.join(PREVIEW, sp + ".png"))
        # Debug: sit, lie, and the sit with fur tinted green, the second colour blue, eyes magenta.
        for c, (spr, cls) in enumerate(blobs + [blobs[0]]):
            s = spr.copy()
            if c == 2:
                tint = {1: (60, 220, 90), 2: (80, 140, 255), 3: (255, 0, 255)}
                for v, col in tint.items():
                    m = cls == v
                    s[m, :3] = (s[m, :3] * 0.4 + np.array(col) * 0.6).astype(np.uint8)
                s[crown, :, :] = (255, 255, 0, 255)
                s[neck, :, :] = (0, 255, 255, 255)
            sheet.alpha_composite(Image.fromarray(s, "RGBA").resize((W * Z, H * Z), Image.NEAREST), (c * W * Z, row * H * Z))
        print(sp, "scale", round(k, 4), "eyes", eyes, "crown", crown, "neck", neck)
    with open(fp, "w", encoding="utf-8") as f:
        f.write("# kind  left eye x0 y0 x1 y1  right eye x0 y0 x1 y1  crown y  neck y  lid colour  asleep: left eye, right eye (written by tools/cut.py)\n")
        f.write("\n".join(old[k] for k in sorted(old)) + "\n")
    sheet.save(DEBUG)


if __name__ == "__main__":
    main()
