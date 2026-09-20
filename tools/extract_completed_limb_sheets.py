from __future__ import annotations

import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\side_body_split_v1")
SHEETS = ROOT / "ai_sheets"
REFERENCE = ROOT / "cropped"
OUTPUT = ROOT / "completed"

SPECS = {
    "arm_far_sheet.png": ["arm_far_upper", "arm_far_forearm", "hand_far"],
    "arm_near_sheet.png": ["arm_near_upper", "arm_near_forearm", "hand_near"],
    "leg_far_sheet.png": ["leg_far_thigh", "leg_far_lower", "foot_far"],
    "leg_near_sheet.png": ["leg_near_thigh", "leg_near_lower", "foot_near"],
}


def extract(sheet_path: Path) -> list[Image.Image]:
    image = Image.open(sheet_path).convert("RGBA")
    rgba = np.asarray(image, dtype=np.uint8)
    alpha = rgba[:, :, 3]
    count, labels, stats, centroids = cv2.connectedComponentsWithStats((alpha >= 32).astype(np.uint8), 8)
    ids = [i for i in range(1, count) if stats[i, cv2.CC_STAT_AREA] >= 1800]
    ids.sort(key=lambda i: centroids[i][0])
    if len(ids) != 3:
        raise RuntimeError(f"{sheet_path.name}: expected 3 parts, found {len(ids)}")
    parts = []
    for component in ids:
        x, y, w, h, _ = stats[component]
        pad = 8
        left, top = max(0, x - pad), max(0, y - pad)
        right, bottom = min(image.width, x + w + pad), min(image.height, y + h + pad)
        component_alpha = np.where(labels == component, alpha, 0).astype(np.uint8)
        clean = rgba.copy()
        clean[:, :, 3] = component_alpha
        parts.append(Image.fromarray(clean, "RGBA").crop((left, top, right, bottom)))
    return parts


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    manifest = {"source_sheets": {}, "parts": {}}
    for sheet_name, names in SPECS.items():
        parts = extract(SHEETS / sheet_name)
        manifest["source_sheets"][sheet_name] = names
        for name, part in zip(names, parts):
            reference = Image.open(REFERENCE / f"{name}.png").convert("RGBA")
            target_h = reference.height
            scale = target_h / part.height
            target_w = max(1, round(part.width * scale))
            normalized = part.resize((target_w, target_h), Image.Resampling.LANCZOS)
            out_path = OUTPUT / f"{name}.png"
            normalized.save(out_path)
            manifest["parts"][name] = {
                "path": str(out_path),
                "width": target_w,
                "height": target_h,
                "source_sheet": sheet_name,
                "role": "AI-completed hidden joint geometry; visible design follows approved reference",
            }
    names = list(manifest["parts"])
    tile_w, tile_h, cols = 300, 390, 3
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * tile_w, rows * (tile_h + 34)), (40, 40, 40, 255))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for i, name in enumerate(names):
        item = Image.open(OUTPUT / f"{name}.png").convert("RGBA")
        item.thumbnail((tile_w - 20, tile_h - 20), Image.Resampling.LANCZOS)
        x = (i % cols) * tile_w + (tile_w - item.width) // 2
        y = (i // cols) * (tile_h + 34) + (tile_h - item.height) // 2
        sheet.alpha_composite(item, (x, y))
        draw.text(((i % cols) * tile_w + 8, (i // cols) * (tile_h + 34) + tile_h + 8), name, fill="white", font=font)
    sheet.convert("RGB").save(OUTPUT / "completed_parts_contact_sheet.jpg", quality=94)
    qa = {"result": "pass", "parts": {}}
    for name in names:
        item = Image.open(OUTPUT / f"{name}.png").convert("RGBA")
        alpha = np.asarray(item.getchannel("A"), dtype=np.uint8)
        count, _, stats, _ = cv2.connectedComponentsWithStats((alpha >= 16).astype(np.uint8), 8)
        significant = int(sum(1 for i in range(1, count) if stats[i, cv2.CC_STAT_AREA] >= 20))
        corners_clear = all(alpha[y, x] == 0 for x, y in ((0, 0), (alpha.shape[1] - 1, 0), (0, alpha.shape[0] - 1), (alpha.shape[1] - 1, alpha.shape[0] - 1)))
        status = "pass" if significant == 1 and corners_clear else "fail"
        if status == "fail":
            qa["result"] = "fail"
        qa["parts"][name] = {"significant_components": significant, "transparent_corners": corners_clear, "status": status}
    (OUTPUT / "qa_report.json").write_text(json.dumps(qa, ensure_ascii=False, indent=2), encoding="utf-8")
    (OUTPUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(OUTPUT), "count": len(manifest["parts"])}, ensure_ascii=False))


if __name__ == "__main__":
    main()
