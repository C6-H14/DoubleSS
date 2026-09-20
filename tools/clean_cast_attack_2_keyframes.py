from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1\keyframes_v2\cast_attack_2")
OUT = ROOT / "color_cleanup_v1"
QA_BLACK = OUT / "qa_black"
QA_WHITE = OUT / "qa_white"
TRANSPARENT = OUT / "transparent"

FILES = [
    "middle_01_sword_left_touch_guard.png",
    "middle_02_wipe_complete_enchanted_v2.png",
    "middle_03_charge_complete_left_rear_v8_palm_facing_viewer.png",
    "middle_04_horizontal_slash_midpoint_v1.png",
    "middle_05_horizontal_slash_followthrough_v4_right_hand_grip.png",
]

# Sampled from the cleanest first keyframe. A single exact background color is
# easier to key than five independently generated pink gradients.
TARGET_BG = np.array([251, 52, 160], dtype=np.uint8)


def background_candidate(rgb: np.ndarray) -> np.ndarray:
    """Return high-confidence generated pink-background pixels."""
    r = rgb[..., 0].astype(np.int16)
    g = rgb[..., 1].astype(np.int16)
    b = rgb[..., 2].astype(np.int16)
    return (
        (r >= 205)
        & (g <= 120)
        & (b >= 105)
        & ((r - g) >= 105)
        & ((b - g) >= 55)
    )


def clean_frame(rgb: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    bg = background_candidate(rgb).astype(np.uint8)

    # Close tiny compression holes in the flat backdrop, while protecting the
    # character, sword and purple effect as foreground cores.
    kernel3 = np.ones((3, 3), np.uint8)
    bg = cv2.morphologyEx(bg, cv2.MORPH_CLOSE, kernel3, iterations=1)
    foreground_core = (1 - bg).astype(np.uint8)
    foreground_core = cv2.morphologyEx(foreground_core, cv2.MORPH_OPEN, kernel3, iterations=1)

    # Keep a narrow antialiased edge around true foreground. Everything else
    # becomes exactly the same pink, eliminating gradient/compression noise.
    protected = cv2.dilate(foreground_core, np.ones((5, 5), np.uint8), iterations=1).astype(bool)

    # Mild edge-preserving denoise only. This reduces colored speckles without
    # flattening facial lines, fingers, clothing folds or sword details.
    filtered = cv2.bilateralFilter(rgb, d=5, sigmaColor=7, sigmaSpace=3)
    cleaned = rgb.copy()
    cleaned[protected] = filtered[protected]
    cleaned[~protected] = TARGET_BG

    # Build a conservative matte solely for QA previews. It is not used as a
    # final production alpha and therefore cannot damage the source artwork.
    matte = cv2.dilate(foreground_core, np.ones((3, 3), np.uint8), iterations=1)
    matte = cv2.GaussianBlur((matte * 255).astype(np.uint8), (3, 3), 0)
    return cleaned, matte


def decontaminate(rgb: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """Estimate alpha against the known pink and remove pink edge spill."""
    src = rgb.astype(np.float32)
    bg = TARGET_BG.astype(np.float32)
    distance = np.linalg.norm(src - bg[None, None, :], axis=2)
    alpha = np.clip((distance - 5.0) / 38.0, 0.0, 1.0)
    alpha = cv2.GaussianBlur(alpha, (3, 3), 0)
    alpha[distance >= 48.0] = 1.0
    alpha[distance <= 4.0] = 0.0

    # Generated images contain a darker red-pink contour that is not removed by
    # ordinary distance keying. Suppress only that hue in the outer 3 px of the
    # foreground; blue-dominant purple sword magic is deliberately excluded.
    r, g, b = src[..., 0], src[..., 1], src[..., 2]
    core = (~background_candidate(rgb)).astype(np.uint8)
    inner_distance = cv2.distanceTransform(core, cv2.DIST_L2, 3)
    outer_edge = (inner_distance > 0.0) & (inner_distance <= 8.0)
    pink_spill = (
        (r >= 125.0)
        & ((r - g) >= 65.0)
        & ((b - g) >= 28.0)
        & (r >= b + 8.0)
    )
    pink_background = background_candidate(rgb)
    alpha[pink_spill & (outer_edge | pink_background)] = 0.0

    # Do not divide edge RGB by tiny alpha values: that creates cyan/blue
    # compensation fringes. The cleaned original RGB is retained and only the
    # matte controls visibility.
    foreground = rgb.copy()
    return foreground, np.clip(alpha * 255.0, 0, 255).astype(np.uint8)


def composite(rgb: np.ndarray, matte: np.ndarray, color: tuple[int, int, int]) -> np.ndarray:
    a = matte.astype(np.float32)[..., None] / 255.0
    bg = np.empty_like(rgb)
    bg[:] = np.array(color, dtype=np.uint8)
    return np.clip(rgb.astype(np.float32) * a + bg.astype(np.float32) * (1.0 - a), 0, 255).astype(np.uint8)


def make_contact_sheet(paths: list[Path], out_path: Path, title: str) -> None:
    thumbs: list[Image.Image] = []
    for path in paths:
        im = Image.open(path).convert("RGB")
        im.thumbnail((360, 270), Image.Resampling.LANCZOS)
        thumbs.append(im)
    pad, label_h = 16, 42
    width = pad + len(thumbs) * (360 + pad)
    height = 270 + label_h + pad * 2
    sheet = Image.new("RGB", (width, height), (32, 32, 32))
    draw = ImageDraw.Draw(sheet)
    draw.text((pad, 10), title, fill=(255, 255, 255), font=ImageFont.load_default())
    x = pad
    for i, im in enumerate(thumbs, start=1):
        sheet.paste(im, (x, label_h))
        draw.text((x, label_h + 272), f"{i:02d}", fill=(255, 255, 255), font=ImageFont.load_default())
        x += 360 + pad
    sheet.save(out_path)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    QA_BLACK.mkdir(parents=True, exist_ok=True)
    QA_WHITE.mkdir(parents=True, exist_ok=True)
    TRANSPARENT.mkdir(parents=True, exist_ok=True)

    cleaned_paths: list[Path] = []
    black_paths: list[Path] = []
    white_paths: list[Path] = []

    for index, name in enumerate(FILES, start=1):
        src = ROOT / name
        rgb = np.array(Image.open(src).convert("RGB"))
        cleaned, _ = clean_frame(rgb)
        foreground, matte = decontaminate(cleaned)

        clean_path = OUT / f"{index:02d}_{src.stem}_clean.png"
        black_path = QA_BLACK / f"{index:02d}.png"
        white_path = QA_WHITE / f"{index:02d}.png"
        transparent_path = TRANSPARENT / f"{index:02d}_{src.stem}_transparent.png"
        Image.fromarray(cleaned).save(clean_path)
        rgba = np.dstack([foreground, matte])
        Image.fromarray(rgba, mode="RGBA").save(transparent_path)
        Image.fromarray(composite(foreground, matte, (0, 0, 0))).save(black_path)
        Image.fromarray(composite(foreground, matte, (255, 255, 255))).save(white_path)
        cleaned_paths.append(clean_path)
        black_paths.append(black_path)
        white_paths.append(white_path)

    make_contact_sheet(cleaned_paths, OUT / "contact_cleaned.png", "cast_attack_2 cleaned pink keyframes")
    make_contact_sheet(black_paths, OUT / "contact_black_qa.png", "black-background edge QA")
    make_contact_sheet(white_paths, OUT / "contact_white_qa.png", "white-background edge QA")


if __name__ == "__main__":
    main()
