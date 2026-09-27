from pathlib import Path
import csv
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SOURCE = ROOT / "death_splice_frames_review_v24_true_head_transition"
OUTPUT = ROOT / "death_splice_v25_alpha_conservative"
FRAMES = OUTPUT / "alpha_frames"


def matte(path: Path) -> Image.Image:
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    red = rgb[:, :, 0].astype(np.float32)
    green = rgb[:, :, 1].astype(np.float32)
    blue = rgb[:, :, 2].astype(np.float32)
    purple_bias = np.minimum(red - green, blue - green)

    candidate = ((h >= 126) & (h <= 168) & (s >= 55) &
                 (v >= 45) & (purple_bias >= 28)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(candidate, 8)
    border_labels = set(np.unique(np.concatenate(
        (labels[0], labels[-1], labels[:, 0], labels[:, -1]))))
    background = np.zeros(candidate.shape, dtype=bool)
    for label in border_labels:
        if label:
            background |= labels == label

    # Only clear enclosed regions that are demonstrably the same flat purple.
    background_rgb = np.array([132, 0, 213], dtype=np.float32)
    distance = np.linalg.norm(rgb.astype(np.float32) - background_rgb, axis=2)
    for label in range(1, count):
        if label in border_labels:
            continue
        region = labels == label
        area = stats[label, cv2.CC_STAT_AREA]
        exact_fraction = float(np.mean(distance[region] <= 18.0))
        if area >= 500 or (area >= 6 and exact_fraction >= 0.22):
            background |= region

    foreground = ~background
    n, component_labels, component_stats, _ = cv2.connectedComponentsWithStats(
        foreground.astype(np.uint8), 8)
    keep = np.zeros_like(foreground)
    for label in range(1, n):
        if component_stats[label, cv2.CC_STAT_AREA] >= 20:
            keep |= component_labels == label
    foreground = keep

    inward = cv2.distanceTransform(foreground.astype(np.uint8), cv2.DIST_L2, 5)
    alpha = np.uint8(np.clip(inward / 1.05, 0, 1) * 255)
    alpha[~foreground] = 0
    alpha[alpha > 250] = 255

    core = (inward >= 2.2) & foreground
    _, nearest = distance_transform_edt(~core, return_indices=True)
    clean = rgb.copy()
    fringe = (alpha > 0) & (alpha < 255)
    clean[fringe] = rgb[nearest[0], nearest[1]][fringe]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def checker(size, cell=24):
    image = Image.new("RGBA", size, (225, 225, 225, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], cell):
        for x in range(0, size[0], cell):
            if (x // cell + y // cell) % 2:
                draw.rectangle((x, y, x + cell - 1, y + cell - 1),
                               fill=(165, 165, 165, 255))
    return image


def main():
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 132:
        raise RuntimeError(f"Expected 132 frames, found {len(paths)}")

    frames = []
    stats = []
    for index, path in enumerate(paths):
        image = matte(path)
        image.save(FRAMES / f"death_{index + 1:03d}.png", optimize=True)
        alpha = np.asarray(image.getchannel("A"))
        frames.append(image)
        stats.append((index + 1, int((alpha > 0).sum()),
                      int(((alpha > 0) & (alpha < 255)).sum())))

    with (OUTPUT / "matte_stats.csv").open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.writer(stream)
        writer.writerow(("frame", "visible_pixels", "antialias_pixels"))
        writer.writerows(stats)

    black = Image.new("RGBA", frames[0].size, (0, 0, 0, 255))
    preview = [Image.alpha_composite(black.copy(), frame).resize(
        (560, 416), Image.Resampling.LANCZOS).convert("RGB") for frame in frames]
    preview[0].save(OUTPUT / "death_alpha_black_24fps.gif", save_all=True,
                    append_images=preview[1:], duration=42, loop=0,
                    disposal=2, optimize=False)

    picks = np.linspace(0, len(frames) - 1, 30).round().astype(int)
    sheet = Image.new("RGB", (6 * 320, 5 * 508), (28, 28, 28))
    draw = ImageDraw.Draw(sheet)
    board = checker(frames[0].size)
    for slot, frame_index in enumerate(picks):
        x = (slot % 6) * 320
        y = (slot // 6) * 508
        for row, background in enumerate((board, black)):
            view = Image.alpha_composite(background.copy(), frames[frame_index]).resize(
                (320, 240), Image.Resampling.LANCZOS).convert("RGB")
            sheet.paste(view, (x, y + row * 240))
        draw.text((x + 5, y + 484), f"frame {frame_index + 1}", fill="white")
    sheet.save(OUTPUT / "death_alpha_checker_black.jpg", quality=96)
    report = {"frames": len(frames), "fps": 24,
              "duration": len(frames) / 24,
              "method": "border-connected conservative purple chroma matte"}
    (OUTPUT / "report.json").write_text(json.dumps(report, indent=2) + "\n",
                                        encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
