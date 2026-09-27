from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SOURCE = BASE / "short_blink_final_v15_garment_color_stable/alpha_frames"
RAW = BASE / "loop_v2_short_blink_180f/raw_frames"
OUT = BASE / "test_internal_only_v18"
PICKS = [16, 57, 120, 180]
ROIS = {
    "neck_collar": (470, 185, 690, 350),
    "hands_guard": (385, 325, 700, 585),
    "blade": (470, 380, 960, 760),
    "shoes": (285, 665, 885, 875),
}


def pink_mask(rgb):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    return (h >= 140) & (h <= 179) & (s >= 45) & (v >= 45)


def enclosed_holes(alpha, roi):
    """Transparent components enclosed by visible pixels inside a padded ROI."""
    x1, y1, x2, y2 = roi
    pad = 4
    x1p, y1p = max(0, x1-pad), max(0, y1-pad)
    x2p, y2p = min(alpha.shape[1], x2+pad), min(alpha.shape[0], y2+pad)
    crop = alpha[y1p:y2p, x1p:x2p]
    transparent = (crop < 72).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(transparent, 8)
    result = np.zeros(alpha.shape, bool)
    for label in range(1, n):
        xx, yy, ww, hh, area = stats[label]
        touches = xx == 0 or yy == 0 or xx + ww == crop.shape[1] or yy + hh == crop.shape[0]
        if touches or area > 180:
            continue
        component = labels == label
        full = np.zeros(alpha.shape, bool)
        full[y1p:y2p, x1p:x2p] = component
        # Only retain the requested unpadded area.
        area_mask = np.zeros(alpha.shape, bool)
        area_mask[y1:y2, x1:x2] = True
        result |= full & area_mask
    return result


def repair_internal(raw, before):
    after = before.copy()
    all_changed = np.zeros(before.shape[:2], bool)
    counts = {}
    bg = pink_mask(raw)
    for name, roi in ROIS.items():
        holes = enclosed_holes(after[:, :, 3], roi)
        # Raw frame acts as evidence. Pink pixels and dark compression noise do
        # not qualify as original subject material.
        hsv = cv2.cvtColor(raw, cv2.COLOR_RGB2HSV)
        evidence = (~bg) & (hsv[:, :, 2] >= 30)
        use = holes & evidence
        # Reject isolated raw specks; a valid recovered patch has neighbours.
        support = cv2.filter2D(use.astype(np.uint8), -1, np.ones((3, 3), np.uint8))
        use &= support >= 3
        after[use, :3] = raw[use]
        after[use, 3] = 255
        all_changed |= use
        counts[name] = int(use.sum())

    # Smooth only the boundary of newly filled internal holes. The external
    # silhouette is provably untouched because all_changed excludes it.
    boundary = all_changed & (cv2.erode(all_changed.astype(np.uint8), np.ones((3,3), np.uint8)) == 0)
    if boundary.any():
        alpha_f = after[:, :, 3].astype(np.float32)
        softened = cv2.GaussianBlur(alpha_f, (0, 0), 0.45)
        after[boundary, 3] = np.maximum(160, softened[boundary]).astype(np.uint8)
    after[after[:, :, 3] == 0, :3] = 0
    return after, counts, all_changed


def composite(a, color=(38, 38, 38, 255)):
    return Image.alpha_composite(Image.new("RGBA", (a.shape[1], a.shape[0]), color), Image.fromarray(a, "RGBA"))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    report = {}
    records = []
    for frame in PICKS:
        before = np.asarray(Image.open(SOURCE / f"idle_{frame:03d}.png").convert("RGBA"), np.uint8).copy()
        raw = np.asarray(Image.open(RAW / f"idle_{frame:03d}.png").convert("RGB"), np.uint8)
        after, counts, changed = repair_internal(raw, before)
        # Contract check: no background-connected transparent pixel from the
        # original may become visible. Enclosed holes are allowed at any depth.
        transparent = (before[:, :, 3] < 72).astype(np.uint8)
        flood = transparent.copy()
        flood_mask = np.zeros((flood.shape[0]+2, flood.shape[1]+2), np.uint8)
        cv2.floodFill(flood, flood_mask, (0, 0), 2)
        external_background = transparent.astype(bool) & (flood == 2)
        outward = changed & external_background
        if outward.any():
            raise RuntimeError(f"frame {frame}: external silhouette changed")
        Image.fromarray(after, "RGBA").save(OUT / f"idle_{frame:03d}_internal_test.png", optimize=True)
        report[str(frame)] = {"filled": counts, "total": int(changed.sum()), "external_pixels_changed": 0}
        records.append((frame, before, after, changed))

    # Detailed ROI contact sheet: before / repaired / changed pixels.
    panels = []
    for name, box in ROIS.items():
        x1, y1, x2, y2 = box
        sample_h, sample_w = records[0][1].shape[:2]
        x2, y2 = min(x2, sample_w), min(y2, sample_h)
        box = (x1, y1, x2, y2)
        cw, ch = x2-x1, y2-y1
        scale = 2
        panel = Image.new("RGB", (cw*scale*3, ch*scale*len(records)), (30,30,30))
        d = ImageDraw.Draw(panel)
        yy = 0
        for frame, before, after, changed in records:
            b = composite(before).crop(box).resize((cw*scale,ch*scale), Image.Resampling.NEAREST)
            a = composite(after).crop(box).resize((cw*scale,ch*scale), Image.Resampling.NEAREST)
            mask = np.zeros((ch,cw,3), np.uint8)
            local = changed[y1:y2,x1:x2]
            mask[local] = (255,40,40)
            m = Image.fromarray(mask).resize((cw*scale,ch*scale), Image.Resampling.NEAREST)
            panel.paste(b,(0,yy)); panel.paste(a,(cw*scale,yy)); panel.paste(m,(cw*scale*2,yy))
            d.text((4,yy+3),f"{frame:03d} BEFORE",fill="white")
            d.text((cw*scale+4,yy+3),f"{frame:03d} INTERNAL",fill="white")
            d.text((cw*scale*2+4,yy+3),f"{frame:03d} CHANGED",fill="white")
            yy += ch*scale
        path = OUT / f"{name}_internal_before_after.png"
        panel.save(path,optimize=True)
        panels.append(str(path))

    (OUT / "internal_test_report.json").write_text(json.dumps(report, indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
