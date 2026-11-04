"""Draws the launcher icon and the notification icon as pixel art, written as vector drawables (and docs/logo.png).

python tools/icon.py

The picture is a 24×24 grid: the rustler's fluffy face peeking over a copper track on the green board.
Each run of same-coloured pixels in a row becomes one rectangle.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DRAW = ROOT / "app/src/main/res/drawable"

GRID = [
    "........................",
    "........................",
    "........................",
    "....kkk..........kkk....",
    "...kfffk........kfffk...",
    "...kfppfkkkkkkkkfppfk...",
    "...kfpffffffffffffpfk...",
    "....kfLLLLLLLLLLLLffk...",
    "...kfLLLffffffffLfffk...",
    "..kfLLffffffffffffffk...",
    "..kfLfffkkfffffkkfffk...",
    "..kfLfffkwffffffkwffdk..",
    ".kffffffkkfffffkkffffk..",
    ".kfffffffffppfffffffdk..",
    ".kfffffffffffffffffddk..",
    ".kffffffffcccccfffffdk..",
    "..kfffffcccccccccffdk...",
    "..kkfffffcccccccfffdk...",
    "...kkdfffffffffffddkk...",
    ".oooookkkkkkkkkkkkooooo.",
    ".OOOOOOOOOOOOOOOOOOOOOO.",
    "........................",
    "........................",
    "........................",
]
GRID = [r.ljust(24, ".")[:24] for r in GRID]
COLORS = {"k": "#FF2A1A10", "f": "#FFC8946A", "L": "#FFE8BC92", "d": "#FF8E5E3A", "p": "#FFE8A8A0",
          "w": "#FFFFFFFF", "c": "#FFF2DCC0", "o": "#FFE0954A", "O": "#FF8E5222"}
BG = "#FF173A2E"


def runs(grid):
    out = {}
    for y, row in enumerate(grid):
        x = 0
        while x < len(row):
            ch = row[x]
            if ch == ".":
                x += 1
                continue
            start = x
            while x < len(row) and row[x] == ch:
                x += 1
            out.setdefault(ch, []).append((start, y, x - start))
    return out


def paths(grid, scale, offset, colour=None, skip=""):
    out = []
    for ch, rs in runs(grid).items():
        if ch in skip:
            continue
        d = " ".join(f"M{offset + x * scale:.2f},{offset + y * scale:.2f}h{w * scale:.2f}v{scale:.2f}h{-w * scale:.2f}z" for x, y, w in rs)
        out.append(f'    <path android:fillColor="{colour or COLORS[ch]}" android:pathData="{d}" />')
    return out


def vector(size, view, body):
    return ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{size}dp"\n    android:height="{size}dp"\n    android:viewportWidth="{view}"\n    android:viewportHeight="{view}">\n'
            + "\n".join(body) + "\n</vector>\n")


def main():
    DRAW.mkdir(parents=True, exist_ok=True)
    scale = 66 / 24
    (DRAW / "ic_launcher_foreground.xml").write_text(vector(108, 108, paths(GRID, scale, 21)), encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_background.xml").write_text(vector(108, 108, [f'    <path android:fillColor="{BG}" android:pathData="M0,0h108v108h-108z" />']), encoding="utf-8", newline="\n")
    # The status bar icon: a white silhouette of the face, eyes cut out.
    stat = [row.replace("w", "e").replace("k", "f") for row in GRID[:19]]
    stat = [r if i < 10 or i > 12 else r for i, r in enumerate(stat)]
    sil = []
    for y, row in enumerate(stat):
        sil.append("".join("." if ch in ".e" or (GRID[y][x] == "k" and 10 <= y <= 12 and 8 <= x <= 17) else "f" for x, ch in enumerate(row)))
    (DRAW / "ic_stat.xml").write_text(vector(24, 24, paths(sil, 1, 0, colour="#FFFFFFFF")), encoding="utf-8", newline="\n")
    try:
        from PIL import Image

        def rgba(c):
            return tuple(int(c[i:i + 2], 16) for i in (3, 5, 7)) + (255,)
        img = Image.new("RGBA", (24, 24), rgba(BG))
        for y, row in enumerate(GRID):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x, y), rgba(COLORS[ch]))
        (ROOT / "docs").mkdir(exist_ok=True)
        img.resize((512, 512), Image.NEAREST).save(ROOT / "docs/logo.png")
    except ImportError:
        pass


if __name__ == "__main__":
    main()
