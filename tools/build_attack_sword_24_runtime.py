#!/usr/bin/env python3
"""Build a 350x350, 24-frame, 30 FPS runtime atlas and Spine 3.4 JSON."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageFilter


SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR / "spine-compat-probe"))
from convert_spine_38_to_34 import convert  # noqa: E402


# Original clean frames map to these decoded frames in the approved trimmed video.
VIDEO_FRAME_MAP = [0, 3, 6, 9, 12, 15, 45, 48, 51, 54, 57, 60, 63, 66, 69, 72]
# Add genuine video frames in the motion-heavy gaps (no duplicated or ghosted frames).
EXTRA_GAPS = {3, 4, 5, 8, 9, 10, 11, 14}
FPS = 30.0
CANVAS = 350
ATLAS_COLUMNS = 5


def resize_frame(image: Image.Image) -> np.ndarray:
    image = image.convert("RGBA")
    image.thumbnail((CANVAS, CANVAS), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    # Keep the ground line stable; the original 700x660 canvas becomes 350x330.
    canvas.alpha_composite(image, ((CANVAS - image.width) // 2, CANVAS - image.height - 10))
    return np.asarray(canvas, dtype=np.uint8)


def remove_light_background(rgb: Image.Image) -> Image.Image:
    flood = rgb.convert("RGB")
    marker = (255, 0, 255)
    w, h = flood.size
    for seed in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1),
                 (w // 2, 0), (w // 2, h - 1), (0, h // 2), (w - 1, h // 2)):
        ImageDraw.floodfill(flood, seed, marker, thresh=42)
    flooded = np.asarray(flood)
    background = np.all(flooded == marker, axis=2)
    alpha = Image.fromarray(np.where(background, 0, 255).astype(np.uint8), "L")
    alpha = alpha.filter(ImageFilter.GaussianBlur(0.45))
    result = rgb.convert("RGBA")
    result.putalpha(alpha)
    return result


def alpha_bbox(frame: np.ndarray) -> tuple[int, int, int, int]:
    ys, xs = np.where(frame[..., 3] > 20)
    return int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)


def body_bbox(frame: np.ndarray) -> tuple[int, int, int, int]:
    rgb = frame[..., :3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    # Hair, skin and clothes are saturated; the long silver blade is not. This
    # makes scale/alignment follow the body instead of the changing sword angle.
    mask = ((frame[..., 3] > 20) & (hsv[..., 1] > 35) & (hsv[..., 2] > 35)).astype(np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((7, 7), np.uint8))
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    if count <= 1:
        return alpha_bbox(frame)
    component = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    x, y, w, h, area = stats[component]
    if area < 100:
        return alpha_bbox(frame)
    return int(x), int(y), int(x + w), int(y + h)


def fit_between(video_frame: Image.Image, left: np.ndarray, right: np.ndarray) -> np.ndarray:
    rgba = remove_light_background(video_frame)
    source_array = np.asarray(rgba, dtype=np.uint8)
    if rgba.getbbox() is None:
        raise RuntimeError("Video in-between frame has no foreground")
    sb = body_bbox(source_array)
    lb, rb = body_bbox(left), body_bbox(right)
    target_w = ((lb[2] - lb[0]) + (rb[2] - rb[0])) / 2
    target_h = ((lb[3] - lb[1]) + (rb[3] - rb[1])) / 2
    # The video model's soft antialias halo makes its body component slightly
    # smaller than the web-clean master mask; compensate so inserted frames do
    # not visually pulse larger than their neighbours.
    scale = 0.82 * min(target_w / (sb[2] - sb[0]), target_h / (sb[3] - sb[1]))
    size = (max(1, round(rgba.width * scale)), max(1, round(rgba.height * scale)))
    resized = rgba.resize(size, Image.Resampling.LANCZOS)
    center_x = ((lb[0] + lb[2]) + (rb[0] + rb[2])) / 4
    bottom = (lb[3] + rb[3]) / 2
    source_center_x = (sb[0] + sb[2]) * 0.5 * scale
    source_bottom = sb[3] * scale
    x = round(center_x - source_center_x)
    y = round(bottom - source_bottom)
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.alpha_composite(resized, (x, y))
    return np.asarray(canvas, dtype=np.uint8)


def build_frames(source_dir: Path, video_path: Path) -> list[np.ndarray]:
    originals = [resize_frame(Image.open(source_dir / f"attack_sword_{i:02}.png")) for i in range(1, 17)]
    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise RuntimeError(f"Unable to open {video_path}")
    result: list[np.ndarray] = []
    for number, frame in enumerate(originals, start=1):
        result.append(frame)
        if number in EXTRA_GAPS:
            decoded_index = (VIDEO_FRAME_MAP[number - 1] + VIDEO_FRAME_MAP[number]) // 2
            capture.set(cv2.CAP_PROP_POS_FRAMES, decoded_index)
            ok, bgr = capture.read()
            if not ok:
                raise RuntimeError(f"Unable to decode video frame {decoded_index}")
            rgb = Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB), "RGB")
            result.append(fit_between(rgb, frame, originals[number]))
    capture.release()
    if len(result) != 24:
        raise AssertionError(f"Expected 24 frames, got {len(result)}")
    return result


def write_atlas(frames: list[np.ndarray], destination: Path, page_name: str) -> None:
    rows = (len(frames) + ATLAS_COLUMNS - 1) // ATLAS_COLUMNS
    page = Image.new("RGBA", (ATLAS_COLUMNS * CANVAS, rows * CANVAS), (0, 0, 0, 0))
    regions = []
    for index, frame in enumerate(frames, start=1):
        col = (index - 1) % ATLAS_COLUMNS
        row = (index - 1) // ATLAS_COLUMNS
        x, y = col * CANVAS, row * CANVAS
        page.alpha_composite(Image.fromarray(frame, "RGBA"), (x, y))
        regions.append((f"attack_sword_{index:02}", x, y))
    page.save(destination / page_name, optimize=True)

    lines = [page_name, f"size: {page.width},{page.height}", "format: RGBA8888",
             "filter: Linear,Linear", "repeat: none"]
    for name, x, y in regions:
        lines.extend([name, "  rotate: false", f"  xy: {x}, {y}",
                      f"  size: {CANVAS}, {CANVAS}", f"  orig: {CANVAS}, {CANVAS}",
                      "  offset: 0, 0", "  index: -1"])
    (destination / "character.atlas").write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_json(source_json: Path, destination: Path, frame_count: int, fps: float,
               hit_frame: int) -> None:
    temp = destination.with_suffix(".converted.json")
    convert(source_json, temp)
    data = json.loads(temp.read_text(encoding="utf-8"))
    temp.unlink()

    data["skeleton"].update({"x": -175, "y": -60.25, "width": 350, "height": 350})
    data["skeleton"]["images"] = "./"
    slot_name = "character_frame"
    attachments = {}
    for i in range(1, frame_count + 1):
        attachments[f"attack_sword_{i:02}"] = {"y": 104.75, "width": 350, "height": 350}
    data["skins"]["default"][slot_name] = attachments
    data["slots"][0]["attachment"] = "attack_sword_01"

    timeline = []
    for i in range(2, frame_count + 1):
        timeline.append({"time": round((i - 1) / fps, 6), "name": f"attack_sword_{i:02}"})
    attack = data["animations"]["attack_sword"]
    attack["slots"][slot_name]["attachment"] = timeline
    attack["events"] = [
        {"time": round((hit_frame - 1) / fps, 6), "name": "attack_hit"},
        {"time": round(frame_count / fps, 6), "name": "attack_end"},
    ]
    destination.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n",
                           encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("frames", type=Path)
    parser.add_argument("video", type=Path)
    parser.add_argument("source_json", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--clean-only", action="store_true",
                        help="Use only the 16 approved web-clean frames")
    args = parser.parse_args()
    args.destination.mkdir(parents=True, exist_ok=True)
    if args.clean_only:
        frames = [resize_frame(Image.open(args.frames / f"attack_sword_{i:02}.png"))
                  for i in range(1, 17)]
        fps = 20.0
        hit_frame = 11
    else:
        frames = build_frames(args.frames, args.video)
        fps = FPS
        hit_frame = 17
    write_atlas(frames, args.destination, "character.png")
    write_json(args.source_json, args.destination / "character.json", len(frames), fps, hit_frame)
    preview = [Image.fromarray(frame, "RGBA") for frame in frames]
    preview[0].save(args.destination / f"attack_sword_{len(frames)}_preview.gif", save_all=True,
                    append_images=preview[1:], duration=33, loop=0, disposal=2)
    print(f"Built {len(frames)} frames at {fps:g} FPS on a 350x350 canvas")


if __name__ == "__main__":
    main()
