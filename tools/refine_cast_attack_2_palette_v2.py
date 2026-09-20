from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1\keyframes_v2\cast_attack_2\color_cleanup_v1")
TRANS = ROOT / "transparent"
OUT = ROOT.parent / "color_cleanup_v2"


def load_numbered(folder: Path, index: int) -> Path:
    return sorted(folder.glob(f"{index:02d}_*.png"))[0]


def masks(rgb: np.ndarray, alpha: np.ndarray) -> dict[str, np.ndarray]:
    r, g, b = [rgb[..., i].astype(np.int16) for i in range(3)]
    fg = alpha > 128
    garment = fg & (r < 120) & (g >= r + 5) & (b >= r + 9) & (g < 150) & (b < 175)
    hair = fg & (r > 125) & (g > 80) & (r >= g + 8) & (g >= b + 12) & (b < 175)
    skin = fg & (r > 160) & (g > 105) & (b > 95) & (r >= g + 8) & (g >= b - 8)
    return {"garment": garment, "hair": hair, "skin": skin, "foreground": fg}


def median_sv(rgb: np.ndarray, mask: np.ndarray) -> tuple[float, float]:
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV).astype(np.float32)
    return float(np.median(hsv[..., 1][mask])), float(np.median(hsv[..., 2][mask]))


def smooth_chroma(rgb: np.ndarray, region: np.ndarray, strength: float) -> np.ndarray:
    lab = cv2.cvtColor(rgb, cv2.COLOR_RGB2LAB).astype(np.float32)
    result = lab.copy()
    for channel in (1, 2):
        smooth = cv2.bilateralFilter(lab[..., channel], d=7, sigmaColor=14, sigmaSpace=5)
        result[..., channel][region] = (
            lab[..., channel][region] * (1.0 - strength) + smooth[region] * strength
        )
    return cv2.cvtColor(np.clip(result, 0, 255).astype(np.uint8), cv2.COLOR_LAB2RGB)


def shift_sv(
    rgb: np.ndarray,
    region: np.ndarray,
    target_s: float,
    target_v: float,
    amount: float,
) -> np.ndarray:
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV).astype(np.float32)
    current_s = float(np.median(hsv[..., 1][region]))
    current_v = float(np.median(hsv[..., 2][region]))
    s_shift = (target_s - current_s) * amount
    v_shift = (target_v - current_v) * amount
    hsv[..., 1][region] = np.clip(hsv[..., 1][region] + s_shift, 0, 255)
    hsv[..., 2][region] = np.clip(hsv[..., 2][region] + v_shift, 0, 255)
    return cv2.cvtColor(hsv.astype(np.uint8), cv2.COLOR_HSV2RGB)


def make_sheet(paths: list[Path], output: Path) -> None:
    tiles: list[Image.Image] = []
    for path in paths:
        im = Image.open(path).convert("RGB")
        im.thumbnail((360, 270), Image.Resampling.LANCZOS)
        tiles.append(im)
    sheet = Image.new("RGB", (16 + len(tiles) * 376, 330), (30, 30, 30))
    draw = ImageDraw.Draw(sheet)
    draw.text((16, 10), "cast_attack_2 palette/noise refinement v2", fill="white", font=ImageFont.load_default())
    for i, im in enumerate(tiles):
        x = 16 + i * 376
        sheet.paste(im, (x, 42))
        draw.text((x, 315), f"{i + 1:02d}", fill="white", font=ImageFont.load_default())
    sheet.save(output)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    source_rgbs: list[np.ndarray] = []
    source_alphas: list[np.ndarray] = []
    source_paths: list[Path] = []
    for index in range(1, 6):
        path = load_numbered(ROOT, index)
        transparent = load_numbered(TRANS, index)
        source_paths.append(path)
        source_rgbs.append(np.array(Image.open(path).convert("RGB")))
        source_alphas.append(np.array(Image.open(transparent).convert("RGBA"))[..., 3])

    # Frames 1 and 2 are the accepted, cleaner palette references.
    target: dict[str, tuple[float, float]] = {}
    for region_name in ("garment", "hair", "skin"):
        values = []
        for i in (0, 1):
            region = masks(source_rgbs[i], source_alphas[i])[region_name]
            values.append(median_sv(source_rgbs[i], region))
        target[region_name] = tuple(np.mean(values, axis=0))

    outputs: list[Path] = []
    for i, (path, rgb, alpha) in enumerate(zip(source_paths, source_rgbs, source_alphas), start=1):
        if i >= 3:
            original_masks = masks(rgb, alpha)
            kernel = np.ones((5, 5), np.uint8)
            smooth_regions = cv2.dilate(
                (original_masks["hair"] | original_masks["garment"]).astype(np.uint8),
                kernel,
                iterations=1,
            ).astype(bool)
            smooth_regions &= original_masks["foreground"]
            # Reduce colored flecks without blurring luminance edges or line art.
            rgb = smooth_chroma(rgb, smooth_regions, 1.0)
            current_masks = masks(rgb, alpha)
            rgb = shift_sv(rgb, current_masks["garment"], *target["garment"], amount=1.0)
            current_masks = masks(rgb, alpha)
            rgb = shift_sv(rgb, current_masks["hair"], *target["hair"], amount=1.0)
            current_masks = masks(rgb, alpha)
            rgb = shift_sv(rgb, current_masks["skin"], *target["skin"], amount=0.68)

            # A restrained global colored-pixel correction removes the washed-out
            # cast while preserving white collar/socks and black line work.
            hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV).astype(np.float32)
            colored = (alpha > 128) & (hsv[..., 1] > 22) & (hsv[..., 2] > 55)
            hsv[..., 1][colored] = np.clip(hsv[..., 1][colored] * 1.08, 0, 255)
            hsv[..., 2][colored] = np.clip(hsv[..., 2][colored] * 0.94, 0, 255)
            rgb = cv2.cvtColor(hsv.astype(np.uint8), cv2.COLOR_HSV2RGB)

            # Frame 5 has extra chroma noise around the eye and face. Smooth only
            # chroma on the upper head region, leaving luminance/eye geometry intact.
            if i == 5:
                h, w = alpha.shape
                face_region = np.zeros_like(alpha, dtype=bool)
                face_region[int(h * 0.12):int(h * 0.39), int(w * 0.46):int(w * 0.69)] = True
                face_region &= alpha > 128
                rgb = smooth_chroma(rgb, face_region, 1.0)

        output = OUT / f"{i:02d}_{path.stem.replace('_clean', '')}_palette_clean.png"
        Image.fromarray(rgb).save(output)
        outputs.append(output)

    make_sheet(outputs, OUT / "contact_palette_clean_v2.png")
    print("Reference HSV medians:", target)


if __name__ == "__main__":
    main()
