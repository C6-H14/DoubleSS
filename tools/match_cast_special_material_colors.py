from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
FRAMES = ROOT / "art_candidates" / "video_source" / "cast_special" / "keyframes_v1" / "purple_1536"
BACKUP = FRAMES / "before_material_color_match"

REFERENCE_NAMES = [
    "cast_special_00_start_purple.png",
    "cast_special_01_purple.png",
    "cast_special_06_end_purple.png",
]
TARGET_NAMES = [
    "cast_special_03_purple.png",
    "cast_special_04_purple.png",
    "cast_special_05_purple.png",
]


def masks(rgb: np.ndarray) -> dict[str, np.ndarray]:
    r, g, b = (rgb[..., i].astype(np.int16) for i in range(3))
    yy = np.indices(r.shape)[0]
    return {
        "hair": (r > 145) & (g > 95) & (b < 175) & ((r - b) > 35) & (g > b),
        "cloth": (r < 105) & (g > 42) & (g < 145) & (b > 42) & (b < 150) & (g >= r * 1.05),
        "shoe": (
            (r > 45)
            & (r < 185)
            & (g > 20)
            & (g < 130)
            & (b < 115)
            & (r > g * 1.12)
            & (yy > r.shape[0] * 0.55)
        ),
    }


def reference_colors() -> dict[str, np.ndarray]:
    samples: dict[str, list[np.ndarray]] = {"hair": [], "cloth": [], "shoe": []}
    for name in REFERENCE_NAMES:
        rgb = np.asarray(Image.open(FRAMES / name).convert("RGB"), dtype=np.uint8)
        for material, mask in masks(rgb).items():
            samples[material].append(rgb[mask].astype(np.float32))
    return {
        material: np.concatenate(values, axis=0).mean(axis=0)
        for material, values in samples.items()
    }


def correct(rgb: np.ndarray, references: dict[str, np.ndarray]) -> np.ndarray:
    original = rgb.astype(np.float32)
    result = original.copy()
    for material, mask in masks(rgb).items():
        current = original[mask].mean(axis=0)
        delta = references[material] - current

        # Feather the material mask slightly, while keeping the purple backdrop
        # excluded. This avoids hard color seams at antialiased edges.
        weight = cv2.GaussianBlur(mask.astype(np.float32), (0, 0), 0.85)
        weight = np.clip(weight, 0.0, 1.0)[..., None]
        shifted = np.clip(original + delta, 0.0, 255.0)
        result = result * (1.0 - weight) + shifted * weight
    return np.clip(np.round(result), 0, 255).astype(np.uint8)


def main() -> None:
    BACKUP.mkdir(exist_ok=True)
    references = reference_colors()
    print("reference colors:", {k: np.round(v, 1).tolist() for k, v in references.items()})
    for name in TARGET_NAMES:
        source = FRAMES / name
        backup = BACKUP / name
        if not backup.exists():
            shutil.copy2(source, backup)
        rgb = np.asarray(Image.open(source).convert("RGB"), dtype=np.uint8)
        corrected = correct(rgb, references)
        Image.fromarray(corrected, "RGB").save(source, optimize=True)
        print(source)


if __name__ == "__main__":
    main()
