"""Make a 1 s / 60 fps buff preview with stable character scale.

This only creates review media. It does not touch the game runtime or JAR.
"""

from __future__ import annotations

import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image


VIDEO_DIR = Path(r"D:\StSmod\art_preview\video")
SOURCE = next(VIDEO_DIR.glob("jimeng-2026-09-27-3794-*.mp4"))
OUT = Path(r"D:\StSmod\art_preview\alternate_fencer_buff\buff_v1_60f_60fps")
FRAMES = OUT / "frames_60fps"
FPS = 60
NORMALIZE_SCALE = True
BASENAME = "buff_v1_60f_60fps"


def phase(start: int, end: int, count: int) -> list[int]:
    return np.linspace(start, end, count).round().astype(int).tolist()


# Preparation / tracing / restrained flash / recovery. Skip the giant star
# at source frames 63-68 while retaining its small lead-in and fade-out.
SELECTED = (
    phase(0, 30, 17)
    + phase(31, 59, 22)
    + [60, 61, 62, 69, 70]
    + phase(71, 96, 16)
)
assert len(SELECTED) == 60 and len(set(SELECTED)) == 60


def subject_bounds(rgb: np.ndarray) -> tuple[int, int]:
    height, width = rgb.shape[:2]
    x = np.arange(width)[None, :]
    y = np.arange(height)[:, None]
    figure = (rgb[:, :, 0] > 115) & (rgb[:, :, 1] > 75) & (x >= 100) & (x < 600)
    # The head and shoes are isolated vertically from the sword glow.
    head_rows = np.where(figure & (y < 260))[0]
    shoe_rows = np.where(figure & (y > 590))[0]
    if len(head_rows) == 0 or len(shoe_rows) == 0:
        raise RuntimeError("Could not locate head or shoes")
    return int(head_rows.min()), int(shoe_rows.max())


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    FRAMES.mkdir(parents=True, exist_ok=True)
    capture = cv2.VideoCapture(str(SOURCE))
    source_count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
    if source_count != 97:
        raise RuntimeError(f"Expected 97 source frames, found {source_count}")
    raw = []
    for index in SELECTED:
        capture.set(cv2.CAP_PROP_POS_FRAMES, index)
        ok, bgr = capture.read()
        if not ok:
            raise RuntimeError(f"Could not read source frame {index}")
        raw.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    capture.release()

    target_top, target_foot = subject_bounds(raw[0])
    target_height = target_foot - target_top
    corrected = []
    reports = []
    for index, rgb in zip(SELECTED, raw):
        top, foot = subject_bounds(rgb)
        if NORMALIZE_SCALE:
            scale = target_height / (foot - top)
            if not 0.94 <= scale <= 1.04:
                raise RuntimeError(f"Unexpected scale {scale:.4f} at source frame {index}")
            # Anchor the rear foot horizontally and both head and shoes vertically.
            x_offset = 142 * (1.0 - scale)
            y_offset = target_foot - scale * foot
            matrix = np.float32([[scale, 0, x_offset], [0, scale, y_offset]])
            fixed = cv2.warpAffine(rgb, matrix, (rgb.shape[1], rgb.shape[0]),
                                   flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT_101)
        else:
            scale = 1.0
            fixed = rgb
        corrected.append(fixed)
        fixed_top, fixed_foot = subject_bounds(fixed)
        reports.append({"source_frame": index, "source_head": top,
                        "source_foot": foot, "scale": round(scale, 5),
                        "output_head": fixed_top, "output_foot": fixed_foot})

    width, height = corrected[0].shape[1], corrected[0].shape[0]
    writer = cv2.VideoWriter(str(OUT / f"{BASENAME}.mp4"),
                             cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    if not writer.isOpened():
        raise RuntimeError("Could not open MP4 writer")
    gif_images = []
    for index, rgb in enumerate(corrected, 1):
        Image.fromarray(rgb, "RGB").save(FRAMES / f"buff_{index:03d}.png")
        writer.write(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR))
        gif_images.append(Image.fromarray(rgb, "RGB").resize((556, 417), Image.Resampling.LANCZOS))
    writer.release()

    # One shared palette avoids frame-to-frame colour jumps in the GIF.
    palette = gif_images[0].quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    quantized = [im.quantize(palette=palette, dither=Image.Dither.NONE) for im in gif_images]
    # GIF delays are centiseconds: 20+20+10 ms averages 60 fps exactly.
    gif_durations = [20, 20, 10] * 20
    quantized[0].save(OUT / f"{BASENAME}.gif", save_all=True,
                      append_images=quantized[1:], duration=gif_durations, loop=0,
                      disposal=2, optimize=False)
    (OUT / "frame_map.json").write_text(json.dumps({
        "source": str(SOURCE), "source_frames": source_count,
        "output_frames": len(SELECTED), "output_fps": FPS,
        "frame_map": reports,
    }, indent=2) + "\n", encoding="utf-8")
    print(f"Created {len(SELECTED)} frames @ {FPS} fps in {OUT}")
    print(f"Corrected head y: {min(x['output_head'] for x in reports)}..{max(x['output_head'] for x in reports)}")
    print(f"Corrected foot y: {min(x['output_foot'] for x in reports)}..{max(x['output_foot'] for x in reports)}")


if __name__ == "__main__":
    main()
