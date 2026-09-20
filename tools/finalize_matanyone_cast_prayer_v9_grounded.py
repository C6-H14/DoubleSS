from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "cast_prayer"
SOURCE = BASE / "preview_v6" / "frames"
KEYED = BASE / "preview_v8_border_connected" / "alpha_frames"
PHA = (ROOT / "art_candidates" / "matting_tests" / "matanyone_cast_prayer_probe" /
       "output" / "cast_prayer_41_prayer1024" / "pha")
OUTPUT = BASE / "preview_v9_matanyone_grounded"
FRAMES = OUTPUT / "alpha_frames"


def semantic_person(frame_no: int, pha_paths: list[Path]) -> tuple[np.ndarray, np.ndarray]:
    rgb = np.asarray(Image.open(SOURCE / f"cast_prayer_{frame_no:03d}.png").convert("RGB"))
    alpha = cv2.imread(str(pha_paths[frame_no - 1]), cv2.IMREAD_GRAYSCALE)
    alpha = cv2.resize(alpha, (rgb.shape[1], rgb.shape[0]), interpolation=cv2.INTER_LINEAR)
    alpha[alpha < 3] = 0
    solid = alpha >= 235
    if not solid.any():
        raise RuntimeError(f"No semantic foreground in frame {frame_no}")
    _, nearest = distance_transform_edt(~solid, return_indices=True)
    clean = rgb.copy()
    fringe = (alpha > 0) & (alpha < 235)
    clean[fringe] = rgb[nearest[0], nearest[1]][fringe]
    clean[alpha == 0] = 0
    return clean, alpha


def add_gold_effect(frame_no: int, person_rgb: np.ndarray, person_alpha: np.ndarray) -> np.ndarray:
    source = np.asarray(Image.open(SOURCE / f"cast_prayer_{frame_no:03d}.png").convert("RGB"))
    keyed_alpha = np.asarray(Image.open(KEYED / f"cast_prayer_{frame_no:03d}.png").convert("RGBA").getchannel("A"))
    hsv = cv2.cvtColor(source, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    outside = person_alpha < 160
    gold_seed = outside & (h >= 7) & (h <= 42) & (s >= 45) & (v >= 105) & (keyed_alpha >= 12)
    region = cv2.dilate(gold_seed.astype(np.uint8), np.ones((19, 19), np.uint8), iterations=1).astype(bool)
    effect_alpha = keyed_alpha.copy()
    effect_alpha[~region] = 0
    effect_alpha[person_alpha >= 210] = 0
    # Reject saturated magenta/salmon plate remnants during the fade.
    pink = (h >= 125) & (s >= 16)
    effect_alpha[pink] = 0
    effect_alpha[effect_alpha < 5] = 0

    alpha = np.maximum(person_alpha, effect_alpha)
    rgb = source.copy()
    semantic = person_alpha > 0
    rgb[semantic] = person_rgb[semantic]
    rgb[alpha == 0] = 0
    return np.dstack((rgb, alpha))


def shoe_ground_y(source_rgb: np.ndarray, person_alpha: np.ndarray) -> int:
    hsv = cv2.cvtColor(source_rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    mask = ((h <= 22) & (s >= 45) & (v >= 25) & (v <= 205) & (person_alpha >= 96)).astype(np.uint8)
    mask[:600] = 0
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    shoe = np.zeros_like(mask)
    for label in range(1, count):
        x, y, width, height, area = stats[label]
        if area >= 35 and y >= 600 and width <= 150 and height <= 110:
            shoe[labels == label] = 1
    ys, _ = np.where(shoe)
    if not len(ys):
        raise RuntimeError("Unable to find shoe ground")
    return int(np.percentile(ys, 99.5))


def translate_y(rgba: np.ndarray, dy: int) -> np.ndarray:
    matrix = np.float32([[1, 0, 0], [0, 1, dy]])
    return cv2.warpAffine(rgba, matrix, (rgba.shape[1], rgba.shape[0]), flags=cv2.INTER_LANCZOS4,
                          borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))


def checkerboard(size, cell=32):
    image = Image.new("RGBA", size, (220, 220, 220, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], cell):
        for x in range(0, size[0], cell):
            if (x // cell + y // cell) % 2:
                draw.rectangle((x, y, x + cell - 1, y + cell - 1), fill=(165, 165, 165, 255))
    return image


def composite(frame: Image.Image, background: Image.Image) -> Image.Image:
    return Image.alpha_composite(background.copy(), frame)


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    pha_paths = sorted(PHA.glob("*.png"))
    if len(pha_paths) != 41:
        raise RuntimeError(f"Expected 41 alpha frames, found {len(pha_paths)}")

    built = []
    grounds = []
    unshifted = []
    for frame_no in range(1, 42):
        person_rgb, person_alpha = semantic_person(frame_no, pha_paths)
        source_rgb = np.asarray(Image.open(SOURCE / f"cast_prayer_{frame_no:03d}.png").convert("RGB"))
        grounds.append(shoe_ground_y(source_rgb, person_alpha))
        unshifted.append(add_gold_effect(frame_no, person_rgb, person_alpha))
    target = grounds[0]
    shifts = [target - ground for ground in grounds]

    for frame_no, (rgba, dy) in enumerate(zip(unshifted, shifts), 1):
        stable = translate_y(rgba, dy)
        image = Image.fromarray(stable, "RGBA")
        image.save(FRAMES / f"cast_prayer_{frame_no:03d}.png", optimize=True)
        built.append(image)

    # GIF delays are stored in centiseconds. 29 ms is silently truncated to
    # 20 ms by Pillow, producing an 820 ms preview. Use 38*30 + 3*20 = 1200 ms.
    durations = [30] * len(built)
    for index in (13, 26, 40):
        durations[index] = 20
    backgrounds = {
        "black": Image.new("RGBA", built[0].size, (0, 0, 0, 255)),
        "checker": checkerboard(built[0].size),
    }
    for name, background in backgrounds.items():
        preview = [composite(frame, background).resize((512, 512), Image.Resampling.LANCZOS).convert("RGB") for frame in built]
        preview[0].save(OUTPUT / f"cast_prayer_{name}_1200ms.gif", save_all=True, append_images=preview[1:],
                        duration=durations, loop=0, disposal=2)

    cols, rows, thumb = 7, 6, 320
    contact = Image.new("RGB", (cols * thumb, rows * (thumb + 28)), "#222")
    draw = ImageDraw.Draw(contact)
    checker = backgrounds["checker"]
    for i, frame in enumerate(built):
        x, y = (i % cols) * thumb, (i // cols) * (thumb + 28)
        contact.paste(composite(frame, checker).resize((thumb, thumb), Image.Resampling.LANCZOS).convert("RGB"), (x, y))
        draw.text((x + 5, y + thumb + 5), f"frame {i+1:02d} dy={shifts[i]:+d}", fill="white")
    contact.save(OUTPUT / "cast_prayer_grounded_contact.jpg", quality=96)
    (OUTPUT / "ground_alignment.json").write_text(json.dumps({
        "target_shoe_ground_y": target,
        "source_ground_y": grounds,
        "vertical_shift": shifts,
    }, indent=2) + "\n", encoding="utf-8")
    print(f"frames=41 target_ground={target} shift={min(shifts)}..{max(shifts)} output={OUTPUT}")


if __name__ == "__main__":
    main()
