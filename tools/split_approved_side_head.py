from __future__ import annotations

import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "character_side_42deg_transparent_attempt.png"
OUTPUT = ROOT / "art_candidates" / "side_head_split_v1"
ALIGNED = OUTPUT / "aligned"
CROPPED = OUTPUT / "cropped"


def polygon_mask(size: tuple[int, int], points: list[tuple[int, int]]) -> Image.Image:
    scale = 4
    mask = Image.new("L", (size[0] * scale, size[1] * scale), 0)
    draw = ImageDraw.Draw(mask)
    draw.polygon([(x * scale, y * scale) for x, y in points], fill=255)
    return mask.resize(size, Image.Resampling.LANCZOS)


def alpha_intersection(source_alpha: Image.Image, mask: Image.Image) -> Image.Image:
    return Image.fromarray(
        np.minimum(np.asarray(source_alpha, dtype=np.uint8), np.asarray(mask, dtype=np.uint8)),
        "L",
    )


def save_layer(source: Image.Image, alpha: Image.Image, name: str) -> dict[str, object]:
    rgba = source.copy()
    rgba.putalpha(alpha)
    aligned_path = ALIGNED / f"{name}.png"
    rgba.save(aligned_path)

    bbox = alpha.getbbox()
    if bbox is None:
        raise RuntimeError(f"empty layer: {name}")
    pad = 8
    left = max(0, bbox[0] - pad)
    top = max(0, bbox[1] - pad)
    right = min(source.width, bbox[2] + pad)
    bottom = min(source.height, bbox[3] + pad)
    crop_box = (left, top, right, bottom)
    cropped = rgba.crop(crop_box)
    cropped_path = CROPPED / f"{name}.png"
    cropped.save(cropped_path)

    center_x = (left + right) / 2
    center_y = (top + bottom) / 2
    return {
        "aligned": str(aligned_path),
        "cropped": str(cropped_path),
        "crop_box": crop_box,
        "width": cropped.width,
        "height": cropped.height,
        "spine_x_at_scale_1": center_x - source.width / 2,
        "spine_y_at_scale_1": source.height / 2 - center_y,
    }


def checkerboard(size: tuple[int, int], cell: int = 16) -> Image.Image:
    yy, xx = np.indices((size[1], size[0]))
    board = ((xx // cell + yy // cell) % 2) * 32 + 196
    rgb = np.stack([board, board, board], axis=2).astype(np.uint8)
    return Image.fromarray(rgb, "RGB").convert("RGBA")


def extract_head_alpha(source: Image.Image, allowed_shape: Image.Image) -> Image.Image:
    """Remove the generated black/gold backdrop without repainting the character."""
    bgr = cv2.cvtColor(np.asarray(source.convert("RGB")), cv2.COLOR_RGB2BGR)
    allowed = np.asarray(allowed_shape, dtype=np.uint8) >= 128
    mask = np.full((source.height, source.width), cv2.GC_BGD, dtype=np.uint8)
    mask[allowed] = cv2.GC_PR_FGD

    # Conservative interior strokes. GrabCut expands these seeds to the real
    # high-contrast silhouette and keeps background holes between hair locks.
    seeds = Image.new("L", source.size, 0)
    draw = ImageDraw.Draw(seeds)
    draw.ellipse((405, 52, 654, 310), fill=255)  # crown, bangs and face
    draw.polygon([(337, 116), (405, 101), (421, 214), (393, 384), (342, 428), (315, 350)], fill=255)
    draw.polygon([(617, 101), (680, 117), (727, 235), (711, 382), (666, 430), (632, 337)], fill=255)
    seed_array = (np.asarray(seeds, dtype=np.uint8) >= 128) & allowed
    mask[seed_array] = cv2.GC_FGD

    bg_model = np.zeros((1, 65), np.float64)
    fg_model = np.zeros((1, 65), np.float64)
    cv2.grabCut(bgr, mask, None, bg_model, fg_model, 8, cv2.GC_INIT_WITH_MASK)
    matte = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0).astype(np.uint8)
    matte[~allowed] = 0
    matte = cv2.GaussianBlur(matte, (0, 0), 0.55)
    matte[matte < 8] = 0
    return Image.fromarray(matte, "L")


def main() -> None:
    ALIGNED.mkdir(parents=True, exist_ok=True)
    CROPPED.mkdir(parents=True, exist_ok=True)
    source = Image.open(SOURCE).convert("RGBA")
    size = source.size

    # Exact-pixel extraction masks. These shapes follow the accepted artwork's
    # hair silhouette and stop above the blouse/collar. No pixels are redrawn.
    center_shape = polygon_mask(
        size,
        [
            (398, 35), (646, 35), (680, 112), (668, 220), (634, 292),
            (578, 340), (548, 354), (477, 350), (438, 326), (401, 279),
            (380, 190), (382, 96),
        ],
    )
    left_hair_shape = polygon_mask(
        size,
        [
            (296, 61), (455, 43), (476, 126), (454, 225), (420, 323),
            (398, 425), (353, 480), (298, 460), (268, 374), (262, 228),
        ],
    )
    right_hair_shape = polygon_mask(
        size,
        [
            (568, 45), (689, 62), (748, 130), (764, 248), (746, 382),
            (705, 465), (647, 481), (602, 431), (581, 331), (568, 224),
        ],
    )
    complete_shape = Image.fromarray(
        np.maximum.reduce(
            [
                np.asarray(center_shape, dtype=np.uint8),
                np.asarray(left_hair_shape, dtype=np.uint8),
                np.asarray(right_hair_shape, dtype=np.uint8),
            ]
        ),
        "L",
    )
    # The production source has a true alpha channel from the background-only
    # extraction pass. Preserve it directly so the accepted character pixels
    # and fine hair edges are not repainted.
    alpha = source.getchannel("A")
    complete = alpha_intersection(alpha, complete_shape)
    # Remove blouse/collar/bow pixels accidentally covered by the broad hair
    # polygons. Below the jaw, retained pixels must have a warm skin/hair
    # chroma; the central neck overlap additionally excludes the gold bow.
    rgba_array = np.asarray(source, dtype=np.uint8)
    red = rgba_array[:, :, 0].astype(np.int16)
    green = rgba_array[:, :, 1].astype(np.int16)
    blue = rgba_array[:, :, 2].astype(np.int16)
    yy, xx = np.indices((source.height, source.width))
    warm_pixel = (red > 145) & (green > 95) & ((red - blue) > 18)
    side_hair_zone = (xx < 430) | (xx > 600)
    neck_shape = np.asarray(
        polygon_mask(size, [(420, 185), (628, 185), (625, 278), (578, 334), (548, 354), (480, 350), (434, 310), (410, 248)]),
        dtype=np.uint8,
    ) >= 128
    skin_pixel = (
        (red > 190)
        & (green > 135)
        & (blue > 115)
        & ((red - blue) > 8)
        & ((red - blue) < 105)
    )
    allowed_below_jaw = (side_hair_zone & warm_pixel) | (neck_shape & skin_pixel)
    complete_array_pre = np.asarray(complete, dtype=np.uint8)
    complete = Image.fromarray(
        np.where((yy <= 240) | allowed_below_jaw, complete_array_pre, 0).astype(np.uint8),
        "L",
    )
    # The existing torso attachment already supplies the collar/neck overlap.
    # End the central head under the jaw so no blouse or collar fragments are
    # carried into the replacement head; side ponytails remain untouched.
    complete_array_trimmed = np.asarray(complete, dtype=np.uint8)
    central_below_jaw = (xx >= 430) & (xx <= 600) & (yy > 292)
    complete = Image.fromarray(np.where(central_below_jaw, 0, complete_array_trimmed).astype(np.uint8), "L")
    # Drop isolated one-pixel/very-small remnants from the removed blouse and
    # bow while preserving the single connected head-and-hair component.
    complete_array_clean = np.asarray(complete, dtype=np.uint8).copy()
    component_count, component_labels, component_stats, _ = cv2.connectedComponentsWithStats(
        (complete_array_clean >= 2).astype(np.uint8), connectivity=8
    )
    for component_id in range(1, component_count):
        if component_stats[component_id, cv2.CC_STAT_AREA] < 40:
            complete_array_clean[component_labels == component_id] = 0
    complete_array_clean[(yy > 365) & (xx >= 575) & (xx <= 620)] = 0
    complete = Image.fromarray(complete_array_clean, "L")

    # Two ornaments are isolated first so they remain independently placeable.
    ornament_near_shape = polygon_mask(size, [(419, 42), (488, 42), (493, 133), (420, 137)])
    ornament_far_shape = polygon_mask(size, [(594, 65), (649, 65), (656, 142), (594, 145)])
    complete_array = np.asarray(complete, dtype=np.uint8)
    near_region = np.asarray(ornament_near_shape, dtype=np.uint8) >= 128
    far_region = np.asarray(ornament_far_shape, dtype=np.uint8) >= 128
    ornament_near = Image.fromarray(np.where(near_region, complete_array, 0).astype(np.uint8), "L")
    ornament_far = Image.fromarray(np.where(far_region, complete_array, 0).astype(np.uint8), "L")

    # Central face + bangs. It intentionally includes the accepted visible
    # facial pixels and front hair as one rigid first-pass head attachment.
    front_shape = polygon_mask(
        size,
        [
            (407, 42), (636, 40), (670, 113), (664, 225), (625, 291),
            (575, 332), (548, 350), (477, 347), (438, 326), (390, 228),
            (386, 110),
        ],
    )
    front_region = np.asarray(front_shape, dtype=np.uint8) >= 128
    front_region &= ~near_region & ~far_region
    face_front = Image.fromarray(np.where(front_region, complete_array, 0).astype(np.uint8), "L")

    # Partition the remainder into back hair/ponytails. Subtraction creates a
    # pixel-perfect reassembly with no duplicated opaque pixels.
    assigned_region = near_region | far_region | front_region
    back_hair = Image.fromarray(np.where(~assigned_region, complete_array, 0).astype(np.uint8), "L")

    manifest = {
        "source": str(SOURCE),
        "canvas": {"width": source.width, "height": source.height},
        "notes": [
            "All visible pixels come directly from the approved 42-degree artwork.",
            "face_front is a rigid first-pass face-plus-bangs attachment.",
            "back_hair includes the remaining crown, rear hair and both ponytails.",
            "ornament_near and ornament_far remain independent attachments.",
        ],
        "layers": {},
    }
    layers = {
        "head_complete_exact": complete,
        "head_back_hair": back_hair,
        "head_face_front": face_front,
        "head_ornament_near": ornament_near,
        "head_ornament_far": ornament_far,
    }
    for name, layer_alpha in layers.items():
        manifest["layers"][name] = save_layer(source, layer_alpha, name)

    # Rebuild from the four split layers and verify exact equality inside the
    # extracted head mask.
    rebuilt = Image.new("RGBA", size, (0, 0, 0, 0))
    for name in ("head_back_hair", "head_face_front", "head_ornament_near", "head_ornament_far"):
        rebuilt.alpha_composite(Image.open(ALIGNED / f"{name}.png").convert("RGBA"))
    rebuilt.save(OUTPUT / "head_rebuilt_exact.png")

    src_a = np.asarray(source)
    reb_a = np.asarray(rebuilt)
    mask = np.asarray(complete) > 0
    max_diff = int(np.abs(src_a.astype(np.int16) - reb_a.astype(np.int16))[mask].max(initial=0))
    manifest["rebuild_max_channel_difference"] = max_diff
    (OUTPUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    # Visual QA sheet.
    names = list(layers)
    thumb_size = (300, 450)
    sheet = Image.new("RGBA", (thumb_size[0] * len(names), thumb_size[1] + 42), (36, 36, 36, 255))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for i, name in enumerate(names):
        layer = Image.open(ALIGNED / f"{name}.png").convert("RGBA")
        bbox = layer.getchannel("A").getbbox()
        cut = layer.crop(bbox)
        cut.thumbnail((thumb_size[0] - 20, thumb_size[1] - 20), Image.Resampling.LANCZOS)
        tile = checkerboard(thumb_size)
        tile.alpha_composite(cut, ((thumb_size[0] - cut.width) // 2, (thumb_size[1] - cut.height) // 2))
        sheet.alpha_composite(tile, (i * thumb_size[0], 0))
        draw.text((i * thumb_size[0] + 8, thumb_size[1] + 12), name, fill=(255, 255, 255, 255), font=font)
    sheet.convert("RGB").save(OUTPUT / "split_preview.jpg", quality=94)

    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
