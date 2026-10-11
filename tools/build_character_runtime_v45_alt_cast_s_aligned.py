from pathlib import Path

import cv2
import numpy as np
from PIL import Image

import build_character_runtime_v44_alt_cast_s as config


ROOT = Path(r"D:\StSmod\DoubleSS")
builder = config.builder
builder.OUT = ROOT / "art_candidates" / "character_runtime_v45_alt_cast_s_aligned"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"

# Measured against alt_idle_001 / alt_idle_241 runtime canvases: the previous
# cast_s endpoint was 10 px too tall; its left shoe extended 14 px too far
# left, while the right shoe was slightly wider. This maps head top and both
# shoe silhouettes to their idle coordinates without cropping the fire burst.
MATRIX = np.float32([[0.932, 0.0, 38.0], [0.0, 0.974, 20.0]])
original_bleed = builder.builder.add_runtime_bleed


def align_and_bleed(image: Image.Image, radius: float = 4.0) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32)
    alpha = rgba[:, :, 3:4] / 255.0
    premultiplied = rgba[:, :, :3] * alpha
    warped_alpha = cv2.warpAffine(alpha, MATRIX, (1024, 1024), flags=cv2.INTER_LINEAR,
                                  borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    warped_rgb = cv2.warpAffine(premultiplied, MATRIX, (1024, 1024), flags=cv2.INTER_LINEAR,
                                borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0))
    warped = np.zeros((1024, 1024, 4), dtype=np.uint8)
    safe_alpha = np.maximum(warped_alpha[:, :, None], 1.0 / 255.0)
    warped[:, :, :3] = np.clip(np.rint(warped_rgb / safe_alpha), 0, 255).astype(np.uint8)
    warped[:, :, 3] = np.clip(np.rint(warped_alpha * 255), 0, 255).astype(np.uint8)
    return original_bleed(Image.fromarray(warped, "RGBA"), radius=radius)


if __name__ == "__main__":
    builder.builder.add_runtime_bleed = align_and_bleed
    builder.main()
