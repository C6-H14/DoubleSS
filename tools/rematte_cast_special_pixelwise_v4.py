from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_special" / "combined_preview" / "trimmed_v2_75f_50fps" / "frames"
OUTPUT = ROOT / "art_candidates" / "video_source" / "cast_special" / "final_v4_pixelwise"
FRAMES = OUTPUT / "alpha_frames"


def matte(path: Path) -> Image.Image:
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)

    # The two generated source videos have tightly clustered purple backgrounds:
    # H=134..140, S>227 and V>164 on their borders. Keep generous margins for
    # compression, but require brightness so purple-tinted dark shoes/clothing
    # can never be classified as background.
    candidate = ((h >= 125) & (h <= 150) & (s >= 80) & (v >= 130)).astype(np.uint8)
    candidate = cv2.morphologyEx(candidate, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))

    count, labels, stats, _ = cv2.connectedComponentsWithStats(candidate, 8)
    border_labels = set(np.unique(np.concatenate((labels[0], labels[-1], labels[:, 0], labels[:, -1]))))
    background = np.zeros(candidate.shape, dtype=bool)
    for label in border_labels:
        if label:
            background |= labels == label

    border_rgb = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    background_rgb = np.median(border_rgb, axis=0)

    # Remove enclosed purple pockets only when their original RGB is close to
    # the actual border background. Brighter/different purple enchantment and
    # rune pixels are retained even though their hue is also purple.
    for label in range(1, count):
        if label not in border_labels:
            component = labels == label
            median_rgb = np.median(rgb[component], axis=0)
            if np.linalg.norm(median_rgb - background_rgb) <= 50.0:
                background |= component

    foreground = ~background
    distance = cv2.distanceTransform(foreground.astype(np.uint8), cv2.DIST_L2, 5)
    alpha = np.uint8(np.clip(distance / 1.25, 0.0, 1.0) * 255.0)
    alpha[background] = 0
    alpha[alpha > 250] = 255

    # Decontaminate translucent RGB using the nearest fully opaque foreground.
    opaque = alpha >= 250
    _, nearest = distance_transform_edt(~opaque, return_indices=True)
    clean = rgb.copy()
    edge = (alpha > 0) & (alpha < 250)
    clean[edge] = rgb[nearest[0], nearest[1]][edge]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 75:
        raise RuntimeError(f"Expected 75 frames, found {len(paths)}")
    frames = []
    for index, path in enumerate(paths, 1):
        frame = matte(path)
        frame.save(FRAMES / f"cast_special_{index:03d}.png", optimize=True)
        frames.append(frame)

    for name, color in (("black", (0, 0, 0, 255)), ("white", (255, 255, 255, 255))):
        views = [Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)
                 .resize((480, 480), Image.Resampling.LANCZOS).convert("RGB") for frame in frames]
        views[0].save(OUTPUT / f"cast_special_{name}_pixelwise_75f_50fps.gif",
                      save_all=True, append_images=views[1:], duration=20, loop=0,
                      disposal=2, optimize=False)
        columns, thumb = 10, 160
        rows = (len(frames) + columns - 1) // columns
        sheet = Image.new("RGB", (columns * thumb, rows * (thumb + 20)), (24, 24, 24))
        draw = ImageDraw.Draw(sheet)
        for i, view in enumerate(views):
            small = view.resize((thumb, thumb), Image.Resampling.LANCZOS)
            x, y = (i % columns) * thumb, (i // columns) * (thumb + 20)
            sheet.paste(small, (x, y))
            draw.text((x + 4, y + thumb + 2), f"{i + 1:03d}", fill="white")
        sheet.save(OUTPUT / f"cast_special_{name}_pixelwise_contact.jpg", quality=96)

    # Explicit regression measurement for the first-frame shoe pixels.
    src = np.asarray(Image.open(paths[0]).convert("RGB"))
    alpha = np.asarray(frames[0].getchannel("A"))
    r, g, b = (src[..., i] for i in range(3))
    yy = np.indices(r.shape)[0]
    shoe = ((yy > 820) & (r > 35) & (r < 160) & (g > 15) & (g < 110)
            & (b < 120) & (r > g * 1.12))
    report = {
        "frames": len(frames),
        "method": "source RGB/HSV pixelwise purple classification",
        "frame1_shoe_pixels": int(shoe.sum()),
        "frame1_shoe_pixels_visible": int(((alpha > 8) & shoe).sum()),
    }
    (OUTPUT / "matte_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
