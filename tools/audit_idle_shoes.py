from pathlib import Path
import csv

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SETS = {
    "v41": ROOT / "art_candidates/video_source/idle/short_blink_final_v41_gap_repaired_pixel_evidence/alpha_frames",
    "v44": ROOT / "art_candidates/video_source/idle/short_blink_final_v44_semantic_color_stable/alpha_frames",
    "v46": ROOT / "art_candidates/video_source/idle/short_blink_final_v46_loop_tail_rebuilt/alpha_frames",
}
OUT = ROOT / "art_candidates/video_source/idle/idle_shoe_audit"


def shoe_metrics(path):
    rgba = np.asarray(Image.open(path).convert("RGBA"), np.uint8)
    rgb, alpha = rgba[:, :, :3], rgba[:, :, 3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    # The two shoe ROIs are deliberately generous and exclude the sword.
    yy, xx = np.indices(alpha.shape)
    left_roi = (xx >= 330) & (xx < 475) & (yy >= 675) & (yy < 805)
    right_roi = (xx >= 680) & (xx < 835) & (yy >= 675) & (yy < 805)
    brown = (hsv[:, :, 0] <= 18) & (hsv[:, :, 1] >= 55) & (hsv[:, :, 2] <= 180)
    visible = alpha >= 32
    return tuple(int(np.sum(roi & visible & brown)) for roi in (left_roi, right_roi))


def composite(path):
    image = Image.open(path).convert("RGBA")
    return Image.alpha_composite(Image.new("RGBA", image.size, (45, 45, 45, 255)), image)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    rows = []
    for tag, folder in SETS.items():
        for frame in range(1, 181):
            left, right = shoe_metrics(folder / f"idle_{frame:03d}.png")
            rows.append((tag, frame, left, right, left + right))
    with (OUT / "shoe_metrics.csv").open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.writer(stream)
        writer.writerow(("set", "frame", "left_brown", "right_brown", "total_brown"))
        writer.writerows(rows)

    current = [row for row in rows if row[0] == "v46"]
    ranked = sorted(current, key=lambda row: row[4])[:24]
    sheet = Image.new("RGB", (6 * 300, 4 * 190), (25, 25, 25))
    draw = ImageDraw.Draw(sheet)
    for slot, (_, frame, left, right, total) in enumerate(ranked):
        source = composite(SETS["v46"] / f"idle_{frame:03d}.png")
        crop = source.crop((300, 640, 860, 820)).resize((300, 96), Image.Resampling.LANCZOS)
        x, y = (slot % 6) * 300, (slot // 6) * 190
        sheet.paste(crop.convert("RGB"), (x, y + 24))
        draw.text((x + 4, y + 4), f"F{frame:03d} L{left} R{right} T{total}", fill="white")
        # Compare against the pre-tail-rebuild frame at the same number.
        old = composite(SETS["v44"] / f"idle_{frame:03d}.png")
        old_crop = old.crop((300, 640, 860, 820)).resize((300, 66), Image.Resampling.LANCZOS)
        sheet.paste(old_crop.convert("RGB"), (x, y + 120))
    sheet.save(OUT / "lowest_shoe_frames_v46_vs_v44.png", optimize=True)

    # Every current frame, enlarged with nearest-neighbour sampling so missing
    # pixels and temporal outline changes remain visible.
    all_sheet = Image.new("RGB", (15 * 224, 12 * 116), (25, 25, 25))
    all_draw = ImageDraw.Draw(all_sheet)
    for index in range(180):
        frame = index + 1
        source = composite(SETS["v46"] / f"idle_{frame:03d}.png")
        crop = source.crop((320, 680, 840, 800)).resize((224, 104), Image.Resampling.NEAREST)
        x, y = (index % 15) * 224, (index // 15) * 116
        all_sheet.paste(crop.convert("RGB"), (x, y + 12))
        all_draw.text((x + 2, y), f"{frame:03d}", fill="white")
    all_sheet.save(OUT / "all_180_shoe_frames_nearest.png", optimize=True)

    for tag in SETS:
        values = [row for row in rows if row[0] == tag]
        print(tag, "left", min(r[2] for r in values), max(r[2] for r in values),
              "right", min(r[3] for r in values), max(r[3] for r in values),
              "lowest", [(r[1], r[4]) for r in sorted(values, key=lambda r: r[4])[:12]])


if __name__ == "__main__":
    main()
