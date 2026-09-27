from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUT = VIDEO_DIR / "idle_v2_matte_60fps"
SOURCE_FRAMES = OUT / "source_24fps"
FRAMES_60 = OUT / "frames_60fps"
GIF = OUT / "idle_v2_matte_60fps.gif"
TARGET_FPS = 60.0

sys.path.insert(0, str(ROOT / "DoubleSS" / "tools"))
from matte_alt_idle_v1 import matte


def matte_with_mask_protection(frame_bgr: np.ndarray) -> Image.Image:
    """Purple key plus a topology guard for the half fencing mask."""
    result = np.asarray(matte(frame_bgr)).copy()
    rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    # The camera is locked. Locate the dark mask/face cluster, then protect
    # only a narrow ellipse well inside the black half-mask. This repairs keyed
    # purple reflections without ever expanding its exterior silhouette.
    x0, y0, x1, y1 = 330, 145, 390, 275
    alpha_roi = result[y0:y1, x0:x1, 3]
    roi = hsv[y0:y1, x0:x1]
    dark = ((roi[:, :, 2] < 115) & (roi[:, :, 1] < 185)).astype(np.uint8)
    count, labels, stats, centers = cv2.connectedComponentsWithStats(dark, 8)
    if count <= 1:
        return Image.fromarray(result, "RGBA")
    label = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    center_y = float(centers[label, 1])
    yy, xx = np.ogrid[: y1 - y0, : x1 - x0]
    inner_mask = ((xx - 29.0) / 11.0) ** 2 + ((yy - center_y) / 36.0) ** 2 <= 1.0
    holes = inner_mask & (alpha_roi < 248)
    if np.any(holes):
        donor_mask = (alpha_roi >= 248) & (roi[:, :, 2] < 125)
        if np.any(donor_mask):
            _, nearest = distance_transform_edt(~donor_mask, return_indices=True)
            clean_roi = result[y0:y1, x0:x1, :3]
            donors = clean_roi[nearest[0], nearest[1]]
            clean_roi[holes] = donors[holes]
            alpha_roi[holes] = 255
    return Image.fromarray(result, "RGBA")


def latest_source() -> Path:
    candidates = [p for p in VIDEO_DIR.glob("*.mp4") if "idle_exposure_matched" not in p.name]
    return max(candidates, key=lambda p: p.stat().st_mtime)


def save_transparent_gif(frames: list[Image.Image], output: Path) -> None:
    # Build one shared 255-color foreground palette; reserve index 255 for
    # transparency so palette changes cannot shimmer between frames.
    samples = [f.convert("RGB").resize((128, 128), Image.Resampling.LANCZOS)
               for f in frames[::max(1, len(frames) // 20)]]
    sheet = Image.new("RGB", (128 * len(samples), 128), (0, 0, 0))
    for index, sample in enumerate(samples):
        sheet.paste(sample, (index * 128, 0))
    palette = sheet.quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    palette_data = palette.getpalette()[: 255 * 3] + [0, 0, 0]

    indexed = []
    for frame in frames:
        alpha = np.asarray(frame.getchannel("A"))
        item = frame.convert("RGB").quantize(palette=palette, dither=Image.Dither.FLOYDSTEINBERG)
        item.putpalette(palette_data)
        pixels = np.asarray(item).copy()
        pixels[alpha < 128] = 255
        item = Image.fromarray(pixels.astype(np.uint8), "P")
        item.putpalette(palette_data)
        item.info["transparency"] = 255
        indexed.append(item)
    indexed[0].save(
        output,
        save_all=True,
        append_images=indexed[1:],
        duration=round(1000 / TARGET_FPS),
        loop=0,
        transparency=255,
        disposal=2,
        optimize=True,
    )


def make_qa(frames: list[Image.Image], path: Path) -> None:
    picks = np.linspace(0, len(frames) - 1, 12).round().astype(int)
    size = 320
    sheet = Image.new("RGB", (size * 4, (size + 24) * 3), (45, 45, 45))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        bg = Image.new("RGBA", frames[index].size, (72, 72, 72, 255))
        bg.alpha_composite(frames[index])
        thumb = bg.resize((size, size), Image.Resampling.LANCZOS).convert("RGB")
        x = n % 4 * size
        y = n // 4 * (size + 24)
        sheet.paste(thumb, (x, y + 24))
        draw.text((x + 6, y + 5), f"60fps frame {index + 1}", fill="white")
    sheet.save(path, quality=95)


def main() -> None:
    source = latest_source()
    if OUT.exists():
        shutil.rmtree(OUT)
    SOURCE_FRAMES.mkdir(parents=True)
    FRAMES_60.mkdir(parents=True)

    capture = cv2.VideoCapture(str(source))
    source_fps = float(capture.get(cv2.CAP_PROP_FPS))
    originals: list[Image.Image] = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        originals.append(matte_with_mask_protection(bgr))
    capture.release()
    if not originals:
        raise RuntimeError(f"No frames decoded from {source}")

    for index, frame in enumerate(originals, 1):
        frame.save(SOURCE_FRAMES / f"frame_{index:04d}.png", optimize=True)

    duration = len(originals) / source_fps
    output_count = round(duration * TARGET_FPS)
    positions = np.arange(output_count, dtype=np.float64) / TARGET_FPS * source_fps
    source_indices = np.minimum(np.rint(positions).astype(int), len(originals) - 1)
    frames_60 = [originals[index].copy() for index in source_indices]
    for index, frame in enumerate(frames_60, 1):
        frame.save(FRAMES_60 / f"frame_{index:04d}.png", optimize=True)

    save_transparent_gif(frames_60, GIF)
    make_qa(frames_60, OUT / "qa_contact_gray.jpg")
    report = {
        "source": str(source),
        "source_frames": len(originals),
        "source_fps": source_fps,
        "duration": duration,
        "output_frames": len(frames_60),
        "output_fps": TARGET_FPS,
        "resampling": "nearest-time; no synthetic optical-flow pixels",
        "gif": str(GIF),
    }
    (OUT / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
