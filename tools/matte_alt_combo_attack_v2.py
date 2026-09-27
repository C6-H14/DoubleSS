from __future__ import annotations

import json
import sys
from pathlib import Path

import cv2
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
sys.path.insert(0, str(ROOT / "tools"))
from matte_alt_idle_v1 import make_contact, matte


SOURCE = Path(
    r"D:\StSmod\art_preview\alternate_fencer_combo_attack\combo_attack_v4_first_thrust_repaired\frames_120f_60fps"
)
OUT = ROOT / "art_candidates" / "alt_fencer_combo_attack_v2"
FRAMES = OUT / "alpha_frames_120f_60fps"
GIF = OUT / "combo_attack_alpha_120f_60fps.gif"
EXPECTED = 120
FPS = 60


def main() -> None:
    FRAMES.mkdir(parents=True, exist_ok=True)
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} frames, found {len(paths)}")

    frames: list[Image.Image] = []
    bboxes = []
    for index, path in enumerate(paths, 1):
        bgr = cv2.imread(str(path), cv2.IMREAD_COLOR)
        if bgr is None:
            raise RuntimeError(f"Failed to read {path}")
        frame = matte(bgr)
        frame.save(FRAMES / f"alt_combo_attack_{index:03d}.png", optimize=True)
        bbox = frame.getchannel("A").point(lambda a: 255 if a >= 8 else 0).getbbox()
        if bbox is None:
            raise RuntimeError(f"Empty matte frame {index}")
        frames.append(frame)
        bboxes.append(bbox)

    # GIF stores delays in centiseconds; this sequence averages to exact 60 FPS.
    durations = ([20, 20, 10] * ((len(frames) + 2) // 3))[: len(frames)]
    target_ms = round(len(frames) * 1000 / FPS / 10) * 10
    durations[-1] += target_ms - sum(durations)
    frames[0].save(
        GIF,
        save_all=True,
        append_images=frames[1:],
        duration=durations,
        loop=0,
        optimize=False,
        disposal=2,
    )
    for name, color in {
        "black": (0, 0, 0, 255),
        "white": (255, 255, 255, 255),
        "gray": (127, 127, 127, 255),
        "green": (0, 180, 60, 255),
    }.items():
        make_contact(frames, OUT / f"contact_{name}.jpg", color)

    union = (
        min(b[0] for b in bboxes),
        min(b[1] for b in bboxes),
        max(b[2] for b in bboxes),
        max(b[3] for b in bboxes),
    )
    report = {
        "source": str(SOURCE),
        "frames": len(frames),
        "fps": FPS,
        "duration": len(frames) / FPS,
        "canvas": list(frames[0].size),
        "union_alpha_bbox": list(union),
        "gif": str(GIF),
    }
    (OUT / "matte_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
