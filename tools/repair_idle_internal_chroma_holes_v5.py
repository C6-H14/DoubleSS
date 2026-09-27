from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
RAW = ROOT / "art_candidates" / "video_source" / "idle" / "loop_v2_short_blink_180f" / "raw_frames"
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_alpha_v4_smoother_edge" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v5"
FRAMES = OUT / "alpha_frames"
COUNT = 180

# x1, y1, x2, y2.  Character motion in the idle source remains inside these
# conservative areas; no other part of the silhouette is modified.
ROIS = {
    "collar_neck": (520, 220, 660, 325),
    "sword_leg_overlap": (520, 455, 760, 635),
}


def border_background_lab(raw_rgb):
    h, w = raw_rgb.shape[:2]
    border = np.concatenate([
        raw_rgb[:48].reshape(-1, 3), raw_rgb[h-48:].reshape(-1, 3),
        raw_rgb[:, :48].reshape(-1, 3), raw_rgb[:, w-48:].reshape(-1, 3),
    ])
    median_rgb = np.median(border, axis=0).astype(np.uint8)
    sample = median_rgb.reshape(1, 1, 3)
    return cv2.cvtColor(sample, cv2.COLOR_RGB2LAB)[0, 0].astype(np.float32)


def repair_frame(raw_path, alpha_path):
    raw_rgb = np.asarray(Image.open(raw_path).convert("RGB"), dtype=np.uint8)
    rgba = np.asarray(Image.open(alpha_path).convert("RGBA"), dtype=np.uint8).copy()
    alpha = rgba[:, :, 3].astype(np.float32)
    raw_lab = cv2.cvtColor(raw_rgb, cv2.COLOR_RGB2LAB).astype(np.float32)
    raw_hsv = cv2.cvtColor(raw_rgb, cv2.COLOR_RGB2HSV)
    bg_lab = border_background_lab(raw_rgb)
    color_distance = np.linalg.norm(raw_lab - bg_lab[None, None, :], axis=2)

    repaired = np.zeros(alpha.shape, dtype=bool)
    per_roi = {}
    for name, (x1, y1, x2, y2) in ROIS.items():
        roi = np.zeros(alpha.shape, dtype=bool)
        roi[y1:y2, x1:x2] = True

        # Holes and thin gaps must remain spatially connected to the existing
        # character.  A 9 px support radius spans the observed missing collar
        # and blade-edge texels without reaching unrelated background.
        solid = alpha >= 96
        support = cv2.dilate(solid.astype(np.uint8), np.ones((19, 19), np.uint8), iterations=1) > 0
        # Explicitly reject the magenta key family, including its darker
        # compression fringe.  Subject skin/hair/clothing and the neutral
        # sword all live outside this hue/saturation band.
        hue = raw_hsv[:, :, 0]
        sat = raw_hsv[:, :, 1]
        magenta_family = (hue >= 142) & (hue <= 179) & (sat >= 45)
        candidate = (roi & support & (alpha < 245)
                     & (color_distance >= 42.0) & ~magenta_family)

        # Convert source/background separation into a soft confidence.  Clear
        # subject colors are opaque; uncertain edge pixels remain antialiased.
        confidence = np.clip((color_distance - 38.0) / 24.0, 0.0, 1.0)
        recovered_alpha = 255.0 * confidence
        use = candidate & (recovered_alpha > alpha)
        alpha[use] = recovered_alpha[use]
        rgba[use, :3] = raw_rgb[use]
        repaired |= use
        per_roi[name] = int(np.count_nonzero(use))

    # The collar contains no intentional transparent opening. Recover any
    # small transparent component enclosed by the character regardless of its
    # source hue (lavender shadow was the main false-negative case).
    solid_now = alpha >= 128
    transparent = (~solid_now).astype(np.uint8)
    flood = transparent.copy()
    flood_mask = np.zeros((flood.shape[0] + 2, flood.shape[1] + 2), np.uint8)
    cv2.floodFill(flood, flood_mask, (0, 0), 2)
    enclosed = transparent.astype(bool) & (flood != 2)
    x1, y1, x2, y2 = ROIS["collar_neck"]
    collar_roi = np.zeros(alpha.shape, dtype=bool)
    collar_roi[y1:y2, x1:x2] = True
    enclosed_collar = enclosed & collar_roi
    alpha[enclosed_collar] = 255.0
    rgba[enclosed_collar, :3] = raw_rgb[enclosed_collar]
    repaired |= enclosed_collar
    per_roi["collar_neck"] += int(np.count_nonzero(enclosed_collar))

    # Remove pink spill from only the newly recovered edge texels.  Pull their
    # color slightly toward the nearest already-opaque subject pixel while
    # retaining the original collar/sword/skin shading.
    # Normalized local blur never samples transparent background.
    weight = (rgba[:, :, 3].astype(np.float32) / 255.0)
    denom = cv2.GaussianBlur(weight, (0, 0), 1.0)
    local = np.zeros_like(raw_rgb, dtype=np.float32)
    base = rgba[:, :, :3].astype(np.float32)
    for c in range(3):
        local[:, :, c] = cv2.GaussianBlur(base[:, :, c] * weight, (0, 0), 1.0) / np.maximum(denom, 1e-5)
    edge_repair = repaired & (alpha < 245)
    base[edge_repair] = 0.65 * base[edge_repair] + 0.35 * local[edge_repair]
    rgba[:, :, :3] = np.clip(np.rint(base), 0, 255).astype(np.uint8)
    rgba[:, :, 3] = np.clip(np.rint(alpha), 0, 255).astype(np.uint8)

    rgba[rgba[:, :, 3] == 0, :3] = 0
    return Image.fromarray(rgba, "RGBA"), per_roi


def composite(frame, color=(45, 45, 45, 255)):
    return Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)


def make_audit(before_paths, after_frames, picks):
    panels = []
    for index in picks:
        before = Image.open(before_paths[index]).convert("RGBA")
        for name, box in ROIS.items():
            # Pad each diagnostic crop and enlarge with nearest-neighbor so
            # individual alpha errors stay visible.
            x1, y1, x2, y2 = box
            crop_box = (x1, y1, x2, y2)
            left = composite(before).crop(crop_box)
            right = composite(after_frames[index]).crop(crop_box)
            pair = Image.new("RGB", ((x2-x1)*4, (y2-y1)*2 + 30), (32, 32, 32))
            pair.paste(left.resize(((x2-x1)*2, (y2-y1)*2), Image.Resampling.NEAREST), (0, 30))
            pair.paste(right.resize(((x2-x1)*2, (y2-y1)*2), Image.Resampling.NEAREST), ((x2-x1)*2, 30))
            d = ImageDraw.Draw(pair)
            d.text((6, 7), f"frame {index+1:03d} {name}: V4 left / repaired V5 right", fill="white")
            panels.append(pair)
    width = max(p.width for p in panels)
    height = sum(p.height for p in panels)
    sheet = Image.new("RGB", (width, height), (32, 32, 32))
    y = 0
    for panel in panels:
        sheet.paste(panel, (0, y))
        y += panel.height
    sheet.save(OUT / "idle_internal_holes_before_after.png", optimize=True)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    raw_paths = sorted(RAW.glob("idle_*.png"))
    source_paths = sorted(SOURCE.glob("idle_*.png"))
    if len(raw_paths) != COUNT or len(source_paths) != COUNT:
        raise RuntimeError(f"Expected {COUNT} raw/source frames, got {len(raw_paths)}/{len(source_paths)}")

    frames = []
    totals = {name: 0 for name in ROIS}
    for i, (raw_path, source_path) in enumerate(zip(raw_paths, source_paths), 1):
        frame, counts = repair_frame(raw_path, source_path)
        frame.save(FRAMES / f"idle_{i:03d}.png", optimize=True)
        frames.append(frame)
        for name, count in counts.items():
            totals[name] = totals.get(name, 0) + count

    make_audit(source_paths, frames, [0, 29, 59, 89, 119, 149, 179])
    for name, color in (("gray", (45,45,45,255)), ("black", (0,0,0,255)), ("green", (0,255,0,255))):
        views = [composite(f, color).resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for f in frames]
        views[0].save(OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)

    report = {"frames": COUNT, "recovered_pixels_by_roi": totals,
              "total_recovered": int(sum(totals.values())),
              "untouched_outside_rois": True}
    (OUT / "internal_repair_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
