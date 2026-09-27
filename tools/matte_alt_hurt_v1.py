from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

import cv2
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
sys.path.insert(0, str(ROOT / "tools"))
from matte_alt_idle_v1 import matte, make_contact


SOURCE = Path(r"D:\StSmod\art_preview\hurt_v15_every_third_removed_again\hurt_v15_40f_frames_60fps")
OUT = ROOT / "art_candidates" / "alt_fencer_hurt_v1"
FRAMES = OUT / "alpha_frames"


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True, exist_ok=True)
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 40:
        raise RuntimeError(f"Expected 40 frames, found {len(paths)}")

    frames: list[Image.Image] = []
    bboxes = []
    for index, path in enumerate(paths, 1):
        bgr = cv2.imread(str(path), cv2.IMREAD_COLOR)
        if bgr is None:
            raise RuntimeError(f"Failed to read {path}")
        frame = matte(bgr)
        frame.save(FRAMES / f"alt_hurt_{index:03d}.png", optimize=True)
        bbox = frame.getchannel("A").point(lambda a: 255 if a >= 8 else 0).getbbox()
        if bbox is None:
            raise RuntimeError(f"Empty matte frame {index}")
        frames.append(frame)
        bboxes.append(bbox)

    for name, color in {
        "black": (0, 0, 0, 255),
        "white": (255, 255, 255, 255),
        "gray": (127, 127, 127, 255),
        "green": (0, 180, 60, 255),
    }.items():
        make_contact(frames, OUT / f"contact_{name}.jpg", color)

    union = (
        min(b[0] for b in bboxes), min(b[1] for b in bboxes),
        max(b[2] for b in bboxes), max(b[3] for b in bboxes),
    )
    report = {
        "source": str(SOURCE),
        "frames": len(frames),
        "fps": 60,
        "duration": len(frames) / 60,
        "canvas": list(frames[0].size),
        "union_alpha_bbox": list(union),
    }
    (OUT / "matte_report.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
