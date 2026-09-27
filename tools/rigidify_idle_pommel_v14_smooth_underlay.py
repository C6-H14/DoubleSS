from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import gaussian_filter1d, median_filter


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v12_loop_color_stable" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v14_smooth_underlay"
FRAMES = OUT / "alpha_frames"
DONOR = 1


def gold_mask(rgba):
    hsv = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2HSV)
    return ((rgba[:, :, 3] > 40) & (hsv[:, :, 0] >= 2) &
            (hsv[:, :, 0] <= 35) & (hsv[:, :, 1] > 45))


def detect_guard(rgba):
    mask = gold_mask(rgba)
    roi = np.zeros(mask.shape, np.uint8)
    roi[395:535, 480:625] = 1
    mask &= roi.astype(bool)
    n, labels, stats, centers = cv2.connectedComponentsWithStats(mask.astype(np.uint8), 8)
    candidates = []
    for i in range(1, n):
        area = stats[i, cv2.CC_STAT_AREA]
        if area >= 45:
            cx, cy = centers[i]
            candidates.append((area, np.array([cx, cy], np.float32)))
    if not candidates:
        raise RuntimeError("guard not found")
    # The guard is the largest gold component around the hands.
    return max(candidates, key=lambda x: x[0])[1]


def detect_blade_axis(rgba, guard):
    rgb = rgba[:, :, :3].astype(np.int16)
    alpha = rgba[:, :, 3]
    hsv = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2HSV)
    yy, xx = np.indices(alpha.shape)
    dx, dy = xx - guard[0], yy - guard[1]
    # Silver/lilac blade pixels, lower-right of the guard. Broad enough for
    # highlights, narrow enough to reject skin, socks and the skirt.
    silver = ((alpha > 100) & (hsv[:, :, 1] < 105) & (hsv[:, :, 2] > 105) &
              (dx > 5) & (dx < 390) & (dy > -5) & (dy < 310) &
              (dy > dx * 0.35) & (dy < dx * 1.05))
    pts = np.column_stack(np.nonzero(silver))[:, ::-1].astype(np.float32)
    if len(pts) < 100:
        raise RuntimeError("blade pixels not found")
    centered = pts - pts.mean(axis=0)
    _, _, vt = np.linalg.svd(centered, full_matrices=False)
    axis = vt[0]
    if axis[0] < 0:
        axis = -axis
    # Keep the expected lower-right orientation and reject accidental legs.
    axis = axis / np.linalg.norm(axis)
    if not (0.35 < axis[1] / max(axis[0], 1e-6) < 1.10):
        axis = np.array([0.78, 0.625], np.float32)
        axis /= np.linalg.norm(axis)
    return axis.astype(np.float32)


def transform_matrix(src_guard, src_axis, dst_guard, dst_axis):
    a0 = np.arctan2(src_axis[1], src_axis[0])
    a1 = np.arctan2(dst_axis[1], dst_axis[0])
    angle = a1 - a0
    c, s = np.cos(angle), np.sin(angle)
    rot = np.array([[c, -s], [s, c]], np.float32)
    translation = dst_guard - rot @ src_guard
    return np.column_stack((rot, translation)).astype(np.float32)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    donor_rgba = np.asarray(Image.open(paths[DONOR-1]).convert("RGBA"), dtype=np.uint8)
    donor_guard = detect_guard(donor_rgba)
    donor_axis = detect_blade_axis(donor_rgba, donor_guard)

    yy, xx = np.indices(donor_rgba.shape[:2])
    relx, rely = xx - donor_guard[0], yy - donor_guard[1]
    along_back = -(relx * donor_axis[0] + rely * donor_axis[1])
    perp = np.abs(-relx * donor_axis[1] + rely * donor_axis[0])
    donor_weapon = gold_mask(donor_rgba)
    # Standard rigid tail: fixed 32..112 px behind the guard, within its narrow
    # shaft/pommel corridor. Everything else is transparent in the sprite.
    tail_mask = donor_weapon & (along_back >= 32) & (along_back <= 112) & (perp <= 22)
    tail_mask = cv2.dilate(tail_mask.astype(np.uint8), np.ones((3,3),np.uint8)) > 0
    tail_sprite = donor_rgba.copy()
    tail_sprite[:, :, 3] = np.where(tail_mask, donor_rgba[:, :, 3], 0).astype(np.uint8)

    # Detect first, then remove temporal outliers. Several frames otherwise
    # select a neighboring gold component and jump ~15-17 px in one frame.
    raw_guards = []
    raw_angles = []
    source_arrays = []
    for path in paths:
        rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)
        guard = detect_guard(rgba)
        axis = detect_blade_axis(rgba, guard)
        source_arrays.append(rgba)
        raw_guards.append(guard)
        raw_angles.append(np.arctan2(axis[1], axis[0]))
    raw_guards = np.asarray(raw_guards, np.float32)
    center = np.median(raw_guards, axis=0)
    valid = np.linalg.norm(raw_guards - center, axis=1) <= 8.0
    frame_index = np.arange(len(raw_guards), dtype=np.float32)
    repaired_guards = raw_guards.copy()
    for ch in (0, 1):
        repaired_guards[:, ch] = np.interp(frame_index, frame_index[valid], raw_guards[valid, ch])
    filtered_guards = median_filter(repaired_guards, size=(5, 1), mode="nearest")
    smooth_guards = gaussian_filter1d(filtered_guards, sigma=1.5, axis=0, mode="nearest")
    raw_angles = np.unwrap(np.asarray(raw_angles, np.float32))
    repaired_angles = np.interp(frame_index, frame_index[valid], raw_angles[valid])
    filtered_angles = median_filter(repaired_angles, size=5, mode="nearest")
    smooth_angles = gaussian_filter1d(filtered_angles, sigma=1.5, mode="nearest")

    records, images = [], []
    for index, rgba0 in enumerate(source_arrays, 1):
        rgba = rgba0.copy()
        guard = smooth_guards[index - 1]
        angle = smooth_angles[index - 1]
        axis = np.array([np.cos(angle), np.sin(angle)], np.float32)
        matrix = transform_matrix(donor_guard, donor_axis, guard, axis)
        warped = cv2.warpAffine(tail_sprite, matrix, (rgba.shape[1], rgba.shape[0]),
                                flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT,
                                borderValue=(0,0,0,0))

        yy2, xx2 = np.indices(rgba.shape[:2])
        rx, ry = xx2 - guard[0], yy2 - guard[1]
        back = -(rx * axis[0] + ry * axis[1])
        side = np.abs(-rx * axis[1] + ry * axis[0])
        old_tail_zone = (back >= 30) & (back <= 125) & (side <= 25)
        old_weapon = gold_mask(rgba) & old_tail_zone
        canonical = warped[:, :, 3] > 0
        keep = cv2.dilate(canonical.astype(np.uint8), np.ones((7, 7), np.uint8)) > 0
        clear = old_weapon & (~keep)
        rgba[clear] = 0

        # Canonical sword is the background layer; the untouched hand/sleeve
        # is composited over it. This correctly fills beneath antialiased hand
        # edges and removes the visible cut seam.
        under = Image.fromarray(warped, "RGBA")
        foreground = Image.fromarray(rgba, "RGBA")
        composed = Image.alpha_composite(under, foreground)
        rgba = np.asarray(composed, dtype=np.uint8).copy()
        insert = canonical & (rgba0[:, :, 3] < 255)
        image = Image.fromarray(rgba, "RGBA")
        image.save(FRAMES / f"idle_{index:03d}.png", optimize=True)
        images.append(image)
        records.append({"frame": index, "guard": guard.tolist(), "axis": axis.tolist(),
                        "raw_guard": raw_guards[index-1].tolist(),
                        "cleared": int(clear.sum()), "inserted": int(insert.sum())})

    views = [Image.alpha_composite(Image.new("RGBA", im.size, (36,25,25,255)), im)
             .resize((533,400), Image.Resampling.LANCZOS).convert("RGB") for im in images]
    views[0].save(OUT / "idle_game_dark_3s_60fps.gif", save_all=True,
                  append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)

    picks = sorted(set(list(range(1,181,5)) + [16,57,120,180]))
    sheet = Image.new("RGB", (320*5, 250*((len(picks)+4)//5)), (35,35,35))
    draw = ImageDraw.Draw(sheet)
    for k,n in enumerate(picks):
        crop = images[n-1].crop((400,350,625,530)).resize((320,247), Image.Resampling.NEAREST)
        bg=Image.new("RGBA",crop.size,(40,40,40,255));bg.alpha_composite(crop)
        x,y=(k%5)*320,(k//5)*250;sheet.paste(bg.convert("RGB"),(x,y));draw.text((x+4,y+4),f"{n:03d}",fill="white")
    sheet.save(OUT / "smooth_underlay_audit_contact.png", optimize=True)
    report={"frames":len(paths),"donor":DONOR,"donor_guard":donor_guard.tolist(),
            "donor_axis":donor_axis.tolist(),"records":records}
    (OUT/"rigid_pommel_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"frames":len(paths),"cleared_total":sum(x['cleared'] for x in records),
                      "inserted_total":sum(x['inserted'] for x in records),
                      "axis_angle_range_deg":[float(np.degrees(np.arctan2(min(x['axis'][1] for x in records),max(x['axis'][0] for x in records)))),float(np.degrees(np.arctan2(max(x['axis'][1] for x in records),min(x['axis'][0] for x in records))))]},indent=2))


if __name__ == "__main__":
    main()
