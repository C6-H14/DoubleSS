from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SRC = BASE / "short_blink_final_v15_garment_color_stable/alpha_frames"
RAW = BASE / "loop_v2_short_blink_180f/raw_frames"
OUT = BASE / "test_multi_region_rgb_restore_v20"
PICKS = [1, 16, 41, 57, 81, 101, 120, 151, 180]
ROIS = {
    "hair_face": (285, 45, 820, 390),
    "torso_collar": (365, 145, 745, 500),
    "hands_guard": (365, 305, 720, 590),
    "skirt_hips": (330, 330, 775, 625),
    "blade": (455, 370, 965, 760),
    "legs_socks_shoes": (260, 455, 900, 840),
}


def pink(rgb):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    return ((hsv[:, :, 0] >= 140) & (hsv[:, :, 0] <= 179)
            & (hsv[:, :, 1] >= 45) & (hsv[:, :, 2] >= 45))


def composite(a, color=(45, 45, 45, 255)):
    return Image.alpha_composite(Image.new("RGBA", (a.shape[1], a.shape[0]), color),
                                 Image.fromarray(a, "RGBA"))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    records = []
    report = {"contract": "solid RGB only; alpha and semitransparent edge frozen", "frames": {}}
    for frame in PICKS:
        before = np.asarray(Image.open(SRC / f"idle_{frame:03d}.png").convert("RGBA"), np.uint8).copy()
        raw = np.asarray(Image.open(RAW / f"idle_{frame:03d}.png").convert("RGB"), np.uint8)
        after = before.copy()
        valid = (before[:, :, 3] >= 224) & (~pink(raw))
        lab_b = cv2.cvtColor(before[:, :, :3], cv2.COLOR_RGB2LAB).astype(np.float32)
        lab_r = cv2.cvtColor(raw, cv2.COLOR_RGB2LAB).astype(np.float32)
        delta = np.linalg.norm(lab_b - lab_r, axis=2)
        # Restore complete solid interiors. The changed-mask below records only
        # perceptually meaningful changes, not byte-level JPEG/video noise.
        after[valid, :3] = raw[valid]
        meaningful = valid & (delta >= 10)
        after[before[:, :, 3] == 0, :3] = 0
        assert np.array_equal(after[:, :, 3], before[:, :, 3])
        Image.fromarray(after, "RGBA").save(OUT / f"idle_{frame:03d}_multi_rgb_test.png", optimize=True)

        by_region = {}
        for name, (x1, y1, x2, y2) in ROIS.items():
            x2, y2 = min(x2, before.shape[1]), min(y2, before.shape[0])
            m = meaningful[y1:y2, x1:x2]
            by_region[name] = int(m.sum())
        report["frames"][str(frame)] = {
            "solid_pixels_restored": int(valid.sum()),
            "meaningful_rgb_changes_deltaE10": int(meaningful.sum()),
            "alpha_pixels_changed": 0,
            "meaningful_by_region": by_region,
        }
        records.append((frame, before, after, meaningful))

    # Region sheets, Lanczos at 1.5x: before / restored / meaningful changes.
    for name, box0 in ROIS.items():
        x1, y1, x2, y2 = box0
        h, w = records[0][1].shape[:2]
        x2, y2 = min(x2, w), min(y2, h)
        box = (x1, y1, x2, y2)
        cw, ch = x2-x1, y2-y1
        scale = 2
        sheet = Image.new("RGB", (cw*scale*3, ch*scale*len(records)), (28, 28, 28))
        d = ImageDraw.Draw(sheet)
        yy = 0
        for frame, before, after, meaningful in records:
            b = composite(before).crop(box).resize((cw*scale, ch*scale), Image.Resampling.LANCZOS)
            a = composite(after).crop(box).resize((cw*scale, ch*scale), Image.Resampling.LANCZOS)
            m = np.zeros((ch, cw, 3), np.uint8)
            m[meaningful[y1:y2, x1:x2]] = (255, 45, 45)
            mi = Image.fromarray(m).resize((cw*scale, ch*scale), Image.Resampling.NEAREST)
            sheet.paste(b, (0, yy)); sheet.paste(a, (cw*scale, yy)); sheet.paste(mi, (cw*scale*2, yy))
            d.text((5, yy+4), f"{frame:03d} V15", fill="white")
            d.text((cw*scale+5, yy+4), f"{frame:03d} RAW RGB", fill="white")
            d.text((cw*scale*2+5, yy+4), "DELTA E >= 10", fill="white")
            yy += ch*scale
        sheet.save(OUT / f"{name}_rgb_before_after.png", optimize=True)
        preview = sheet.copy()
        preview.thumbnail((1600, 2000), Image.Resampling.LANCZOS)
        preview.save(OUT / f"{name}_rgb_preview.png", optimize=True)

    (OUT / "multi_region_rgb_report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    print(json.dumps({f: report['frames'][str(f)]['meaningful_by_region'] for f in PICKS}, indent=2))


if __name__ == "__main__":
    main()
