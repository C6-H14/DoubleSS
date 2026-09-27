from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SRC = BASE / "short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
RAW = BASE / "loop_v2_short_blink_180f/raw_frames/idle_073.png"
OUT = BASE / "test_hand_narrow_raw_restore_v28"
VIEW = (455, 360, 570, 450)


def composite_dark(a):
    bg = Image.new("RGBA", (a.shape[1], a.shape[0]), (45, 45, 45, 255))
    return Image.alpha_composite(bg, Image.fromarray(a, "RGBA"))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    rgba = np.asarray(Image.open(SRC).convert("RGBA"), np.uint8).copy()
    raw = np.asarray(Image.open(RAW).convert("RGB"), np.uint8)
    alpha = rgba[:, :, 3].copy()

    # Three disconnected 3 px ribbons. They follow only the damaged seams;
    # normal sleeve/skin interiors between those seams are intentionally skipped.
    paths = [
        np.array([[484, 389], [488, 395], [490, 402], [492, 409]], np.int32),
        np.array([[503, 395], [511, 394], [519, 397], [527, 401]], np.int32),
        np.array([[539, 408], [543, 414], [547, 420], [551, 426]], np.int32),
    ]
    mask_u8 = np.zeros(alpha.shape, np.uint8)
    for p in paths:
        cv2.polylines(mask_u8, [p], False, 255, 3, cv2.LINE_AA)
    mask = (mask_u8 >= 96) & (alpha >= 150)

    after = rgba.copy()
    after[mask, :3] = raw[mask]
    assert np.array_equal(after[:, :, 3], alpha)
    Image.fromarray(after, "RGBA").save(OUT / "idle_073_narrow_raw_restore.png", optimize=True)

    x1, y1, x2, y2 = VIEW
    s = 10
    before = composite_dark(rgba).crop(VIEW).resize(((x2-x1)*s, (y2-y1)*s), Image.Resampling.NEAREST)
    result = composite_dark(after).crop(VIEW).resize(((x2-x1)*s, (y2-y1)*s), Image.Resampling.NEAREST)
    overlay = np.asarray(composite_dark(rgba).crop(VIEW).convert("RGB")).copy()
    local = mask[y1:y2, x1:x2]
    overlay[local] = np.rint(0.25 * overlay[local] + 0.75 * np.array([20, 200, 255])).astype(np.uint8)
    overlay = Image.fromarray(overlay).resize(((x2-x1)*s, (y2-y1)*s), Image.Resampling.NEAREST)

    sheet = Image.new("RGB", ((x2-x1)*s*3, (y2-y1)*s), (28, 28, 28))
    sheet.paste(before, (0, 0)); sheet.paste(overlay, ((x2-x1)*s, 0)); sheet.paste(result, ((x2-x1)*s*2, 0))
    d = ImageDraw.Draw(sheet)
    d.text((8, 8), "V21 BEFORE", fill="white")
    d.text(((x2-x1)*s+8, 8), "CYAN = 3 DISCONNECTED NARROW BANDS", fill="white")
    d.text(((x2-x1)*s*2+8, 8), "V28 RAW RGB RESTORE; ALPHA LOCKED", fill="white")
    sheet.save(OUT / "frame073_before_mask_after_10x.png", optimize=True)
    preview = sheet.copy(); preview.thumbnail((2100, 900), Image.Resampling.NEAREST)
    preview.save(OUT / "frame073_preview.png", optimize=True)

    changed = np.any(after[:, :, :3] != rgba[:, :, :3], axis=2)
    report = {
        "frame": 73,
        "mask_pixels": int(mask.sum()),
        "rgb_pixels_changed": int(changed.sum()),
        "changed_outside_mask": int((changed & ~mask).sum()),
        "alpha_pixels_changed": int(np.count_nonzero(after[:, :, 3] != alpha)),
        "method": "copy aligned raw-frame RGB through three disconnected 3px ribbons; preserve V21 alpha",
        "paths": [p.tolist() for p in paths],
    }
    (OUT / "v28_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
