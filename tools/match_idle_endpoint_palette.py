from pathlib import Path

import numpy as np
from PIL import Image


SOURCE = Path(r"D:\StSmod\art_preview\video\idle_source_frame059.png")
OUTPUT_DIR = Path(r"D:\StSmod\art_preview\video\idle_endpoint_palette_v2")


def main() -> None:
    image = Image.open(SOURCE).convert("RGB")
    rgb = np.asarray(image, dtype=np.float32)
    maximum = rgb.max(axis=2)
    minimum = rgb.min(axis=2)

    # Preserve the chroma background byte-for-byte.  Only grade pixels that
    # belong to the character/sword, with a short soft transition at the key.
    purple_score = np.minimum(
        rgb[:, :, 2] - rgb[:, :, 1] * 1.25,
        rgb[:, :, 0] - rgb[:, :, 1] * 1.15,
    )
    chroma = maximum - minimum
    foreground = 1.0 - np.clip((purple_score - 2.0) / 18.0, 0.0, 1.0)
    foreground *= np.clip((chroma - 8.0) / 18.0, 0.0, 1.0)
    neutral = chroma <= 22.0
    foreground[neutral] = 1.0

    luminance = 0.2126 * rgb[:, :, 0] + 0.7152 * rgb[:, :, 1] + 0.0722 * rgb[:, :, 2]
    # Quantile-matched curve derived from the supplied palette reference:
    # lift dark facial/metal detail slightly, lower white-cloth highlights.
    source_knots = np.array([0, 72.5, 135.4, 187.1, 220.5, 227.3, 255], dtype=np.float32)
    target_knots = np.array([0, 82.2, 138.2, 182.3, 207.5, 214.1, 239], dtype=np.float32)
    target_luminance = np.interp(luminance, source_knots, target_knots)
    ratio = target_luminance / np.maximum(luminance, 1.0)
    graded = np.clip(rgb * ratio[:, :, None], 0, 255)
    # Slightly reduce chroma to match the softer reference without changing hue.
    gray = target_luminance[:, :, None]
    graded = gray + (graded - gray) * 0.95
    result = rgb * (1.0 - foreground[:, :, None]) + graded * foreground[:, :, None]

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out = Image.fromarray(np.clip(result + 0.5, 0, 255).astype(np.uint8), "RGB")
    out.save(OUTPUT_DIR / "idle_start.png")
    out.save(OUTPUT_DIR / "idle_end.png")


if __name__ == "__main__":
    main()
