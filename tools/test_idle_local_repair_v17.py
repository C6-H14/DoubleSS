from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SOURCE = BASE / "short_blink_final_v15_garment_color_stable/alpha_frames"
RAW = BASE / "loop_v2_short_blink_180f/raw_frames"
OUT = BASE / "test_local_repair_v17"
PICKS = [16, 57, 120, 180]

# Conservative local areas. No whole-character morphology is performed.
ROIS = {
    "hair_face": (300, 55, 805, 385),
    "neck_collar": (430, 160, 700, 365),
    "hands_guard": (380, 320, 700, 585),
    "blade": (480, 385, 955, 755),
    "legs_shoes": (290, 455, 880, 850),
}


def pink_background_mask(rgb):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    # Video key is vivid pink/magenta. Include compressed darker edge spill.
    return (h >= 142) & (h <= 179) & (s >= 52) & (v >= 55)


def recover_supported_pixels(raw_rgb, rgba, roi, radius=5):
    x1, y1, x2, y2 = roi
    a = rgba[:, :, 3].astype(np.float32)
    existing = a >= 112
    support = cv2.dilate(existing.astype(np.uint8),
                         cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (radius * 2 + 1,) * 2)) > 0
    bg = pink_background_mask(raw_rgb)
    raw_hsv = cv2.cvtColor(raw_rgb, cv2.COLOR_RGB2HSV)
    raw_subject = (~bg) & (raw_hsv[:, :, 2] >= 28)
    area = np.zeros(a.shape, bool)
    area[y1:y2, x1:x2] = True
    candidate = area & support & raw_subject & (a < 220)

    # Require local raw-color support, rejecting isolated compression specks.
    coherent = cv2.morphologyEx(candidate.astype(np.uint8), cv2.MORPH_OPEN,
                                cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))) > 0
    use = candidate & cv2.dilate(coherent.astype(np.uint8), np.ones((3, 3), np.uint8)).astype(bool)
    # Distance from key family gives a stable soft edge.
    hsv = raw_hsv.astype(np.float32)
    hue_delta = np.minimum(np.abs(hsv[:, :, 0] - 160), 180 - np.abs(hsv[:, :, 0] - 160))
    confidence = np.clip((hue_delta - 5) / 11, 0, 1) * np.clip((hsv[:, :, 2] - 24) / 45, 0, 1)
    new_a = np.maximum(a, confidence * 255)
    changed = use & (new_a > a + 3)
    rgba[changed, :3] = raw_rgb[changed]
    a[changed] = new_a[changed]
    rgba[:, :, 3] = np.rint(np.clip(a, 0, 255)).astype(np.uint8)
    return changed


def clean_low_alpha_fragments(rgba, roi):
    x1, y1, x2, y2 = roi
    a = rgba[:, :, 3]
    crop = a[y1:y2, x1:x2]
    # Only remove tiny components that never reach a robust opaque core.
    weak = ((crop > 0) & (crop < 112)).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(weak, 8)
    removed = np.zeros_like(crop, bool)
    for label in range(1, n):
        size = stats[label, cv2.CC_STAT_AREA]
        component = labels == label
        if size <= 5:
            removed |= component
    target = rgba[y1:y2, x1:x2]
    target[removed, :3] = 0
    target[removed, 3] = 0
    return int(removed.sum())


def repair_blade(raw_rgb, rgba):
    x1, y1, x2, y2 = ROIS["blade"]
    rgb = rgba[y1:y2, x1:x2, :3]
    a = rgba[y1:y2, x1:x2, 3]
    raw = raw_rgb[y1:y2, x1:x2]
    hsv = cv2.cvtColor(raw, cv2.COLOR_RGB2HSV)
    # Neutral/lilac bright steel only; skin, teal skirt, and gold trim excluded.
    neutral = (hsv[:, :, 2] >= 105) & (hsv[:, :, 1] <= 105)
    current = (a >= 45) & neutral
    # Keep only elongated components, then close single-pixel chips along them.
    n, labels, stats, _ = cv2.connectedComponentsWithStats(current.astype(np.uint8), 8)
    steel = np.zeros_like(current)
    for label in range(1, n):
        xx, yy, ww, hh, area = stats[label]
        if area >= 80 and max(ww, hh) >= 55:
            steel |= labels == label
    closed = cv2.morphologyEx(steel.astype(np.uint8), cv2.MORPH_CLOSE,
                              cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3)), iterations=1)
    # Intersect dilation with raw steel evidence, so the blade cannot grow into legs.
    raw_steel = neutral & (~pink_background_mask(raw))
    recovered = (closed.astype(bool) | (cv2.dilate(steel.astype(np.uint8), np.ones((3, 3), np.uint8)).astype(bool) & raw_steel))
    add = recovered & (a < 170)
    rgb[add] = raw[add]
    a[add] = np.maximum(a[add], 230)

    # A light subpixel edge, computed only for the steel mask.
    soft = cv2.GaussianBlur((recovered.astype(np.uint8) * 255), (0, 0), 0.55)
    edge = (soft > 8) & (soft < 247) & raw_steel
    stronger = edge & (soft > a)
    rgb[stronger] = raw[stronger]
    a[stronger] = soft[stronger]
    return int(add.sum()), int(stronger.sum())


def composite(rgba, color):
    return Image.alpha_composite(Image.new("RGBA", (rgba.shape[1], rgba.shape[0]), color),
                                 Image.fromarray(rgba, "RGBA"))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    report = {}
    rows = []
    for frame in PICKS:
        src_path = SOURCE / f"idle_{frame:03d}.png"
        raw_path = RAW / f"idle_{frame:03d}.png"
        before = np.asarray(Image.open(src_path).convert("RGBA"), np.uint8).copy()
        raw = np.asarray(Image.open(raw_path).convert("RGB"), np.uint8)
        after = before.copy()
        restored_all = np.zeros(before.shape[:2], bool)
        recovered = {}
        for name in ("hair_face", "neck_collar", "hands_guard", "legs_shoes"):
            changed = recover_supported_pixels(raw, after, ROIS[name])
            restored_all |= changed
            recovered[name] = int(changed.sum())
        blade_add, blade_edge = repair_blade(raw, after)
        # Newly recovered outer pixels use the nearest trustworthy color from
        # the already-clean transparent frame. Raw footage decides occupancy,
        # never edge color, preventing its pink antialias fringe from returning.
        donor = before[:, :, 3] >= 180
        _, nearest = distance_transform_edt(~donor, return_indices=True)
        nearest_rgb = before[nearest[0], nearest[1], :3]
        outer_restore = restored_all & (before[:, :, 3] < 96)
        after[outer_restore, :3] = nearest_rgb[outer_restore]
        removed = {name: clean_low_alpha_fragments(after, ROIS[name]) for name in ROIS}
        after[after[:, :, 3] == 0, :3] = 0
        Image.fromarray(after, "RGBA").save(OUT / f"idle_{frame:03d}_test.png", optimize=True)
        report[str(frame)] = {"recovered": recovered, "blade_core_added": blade_add,
                              "blade_edge_added": blade_edge, "tiny_fragments_removed": removed}
        rows.append((frame, before, after))

    # Side-by-side, dark and light backgrounds, with local crops enlarged.
    bg_specs = [("dark", (36, 25, 25, 255)), ("light", (225, 225, 225, 255))]
    regions = ["hair_face", "neck_collar", "hands_guard", "blade", "legs_shoes"]
    thumb_w, thumb_h = 310, 220
    sheet = Image.new("RGB", (thumb_w * 4, thumb_h * len(rows) * len(bg_specs)), (30, 30, 30))
    draw = ImageDraw.Draw(sheet)
    y = 0
    for frame, before, after in rows:
        for bg_name, color in bg_specs:
            b = composite(before, color).resize((thumb_w * 2, thumb_h), Image.Resampling.LANCZOS).convert("RGB")
            a = composite(after, color).resize((thumb_w * 2, thumb_h), Image.Resampling.LANCZOS).convert("RGB")
            sheet.paste(b, (0, y)); sheet.paste(a, (thumb_w * 2, y))
            draw.text((5, y + 4), f"frame {frame:03d} {bg_name} BEFORE", fill="white")
            draw.text((thumb_w * 2 + 5, y + 4), f"frame {frame:03d} {bg_name} TEST", fill="white")
            y += thumb_h
    sheet.save(OUT / "full_before_after_dark_light.png", optimize=True)

    for name in regions:
        x1, y1, x2, y2 = ROIS[name]
        cw, ch = x2 - x1, y2 - y1
        scale = 2
        local = Image.new("RGB", (cw * scale * 2, ch * scale * len(rows)), (30, 30, 30))
        d = ImageDraw.Draw(local)
        yy = 0
        for frame, before, after in rows:
            b = composite(before, (38, 38, 38, 255)).crop((x1, y1, x2, y2)).resize((cw*scale, ch*scale), Image.Resampling.NEAREST)
            a = composite(after, (38, 38, 38, 255)).crop((x1, y1, x2, y2)).resize((cw*scale, ch*scale), Image.Resampling.NEAREST)
            local.paste(b, (0, yy)); local.paste(a, (cw*scale, yy))
            d.text((4, yy + 3), f"{frame:03d} before", fill="white")
            d.text((cw*scale + 4, yy + 3), f"{frame:03d} test", fill="white")
            yy += ch*scale
        local.save(OUT / f"{name}_before_after.png", optimize=True)

    (OUT / "test_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
