"""Transparent 60-frame preview of the regenerated fencing buff animation.

Keeps the character, épée, and warm sword glow while removing the purple
background and floor illumination. Does not modify game resources.
"""

from __future__ import annotations

import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw

from matte_alt_idle_v1 import matte


ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_buff")
SOURCE = ROOT / "buff_v2_60f_60fps" / "frames_60fps"
OUT = ROOT / "buff_v2_alpha_60f_60fps"
FRAMES = OUT / "alpha_frames_60fps"
FPS = 60


def clean_frame(image: Image.Image) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.uint8).copy()
    height, width = rgba.shape[:2]
    alpha = rgba[:, :, 3]
    rgb = rgba[:, :, :3].astype(np.int16)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    xx = np.arange(width)[None, :]
    yy = np.arange(height)[:, None]

    # The generated floor glow/shadow is a wide band under the shoes. Keep
    # only the two narrow shoe/ankle corridors in the lower part of the frame.
    shoe_corridors = ((xx >= 105) & (xx <= 235)) | ((xx >= 430) & (xx <= 590))
    alpha[(yy >= 625) & ~shoe_corridors] = 0
    alpha[690:] = 0
    neutral_floor = ((yy >= 665) & (r >= 65) & (r <= 180)
                     & (np.abs(r - g) <= 4) & (np.abs(g - b) <= 4))
    alpha[neutral_floor] = 0
    # Sword flame is warm. Discard isolated purple H.264 flecks around it,
    # but do not apply this test to the figure or the steel blade.
    effect_region = (xx >= 550) & (yy < 480)
    purple = (r > g + 6) & (b > g + 6)
    alpha[effect_region & purple] = 0

    # Remove tiny unconnected codec specks, retaining thin épée and sparks.
    count, labels, stats, _ = cv2.connectedComponentsWithStats((alpha > 8).astype(np.uint8), 8)
    for label in range(1, count):
        x, y, w, h, area = stats[label]
        if area < 4:
            alpha[labels == label] = 0
        elif y > 645 and w > 5 * h and h < 14:
            alpha[labels == label] = 0
    rgba[:, :, :3][alpha == 0] = 0
    return Image.fromarray(rgba, "RGBA")


def make_contact(frames: list[Image.Image], background: tuple[int, int, int, int], path: Path) -> None:
    picks = np.linspace(0, len(frames) - 1, 20).round().astype(int)
    sheet = Image.new("RGB", (5 * 300, 4 * 245), (30, 30, 30))
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(picks):
        composited = Image.alpha_composite(Image.new("RGBA", frames[index].size, background), frames[index])
        thumb = composited.resize((300, 225), Image.Resampling.LANCZOS).convert("RGB")
        x, y = (slot % 5) * 300, (slot // 5) * 245
        sheet.paste(thumb, (x, y))
        draw.text((x + 4, y + 226), f"{index+1:02d}", fill=(255, 255, 255))
    sheet.save(path)


def main() -> None:
    paths = sorted(SOURCE.glob("buff_*.png"))
    if len(paths) != 60:
        raise RuntimeError(f"Expected 60 frames, found {len(paths)}")
    FRAMES.mkdir(parents=True, exist_ok=True)
    frames = []
    bboxes = []
    for index, path in enumerate(paths, 1):
        bgr = cv2.imread(str(path), cv2.IMREAD_COLOR)
        if bgr is None:
            raise RuntimeError(f"Could not read {path}")
        image = clean_frame(matte(bgr))
        image.save(FRAMES / f"buff_alpha_{index:03d}.png", optimize=True)
        bbox = image.getchannel("A").point(lambda a: 255 if a >= 8 else 0).getbbox()
        if bbox is None:
            raise RuntimeError(f"Empty alpha at frame {index}")
        frames.append(image)
        bboxes.append(bbox)
    for name, color in {
        "dark": (32, 32, 32, 255), "white": (255, 255, 255, 255),
        "green": (0, 170, 60, 255),
    }.items():
        make_contact(frames, color, OUT / f"contact_{name}.png")
    (OUT / "matte_report.json").write_text(json.dumps({
        "source": str(SOURCE), "frames": len(frames), "fps": FPS,
        "bbox_union": [min(b[0] for b in bboxes), min(b[1] for b in bboxes),
                       max(b[2] for b in bboxes), max(b[3] for b in bboxes)],
        "jar_exported": False,
    }, indent=2) + "\n", encoding="utf-8")
    print(f"Matted {len(frames)} frames in {FRAMES}")


if __name__ == "__main__":
    main()
