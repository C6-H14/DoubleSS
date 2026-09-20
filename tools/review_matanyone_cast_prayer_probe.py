from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "cast_prayer"
SOURCE = BASE / "preview_v6" / "frames"
CHROMA_ALPHA = BASE / "preview_v8_border_connected" / "alpha_frames"
WORK = ROOT / "art_candidates" / "matting_tests" / "matanyone_cast_prayer_probe"
PHA = WORK / "output" / "cast_prayer_41_prayer1024" / "pha"
REVIEW = WORK / "review_7_frames"
PICKS = [1, 15, 22, 23, 30, 38, 41]


def semantic_rgba(frame_no: int) -> Image.Image:
    rgb = np.asarray(Image.open(SOURCE / f"cast_prayer_{frame_no:03d}.png").convert("RGB"))
    pha_paths = sorted(PHA.glob("*.png"))
    alpha = cv2.imread(str(pha_paths[frame_no - 1]), cv2.IMREAD_GRAYSCALE)
    alpha = cv2.resize(alpha, (rgb.shape[1], rgb.shape[0]), interpolation=cv2.INTER_LINEAR)
    alpha[alpha < 3] = 0
    solid = alpha >= 235
    _, nearest = distance_transform_edt(~solid, return_indices=True)
    clean = rgb.copy()
    fringe = (alpha > 0) & (alpha < 235)
    clean[fringe] = rgb[nearest[0], nearest[1]][fringe]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def semantic_with_gold_effect(frame_no: int) -> Image.Image:
    source = np.asarray(Image.open(SOURCE / f"cast_prayer_{frame_no:03d}.png").convert("RGB"))
    person = np.asarray(semantic_rgba(frame_no))
    person_alpha = person[..., 3]
    keyed_alpha = np.asarray(Image.open(CHROMA_ALPHA / f"cast_prayer_{frame_no:03d}.png").convert("RGBA").getchannel("A"))
    hsv = cv2.cvtColor(source, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)

    outside = person_alpha < 160
    # Unambiguous yellow/gold pixels outside the semantic person seed the spell.
    gold_seed = outside & (h >= 7) & (h <= 42) & (s >= 45) & (v >= 105) & (keyed_alpha >= 12)
    effect_region = cv2.dilate(gold_seed.astype(np.uint8), np.ones((19, 19), np.uint8), iterations=1).astype(bool)
    # Recover the pale/white fringe only locally around a gold seed, using the
    # border-connected key as a conservative upper bound.
    effect_alpha = keyed_alpha.copy()
    effect_alpha[~effect_region] = 0
    effect_alpha[person_alpha >= 210] = 0
    combined_alpha = np.maximum(person_alpha, effect_alpha)

    combined_rgb = source.copy()
    # Semantic RGB is cleaner on the character boundary; source RGB is needed
    # for the independent gold spell pixels.
    semantic_mask = person_alpha > 0
    combined_rgb[semantic_mask] = person[..., :3][semantic_mask]
    combined_rgb[combined_alpha == 0] = 0
    return Image.fromarray(np.dstack((combined_rgb, combined_alpha)), "RGBA")


def composite(image: Image.Image, color):
    return Image.alpha_composite(Image.new("RGBA", image.size, color), image).convert("RGB")


def main() -> None:
    REVIEW.mkdir(parents=True, exist_ok=True)
    frames = []
    combined_frames = []
    for frame_no in PICKS:
        image = semantic_rgba(frame_no)
        image.save(REVIEW / f"cast_prayer_{frame_no:03d}_matanyone.png", optimize=True)
        frames.append((frame_no, image))
        combined = semantic_with_gold_effect(frame_no)
        combined.save(REVIEW / f"cast_prayer_{frame_no:03d}_combined.png", optimize=True)
        combined_frames.append((frame_no, combined))

    tile = 400
    sheet = Image.new("RGB", (len(frames) * tile, tile * 2 + 32), "#222")
    draw = ImageDraw.Draw(sheet)
    for column, (frame_no, image) in enumerate(frames):
        x = column * tile
        sheet.paste(composite(image, (0, 0, 0, 255)).resize((tile, tile), Image.Resampling.LANCZOS), (x, 0))
        sheet.paste(composite(image, (255, 255, 255, 255)).resize((tile, tile), Image.Resampling.LANCZOS), (x, tile))
        draw.text((x + 5, tile * 2 + 6), f"frame {frame_no:02d} - semantic person/sword only", fill="white")
    sheet.save(REVIEW / "matanyone_probe_black_white.jpg", quality=96)

    combined_sheet = Image.new("RGB", (len(combined_frames) * tile, tile * 2 + 32), "#222")
    draw = ImageDraw.Draw(combined_sheet)
    for column, (frame_no, image) in enumerate(combined_frames):
        x = column * tile
        combined_sheet.paste(composite(image, (0, 0, 0, 255)).resize((tile, tile), Image.Resampling.LANCZOS), (x, 0))
        combined_sheet.paste(composite(image, (255, 255, 255, 255)).resize((tile, tile), Image.Resampling.LANCZOS), (x, tile))
        draw.text((x + 5, tile * 2 + 6), f"frame {frame_no:02d} - person + gold effect", fill="white")
    combined_sheet.save(REVIEW / "combined_probe_black_white.jpg", quality=96)
    print(REVIEW)


if __name__ == "__main__":
    main()
