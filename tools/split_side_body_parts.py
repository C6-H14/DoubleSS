from __future__ import annotations

import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "character_side_42deg_transparent_attempt.png"
HEAD_DIR = ROOT / "art_candidates" / "side_head_split_v1" / "aligned"
OUT = ROOT / "art_candidates" / "side_body_split_v1"
ALIGNED = OUT / "aligned"
CROPPED = OUT / "cropped"


def poly(size: tuple[int, int], points: list[tuple[int, int]]) -> Image.Image:
    scale = 4
    mask = Image.new("L", (size[0] * scale, size[1] * scale), 0)
    ImageDraw.Draw(mask).polygon([(x * scale, y * scale) for x, y in points], fill=255)
    return mask.resize(size, Image.Resampling.LANCZOS)


def masked_alpha(base: Image.Image, points: list[tuple[int, int]]) -> Image.Image:
    m = np.asarray(poly(base.size, points), dtype=np.uint8)
    a = np.asarray(base.getchannel("A"), dtype=np.uint8)
    return Image.fromarray(np.minimum(a, m), "L")


def union(*alphas: Image.Image) -> Image.Image:
    return Image.fromarray(np.maximum.reduce([np.asarray(a, dtype=np.uint8) for a in alphas]), "L")


def subtract(alpha: Image.Image, cover: Image.Image) -> Image.Image:
    a = np.asarray(alpha, dtype=np.uint8)
    c = np.asarray(cover, dtype=np.uint8)
    return Image.fromarray(np.where(c >= 128, 0, a).astype(np.uint8), "L")


def restrict_x(alpha: Image.Image, minimum: int | None = None, maximum: int | None = None) -> Image.Image:
    a = np.asarray(alpha, dtype=np.uint8)
    xx = np.indices(a.shape)[1]
    keep = np.ones(a.shape, dtype=bool)
    if minimum is not None:
        keep &= xx >= minimum
    if maximum is not None:
        keep &= xx <= maximum
    return Image.fromarray(np.where(keep, a, 0).astype(np.uint8), "L")


def keep_largest_component(alpha: Image.Image) -> Image.Image:
    a = np.asarray(alpha, dtype=np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats((a >= 16).astype(np.uint8), connectivity=8)
    if count <= 2:
        return alpha
    largest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    return Image.fromarray(np.where(labels == largest, a, 0).astype(np.uint8), "L")


def save_layer(source: Image.Image, alpha: Image.Image, name: str) -> dict[str, object]:
    rgba = source.copy()
    rgba.putalpha(alpha)
    rgba.save(ALIGNED / f"{name}.png")
    bbox = alpha.getbbox()
    if bbox is None:
        raise RuntimeError(f"empty layer: {name}")
    pad = 8
    left, top = max(0, bbox[0] - pad), max(0, bbox[1] - pad)
    right, bottom = min(source.width, bbox[2] + pad), min(source.height, bbox[3] + pad)
    crop = rgba.crop((left, top, right, bottom))
    crop.save(CROPPED / f"{name}.png")
    return {
        "aligned": str(ALIGNED / f"{name}.png"),
        "cropped": str(CROPPED / f"{name}.png"),
        "crop_box": [left, top, right, bottom],
        "width": crop.width,
        "height": crop.height,
        "spine_x_at_scale_1": (left + right) / 2 - source.width / 2,
        "spine_y_at_scale_1": source.height / 2 - (top + bottom) / 2,
    }


def main() -> None:
    ALIGNED.mkdir(parents=True, exist_ok=True)
    CROPPED.mkdir(parents=True, exist_ok=True)
    src = Image.open(SOURCE).convert("RGBA")
    size = src.size

    # Visible-pixel regions follow the approved 42-degree artwork. Generous
    # overlaps at joints are deliberate, so rotation never opens a seam.
    masks: dict[str, Image.Image] = {}
    masks["torso_blouse"] = masked_alpha(src, [
        (391, 273), (622, 270), (665, 346), (650, 540),
        (610, 570), (420, 570), (373, 535), (373, 350),
    ])
    masks["neck_collar_bridge"] = masked_alpha(src, [
        (418, 252), (608, 250), (633, 351), (590, 395),
        (440, 389), (390, 339),
    ])
    masks["skirt_with_waistband"] = masked_alpha(src, [
        (393, 498), (625, 493), (704, 595), (742, 786),
        (646, 840), (420, 836), (326, 783), (354, 586),
    ])

    masks["arm_far_complete"] = masked_alpha(src, [
        (347, 293), (438, 290), (463, 441), (438, 601),
        (405, 722), (384, 808), (323, 806), (309, 743),
        (337, 621), (326, 456),
    ])
    masks["arm_far_upper"] = masked_alpha(src, [
        (344, 286), (443, 286), (463, 455), (433, 589),
        (357, 585), (325, 456),
    ])
    masks["arm_far_forearm"] = masked_alpha(src, [
        (351, 535), (444, 538), (423, 690), (395, 760),
        (332, 751), (328, 670),
    ])
    masks["hand_far"] = masked_alpha(src, [
        (326, 707), (405, 702), (405, 821), (315, 821),
    ])

    masks["arm_near_complete"] = masked_alpha(src, [
        (585, 282), (667, 300), (697, 454), (713, 596),
        (766, 704), (783, 785), (735, 821), (686, 773),
        (666, 666), (627, 554), (600, 425),
    ])
    masks["arm_near_upper"] = masked_alpha(src, [
        (579, 278), (670, 292), (702, 461), (697, 584),
        (626, 591), (599, 434),
    ])
    masks["arm_near_forearm"] = masked_alpha(src, [
        (624, 529), (707, 528), (729, 673), (757, 748),
        (697, 773), (661, 674),
    ])
    masks["hand_near"] = masked_alpha(src, [
        (687, 700), (778, 698), (793, 824), (703, 832),
    ])

    masks["leg_far_complete"] = masked_alpha(src, [
        (356, 764), (520, 757), (524, 1087), (487, 1270),
        (452, 1465), (326, 1488), (326, 1360), (367, 1190),
        (383, 1030),
    ])
    masks["leg_far_thigh"] = masked_alpha(src, [
        (353, 751), (527, 750), (526, 1065), (493, 1145),
        (382, 1135), (375, 963),
    ])
    masks["leg_far_lower"] = masked_alpha(src, [
        (372, 1040), (515, 1035), (492, 1285), (458, 1376),
        (340, 1372), (344, 1241),
    ])
    masks["foot_far"] = masked_alpha(src, [
        (326, 1322), (466, 1315), (474, 1498), (311, 1502),
    ])

    masks["leg_near_complete"] = masked_alpha(src, [
        (487, 758), (661, 765), (661, 1020), (630, 1190),
        (622, 1370), (677, 1453), (650, 1510), (502, 1503),
        (495, 1360), (520, 1190), (522, 1010),
    ])
    masks["leg_near_thigh"] = masked_alpha(src, [
        (482, 751), (668, 751), (665, 1048), (635, 1147),
        (518, 1144), (517, 1015),
    ])
    masks["leg_near_lower"] = masked_alpha(src, [
        (514, 1038), (650, 1035), (631, 1289), (625, 1385),
        (510, 1384), (500, 1245),
    ])
    masks["foot_near"] = masked_alpha(src, [
        (500, 1320), (650, 1318), (687, 1518), (491, 1518),
    ])

    # Remove pixels belonging to attachments that sit in front/behind these
    # regions. This avoids static duplicates when an arm, leg, or hair layer
    # rotates away from its setup pose.
    head_cover = Image.open(HEAD_DIR / "head_complete_exact.png").convert("RGBA").getchannel("A")
    head_cover = head_cover.filter(ImageFilter.MaxFilter(15))
    arm_cover = union(masks["arm_far_complete"], masks["arm_near_complete"])
    skirt_cover = masks["skirt_with_waistband"]
    for name in (
        "torso_blouse", "neck_collar_bridge",
        "arm_far_complete", "arm_far_upper", "arm_far_forearm", "hand_far",
        "arm_near_complete", "arm_near_upper", "arm_near_forearm", "hand_near",
    ):
        masks[name] = subtract(masks[name], head_cover)
    for name in ("arm_far_complete", "arm_far_upper", "arm_far_forearm", "hand_far"):
        masks[name] = restrict_x(masks[name], maximum=474 if name != "hand_far" else 390)
    for name in ("arm_near_complete", "arm_near_upper", "arm_near_forearm", "hand_near"):
        minimum = 620 if name == "arm_near_forearm" else (700 if name == "hand_near" else 605)
        masks[name] = restrict_x(masks[name], minimum=minimum)
    rgb_for_arms = np.asarray(src.convert("RGB"), dtype=np.uint8)
    gold_pixel = (rgb_for_arms[:, :, 0] > 170) & (rgb_for_arms[:, :, 1] > 105) & (rgb_for_arms[:, :, 2] < 115)
    for name in (
        "arm_far_complete", "arm_far_upper", "arm_far_forearm", "hand_far",
        "arm_near_complete", "arm_near_upper", "arm_near_forearm", "hand_near",
    ):
        a = np.asarray(masks[name], dtype=np.uint8)
        masks[name] = Image.fromarray(np.where(gold_pixel, 0, a).astype(np.uint8), "L")
    masks["torso_blouse"] = subtract(masks["torso_blouse"], arm_cover)
    masks["skirt_with_waistband"] = subtract(masks["skirt_with_waistband"], arm_cover)
    rgb = np.asarray(src.convert("RGB"), dtype=np.uint8)
    red, green, blue = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    garment_pixel = ((red < 150) & (green < 165) & (blue < 180)) | ((red > 170) & (green > 105) & (blue < 115))
    skirt_alpha = np.asarray(masks["skirt_with_waistband"], dtype=np.uint8)
    masks["skirt_with_waistband"] = Image.fromarray(np.where(garment_pixel, skirt_alpha, 0).astype(np.uint8), "L")
    for name in (
        "leg_far_complete", "leg_far_thigh", "leg_far_lower", "foot_far",
        "leg_near_complete", "leg_near_thigh", "leg_near_lower", "foot_near",
    ):
        masks[name] = subtract(masks[name], skirt_cover)
    for name in ("leg_far_complete", "leg_far_thigh", "leg_far_lower", "foot_far"):
        masks[name] = restrict_x(masks[name], maximum=514)
    for name in ("leg_near_complete", "leg_near_thigh", "leg_near_lower", "foot_near"):
        masks[name] = restrict_x(masks[name], minimum=500)
    masks["leg_near_complete"] = keep_largest_component(masks["leg_near_complete"])
    masks["leg_near_thigh"] = keep_largest_component(masks["leg_near_thigh"])

    # Utility layers for draw-order checks and future mesh work.
    masks["body_without_head"] = union(
        masks["torso_blouse"], masks["skirt_with_waistband"],
        masks["arm_far_complete"], masks["arm_near_complete"],
        masks["leg_far_complete"], masks["leg_near_complete"],
    )

    manifest = {
        "source": str(SOURCE),
        "canvas": {"width": src.width, "height": src.height},
        "orientation": "approved 42-degree three-quarter view",
        "joint_overlap_policy": "Segmented limbs overlap around elbow, wrist, knee and ankle to prevent rotation gaps.",
        "weapon": {
            "status": "reuse existing independent sword attachment",
            "path": "images/weapon/sword.png",
        },
        "recommended_draw_order_back_to_front": [
            "arm_far", "leg_far", "head_back_hair", "torso_blouse",
            "leg_near", "skirt_with_waistband", "arm_near",
            "head_face_front", "head_ornament_far", "head_ornament_near", "sword",
        ],
        "layers": {},
    }
    for name, alpha in masks.items():
        manifest["layers"][name] = save_layer(src, alpha, name)

    # Exact current-pose composite, using whole-limb layers (not segmented
    # duplicates), plus the already approved four head layers.
    preview = Image.new("RGBA", size, (0, 0, 0, 0))
    order = [
        "arm_far_complete", "leg_far_complete", "torso_blouse",
        "leg_near_complete", "skirt_with_waistband", "arm_near_complete",
    ]
    head_order = ["head_back_hair", "head_face_front", "head_ornament_far", "head_ornament_near"]
    for name in order:
        preview.alpha_composite(Image.open(ALIGNED / f"{name}.png").convert("RGBA"))
    for name in head_order:
        preview.alpha_composite(Image.open(HEAD_DIR / f"{name}.png").convert("RGBA"))
    preview.save(OUT / "side_character_rebuilt.png")

    # Contact sheet for one-pass review.
    names = [name for name in masks if name != "body_without_head"]
    cols, tile_w, tile_h = 5, 260, 360
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * tile_w, rows * (tile_h + 34)), (42, 42, 42, 255))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for i, name in enumerate(names):
        item = Image.open(CROPPED / f"{name}.png").convert("RGBA")
        item.thumbnail((tile_w - 18, tile_h - 18), Image.Resampling.LANCZOS)
        x = (i % cols) * tile_w + (tile_w - item.width) // 2
        y = (i // cols) * (tile_h + 34) + (tile_h - item.height) // 2
        sheet.alpha_composite(item, (x, y))
        draw.text(((i % cols) * tile_w + 8, (i // cols) * (tile_h + 34) + tile_h + 8), name, fill="white", font=font)
    sheet.convert("RGB").save(OUT / "split_contact_sheet.jpg", quality=94)
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    expected_x = {
        "arm_far_complete": (None, 474), "arm_far_upper": (None, 474),
        "arm_far_forearm": (None, 474), "hand_far": (None, 404),
        "arm_near_complete": (555, None), "arm_near_upper": (555, None),
        "arm_near_forearm": (555, None), "hand_near": (680, None),
        "leg_far_complete": (None, 514), "leg_far_thigh": (None, 514),
        "leg_far_lower": (None, 514), "foot_far": (None, 514),
        "leg_near_complete": (500, None), "leg_near_thigh": (500, None),
        "leg_near_lower": (500, None), "foot_near": (500, None),
    }
    qa = {"layers": {}, "result": "pass"}
    for name, alpha in masks.items():
        arr = np.asarray(alpha, dtype=np.uint8)
        binary = (arr >= 16).astype(np.uint8)
        count, _, stats, _ = cv2.connectedComponentsWithStats(binary, connectivity=8)
        significant = int(sum(1 for i in range(1, count) if stats[i, cv2.CC_STAT_AREA] >= 20))
        ys, xs = np.where(binary > 0)
        entry = {
            "opaque_or_edge_pixels": int(binary.sum()),
            "significant_components": significant,
            "bbox": [int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)] if len(xs) else None,
            "x_range_check": "not_applicable",
        }
        if name in expected_x:
            minimum, maximum = expected_x[name]
            bad = ((xs < minimum).sum() if minimum is not None else 0) + ((xs > maximum).sum() if maximum is not None else 0)
            entry["x_range_check"] = "pass" if bad == 0 else f"fail:{int(bad)}"
            if bad:
                qa["result"] = "fail"
        qa["layers"][name] = entry
    (OUT / "qa_report.json").write_text(json.dumps(qa, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(OUT), "layer_count": len(masks)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
