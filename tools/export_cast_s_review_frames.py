"""Export game-aligned cast_s frames on a dark review background.

The staged runtime PNGs store premultiplied RGB, so composite them without
multiplying their colour by alpha a second time.
"""

from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\character_runtime_v45_alt_cast_s_aligned")
SOURCE = ROOT / "images"
OUTPUT = ROOT / "review_frames_on_dark_45f_60fps"
BACKGROUND = np.array([32, 32, 32], dtype=np.float32)


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    paths = sorted(SOURCE.glob("cast_s_alpha_*.png"))
    if len(paths) != 45:
        raise RuntimeError(f"Expected 45 staged frames, found {len(paths)}")
    for path in paths:
        source = np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32)
        alpha = source[:, :, 3:4] / 255.0
        rgb = source[:, :, :3] + BACKGROUND * (1.0 - alpha)
        rgb = np.clip(np.rint(rgb), 0, 255).astype(np.uint8)
        Image.fromarray(rgb, "RGB").save(OUTPUT / path.name)
    print(f"Exported {len(paths)} review frames to {OUTPUT}")


if __name__ == "__main__":
    main()
