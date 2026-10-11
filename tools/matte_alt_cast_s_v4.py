from pathlib import Path
import json
import sys

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
PROJECT = ROOT / "DoubleSS"
sys.path.insert(0, str(PROJECT / "tools"))
from matte_alt_idle_v1 import matte


SOURCE = ROOT / "art_preview" / "alternate_fencer_cast_s" / "cast_s_v3_45f_750ms" / "frames_45f_60fps"
OUT = ROOT / "art_preview" / "alternate_fencer_cast_s" / "cast_s_v4_alpha_45f_60fps"
FRAMES_OUT = OUT / "alpha_frames_45f_60fps"
FPS = 60
EXPECTED = 45


def remove_ground_artifacts(frame: Image.Image) -> Image.Image:
    rgba = np.array(frame)
    alpha = rgba[:, :, 3]
    count, labels, stats, _ = cv2.connectedComponentsWithStats((alpha > 8).astype(np.uint8), 8)
    h, _ = alpha.shape
    for label in range(1, count):
        x, y, w, ch, area = stats[label]
        if area < 20:
            continue
        colors = rgba[:, :, :3][labels == label]
        median = np.median(colors, axis=0)
        # Generated purple/pink floor glows are low, wide, dim components.
        # Keep bright orange fire rays and every component connected to the character.
        if y > h * 0.72 and w > ch * 3.0 and ch < 42 and float(median.max()) < 220:
            rgba[labels == label] = 0
    return Image.fromarray(rgba, "RGBA")


def contact(frames, path, color):
    picks = np.linspace(0, len(frames) - 1, 20).round().astype(int)
    tw, th, cols = 256, 192, 5
    sheet = Image.new("RGB", (tw * cols, (th + 23) * 4), (24, 24, 24))
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(picks):
        view = Image.alpha_composite(Image.new("RGBA", frames[index].size, color), frames[index])
        thumb = view.resize((tw, th), Image.Resampling.LANCZOS).convert("RGB")
        x, y = slot % cols * tw, slot // cols * (th + 23)
        sheet.paste(thumb, (x, y + 23))
        draw.text((x + 5, y + 4), f"frame {index + 1:02d}", fill="white")
    sheet.save(path, quality=95)


def main():
    paths = sorted(SOURCE.glob("*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} frames, found {len(paths)}")
    FRAMES_OUT.mkdir(parents=True, exist_ok=True)
    frames = []
    bboxes = []
    for index, path in enumerate(paths, 1):
        bgr = cv2.imread(str(path), cv2.IMREAD_COLOR)
        if bgr is None:
            raise RuntimeError(f"Cannot read {path}")
        rgba = remove_ground_artifacts(matte(bgr))
        rgba.save(FRAMES_OUT / f"cast_s_alpha_{index:03d}.png", optimize=True)
        bbox = rgba.getchannel("A").point(lambda a: 255 if a >= 8 else 0).getbbox()
        if bbox is None:
            raise RuntimeError(f"Empty alpha frame {index}")
        frames.append(rgba)
        bboxes.append(bbox)

    for name, color in {
        "black": (0, 0, 0, 255), "white": (255, 255, 255, 255),
        "gray": (127, 127, 127, 255), "green": (0, 180, 60, 255),
    }.items():
        contact(frames, OUT / f"contact_{name}.jpg", color)

    preview = [f.resize((640, 480), Image.Resampling.LANCZOS) for f in frames]
    preview[0].save(OUT / "cast_s_alpha_45f_60fps.gif", save_all=True,
                    append_images=preview[1:], duration=round(1000 / FPS),
                    loop=0, disposal=2, optimize=False)

    report = {
        "source": str(SOURCE), "frames": len(frames), "fps": FPS,
        "duration_seconds": len(frames) / FPS, "canvas": list(frames[0].size),
        "union_alpha_bbox": [min(b[0] for b in bboxes), min(b[1] for b in bboxes),
                             max(b[2] for b in bboxes), max(b[3] for b in bboxes)],
        "jar_exported": False,
    }
    (OUT / "matte_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
