from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v7_outer_ring_removed" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v10_outline_preserved"
FRAMES = OUT / "alpha_frames"
COUNT = 180
BLEED_RADIUS = 4.0
BLINK_FIRST = 92
BLINK_LAST = 120


def deep_metrics(rgba):
    solid = rgba[:, :, 3] >= 245
    inward = cv2.distanceTransform(solid.astype(np.uint8), cv2.DIST_L2, 5)
    mask = inward >= 6.0
    lab = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2LAB).astype(np.float32)
    a = lab[:, :, 1] - 128.0
    b = lab[:, :, 2] - 128.0
    return float(lab[:, :, 0][mask].mean()), float(np.sqrt(a[mask] ** 2 + b[mask] ** 2).mean())


def normalize_color(rgba, target_l, target_chroma):
    alpha = rgba[:, :, 3]
    subject = alpha > 0
    current_l, current_chroma = deep_metrics(rgba)
    lab = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2LAB).astype(np.float32)
    lab[:, :, 0][subject] = np.clip(lab[:, :, 0][subject] + (target_l - current_l), 0, 255)
    scale = target_chroma / max(current_chroma, 1e-6)
    for channel in (1, 2):
        centered = lab[:, :, channel][subject] - 128.0
        lab[:, :, channel][subject] = np.clip(128.0 + centered * scale, 0, 255)
    rgba[:, :, :3] = cv2.cvtColor(lab.astype(np.uint8), cv2.COLOR_LAB2RGB)
    return current_l, current_chroma


def add_transparent_rgb_bleed(rgba):
    alpha = rgba[:, :, 3]
    subject = alpha > 0
    rgb = rgba[:, :, :3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    saturated = hsv[:, :, 1] > 180
    yellow = (rgb[:, :, 0] > 225) & (rgb[:, :, 1] > 185) & (rgb[:, :, 2] < 90)
    red_or_pink = (rgb[:, :, 0] > 205) & (rgb[:, :, 1] < 125) & (rgb[:, :, 2] < 155)
    suspicious = (alpha > 0) & (alpha < 160) & saturated & (yellow | red_or_pink)
    # Preserve legitimate black/brown outline pixels and ordinary antialiasing.
    # Only contaminated chroma-key colors are replaced, using the nearest
    # non-suspicious visible edge pixel rather than a deep interior fill.
    clean_visible = subject & (~suspicious) & (alpha >= 24)
    _, clean_nearest = distance_transform_edt(~clean_visible, return_indices=True)
    clean_rgb = rgba[clean_nearest[0], clean_nearest[1], :3]
    rgba[suspicious, :3] = clean_rgb[suspicious]

    # After cleanup, extrude the actual outline color into transparent texels.
    _, nearest = distance_transform_edt(~subject, return_indices=True)
    distance_to_subject = distance_transform_edt(~subject)
    bleed = (~subject) & (distance_to_subject <= BLEED_RADIUS)
    donor_rgb = rgba[nearest[0], nearest[1], :3]
    rgba[bleed, :3] = donor_rgb[bleed]
    # Pixels beyond the useful filter footprint stay transparent black.
    rgba[(~subject) & (~bleed), :3] = 0
    return int(np.count_nonzero(bleed)), int(np.count_nonzero(suspicious))


def composite(frame, color):
    return Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    if len(paths) != COUNT:
        raise RuntimeError(f"Expected {COUNT}, found {len(paths)}")
    arrays = [np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy() for path in paths]
    before = [deep_metrics(a) for a in arrays]
    l_start, c_start = before[BLINK_FIRST - 2]  # frame 91
    l_end, c_end = before[BLINK_LAST - 1]       # frame 120

    rows = []
    frames = []
    for index, rgba in enumerate(arrays, 1):
        original_l, original_c = before[index - 1]
        target_l, target_c = original_l, original_c
        if BLINK_FIRST <= index <= BLINK_LAST:
            t = (index - (BLINK_FIRST - 1)) / (BLINK_LAST - (BLINK_FIRST - 1))
            target_l = l_start * (1.0 - t) + l_end * t
            target_c = c_start * (1.0 - t) + c_end * t
            normalize_color(rgba, target_l, target_c)
        bleed_count, fringe_count = add_transparent_rgb_bleed(rgba)
        final_l, final_c = deep_metrics(rgba)
        image = Image.fromarray(rgba, "RGBA")
        image.save(FRAMES / f"idle_{index:03d}.png", optimize=True)
        frames.append(image)
        rows.append({"frame": index, "l_before": original_l, "l_target": target_l,
                     "l_after": final_l, "chroma_before": original_c,
                     "chroma_target": target_c, "chroma_after": final_c,
                     "transparent_rgb_bleed_pixels": bleed_count,
                     "low_alpha_fringe_recolored": fringe_count})

    for name, color in (("game_dark", (36, 25, 25, 255)), ("black", (0, 0, 0, 255)),
                        ("gray", (45, 45, 45, 255)), ("green", (0, 255, 0, 255))):
        views = [composite(f, color).resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for f in frames]
        views[0].save(OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)

    picks = [0, 60, 90, 101, 109, 119, 149, 179]
    sheet = Image.new("RGB", (1066, len(picks) * 240), (28, 28, 30))
    draw = ImageDraw.Draw(sheet)
    for row, p in enumerate(picks):
        old = composite(Image.open(paths[p]).convert("RGBA"), (36, 25, 25, 255)).resize((533, 400), Image.Resampling.LANCZOS)
        new = composite(frames[p], (36, 25, 25, 255)).resize((533, 400), Image.Resampling.LANCZOS)
        y = row * 240
        sheet.paste(old.crop((0, 80, 533, 300)), (0, y + 20))
        sheet.paste(new.crop((0, 80, 533, 300)), (533, y + 20))
        draw.text((8, y + 3), f"frame {p+1:03d}: V7 left / V8 right", fill="white")
    sheet.save(OUT / "idle_v7_v8_game_dark_contact.png", optimize=True)
    report = {"frames": COUNT, "bleed_radius": BLEED_RADIUS,
              "blink_normalized": [BLINK_FIRST, BLINK_LAST], "rows": rows}
    (OUT / "v8_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"frames": COUNT, "bleed_pixels_total": sum(r["transparent_rgb_bleed_pixels"] for r in rows),
                      "blink_l_before_range": [min(r["l_before"] for r in rows[91:120]), max(r["l_before"] for r in rows[91:120])],
                      "blink_l_after_range": [min(r["l_after"] for r in rows[91:120]), max(r["l_after"] for r in rows[91:120])],
                      "blink_chroma_before_range": [min(r["chroma_before"] for r in rows[91:120]), max(r["chroma_before"] for r in rows[91:120])],
                      "blink_chroma_after_range": [min(r["chroma_after"] for r in rows[91:120]), max(r["chroma_after"] for r in rows[91:120])]}, indent=2))


if __name__ == "__main__":
    main()
