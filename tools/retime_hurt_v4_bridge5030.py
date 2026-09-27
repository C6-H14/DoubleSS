from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw

from matte_alt_idle_v1 import matte


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
BRIDGE = next(VIDEO_DIR.glob("jimeng-2026-09-26-5030-*.mp4"))
HURT = next(VIDEO_DIR.glob("jimeng-2026-09-26-3578-*.mp4"))
OUT_ROOT = ROOT / "art_preview" / "hurt_v4_bridge5030"
FRAMES = OUT_ROOT / "hurt_v4_51f_frames_60fps"
GIF = OUT_ROOT / "hurt_v4_51f_60fps.gif"
MP4 = OUT_ROOT / "hurt_v4_51f_60fps.mp4"
FPS = 60
BRIDGE_SOURCE_RANGE = (1, 62)
BRIDGE_OUTPUT_FRAMES = 15
HURT_SOURCE_RANGE = (21, 37)
HURT_OUTPUT_FRAMES = 9
HOLD_FRAMES = 4


def decode(path: Path) -> tuple[list[np.ndarray], float]:
    cap = cv2.VideoCapture(str(path))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    cap.release()
    if not frames:
        raise RuntimeError(f"No frames decoded from {path}")
    return frames, fps


def sampled(first: int, last: int, count: int) -> list[int]:
    return [round(x) for x in np.linspace(first - 1, last - 1, count)]


def keyed_on_common_background(rgb: np.ndarray, scale: float = 1.0) -> np.ndarray:
    rgba = matte(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR))
    if scale != 1.0:
        w, h = rgba.size
        enlarged = rgba.resize(
            (round(w * scale), round(h * scale)), Image.Resampling.LANCZOS
        )
        left = (enlarged.width - w) // 2
        top = (enlarged.height - h) // 2
        rgba = enlarged.crop((left, top, left + w, top + h))
    background = Image.new("RGBA", rgba.size, (56, 0, 111, 255))
    return np.asarray(Image.alpha_composite(background, rgba).convert("RGB"))


def make_contact(images: list[Image.Image], path: Path) -> None:
    picks = list(range(10, 18)) + list(range(22, 29))
    tw, th, cols = 278, 209, 5
    sheet = Image.new("RGB", (cols * tw, 3 * (th + 25)), (24, 24, 24))
    draw = ImageDraw.Draw(sheet)
    for n, idx in enumerate(picks):
        thumb = images[idx].resize((tw, th), Image.Resampling.LANCZOS)
        x = (n % cols) * tw
        y = (n // cols) * (th + 25)
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 6, y + 5), f"output {idx + 1}", fill="white")
    sheet.save(path, quality=96)


def main() -> None:
    bridge_frames, bridge_fps = decode(BRIDGE)
    hurt_frames, hurt_fps = decode(HURT)
    bridge_indices = sampled(*BRIDGE_SOURCE_RANGE, BRIDGE_OUTPUT_FRAMES)
    hurt_indices = sampled(*HURT_SOURCE_RANGE, HURT_OUTPUT_FRAMES)

    enter_arrays = [keyed_on_common_background(bridge_frames[i]) for i in bridge_indices]
    # The original hurt segment is about three percent smaller than the new
    # bridge at their shared pose. Scale it around canvas center so head and
    # foot baselines remain continuous at the splice.
    enter_arrays.extend(keyed_on_common_background(hurt_frames[i], 1.03)
                        for i in hurt_indices)
    if len(enter_arrays) != 24:
        raise RuntimeError(f"Expected 24 enter frames, got {len(enter_arrays)}")
    sequence = enter_arrays + [enter_arrays[-1]] * HOLD_FRAMES + enter_arrays[-2::-1]
    if len(sequence) != 51:
        raise RuntimeError(f"Expected 51 total frames, got {len(sequence)}")

    if OUT_ROOT.exists():
        shutil.rmtree(OUT_ROOT)
    FRAMES.mkdir(parents=True)
    images = []
    for i, rgb in enumerate(sequence, 1):
        image = Image.fromarray(rgb, "RGB")
        image.save(FRAMES / f"frame_{i:03d}.png", optimize=True)
        images.append(image)

    images[0].save(
        GIF, save_all=True, append_images=images[1:],
        duration=round(1000 / FPS), loop=0, disposal=2, optimize=False,
    )
    width, height = images[0].size
    writer = cv2.VideoWriter(
        str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height)
    )
    if not writer.isOpened():
        raise RuntimeError("Failed to open MP4 writer")
    for image in images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()
    make_contact(images, OUT_ROOT / "junction_contact.jpg")

    metadata = {
        "fps": FPS,
        "frames": len(sequence),
        "duration": len(sequence) / FPS,
        "enter": {
            "frames": len(enter_arrays), "duration": len(enter_arrays) / FPS,
            "bridge_source": str(BRIDGE), "bridge_source_fps": bridge_fps,
            "bridge_source_frames_1based": [i + 1 for i in bridge_indices],
            "hurt_source": str(HURT), "hurt_source_fps": hurt_fps,
            "hurt_source_frames_1based": [i + 1 for i in hurt_indices],
        },
        "hold": {"frames": HOLD_FRAMES, "duration": HOLD_FRAMES / FPS},
        "recover": {
            "frames": len(enter_arrays) - 1,
            "duration": (len(enter_arrays) - 1) / FPS,
            "method": "reverse exact composite enter path without duplicate endpoint",
        },
    }
    (OUT_ROOT / "hurt_v4_metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
