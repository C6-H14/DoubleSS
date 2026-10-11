from pathlib import Path
import json
import math

import cv2
import numpy as np
from PIL import Image, ImageDraw

from repair_alt_cast_s_video_v1 import extended_background, repair_fire, PAD_X, PAD_Y


ROOT = Path(r"D:\StSmod")
SOURCE_DIR = ROOT / "art_preview" / "alternate_fencer_cast_s" / "cast_s_v1_repaired" / "source_frames_97"
OUT = ROOT / "art_preview" / "alternate_fencer_cast_s" / "cast_s_v2_landmark_length"
FRAMES_OUT = OUT / "repaired_frames_97"
POINTS_OUT = OUT / "sword_landmark_frames"
FPS = 24.149377593360995


def detect_blade(rgb: np.ndarray):
    h, w = rgb.shape[:2]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    mask = ((hsv[:, :, 1] < 78) & (hsv[:, :, 2] > 105)).astype(np.uint8) * 255
    roi = np.zeros_like(mask)
    roi[330:h, 0:min(330, w)] = 255
    mask = cv2.bitwise_and(mask, roi)
    lines = cv2.HoughLinesP(mask, 1, np.pi / 360, 42, minLineLength=90, maxLineGap=16)
    if lines is None:
        return None
    choices = []
    for x1, y1, x2, y2 in lines.reshape(-1, 4):
        if x1 > x2:
            x1, y1, x2, y2 = x2, y2, x1, y1
        dx, dy = x2 - x1, y2 - y1
        if dx < 50:
            continue
        slope = dy / dx
        length = math.hypot(dx, dy)
        if -2.6 < slope < -0.28 and x1 < 95 and y1 > 480:
            choices.append((length, np.array([x1, y1], np.float32), np.array([x2, y2], np.float32)))
    if not choices:
        return None
    _, low_left, high_right = max(choices, key=lambda x: x[0])
    d = low_left - high_right
    d /= max(float(np.linalg.norm(d)), 1.0)

    ys, xs = np.where(mask > 0)
    pts = np.column_stack([xs, ys]).astype(np.float32)
    normal = np.array([-d[1], d[0]], np.float32)
    distance = np.abs((pts - high_right) @ normal)
    near = pts[distance < 5.2]
    if len(near) < 25:
        return None
    proj = (near - high_right) @ d
    # Detect the round guard, then anchor the blade root on its outward rim. This
    # prevents a collinear forearm highlight from being counted as sword length.
    gray = cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY)
    circles = cv2.HoughCircles(gray[330:560, 100:330], cv2.HOUGH_GRADIENT,
                               dp=1.2, minDist=18, param1=100, param2=30,
                               minRadius=18, maxRadius=55)
    guard_candidates = []
    if circles is not None:
        for cx, cy, radius in circles.reshape(-1, 3):
            center = np.array([cx + 100, cy + 330], np.float32)
            line_distance = abs(float((center - high_right) @ normal))
            if 145 <= center[0] <= 255 and 390 <= center[1] <= 520 and line_distance < 32:
                guard_candidates.append((line_distance + abs(radius - 29) * 0.3, center, float(radius)))
    if not guard_candidates:
        return None
    _, guard_center, guard_radius = min(guard_candidates, key=lambda x: x[0])
    hilt = guard_center + d * (guard_radius * 0.82)

    # Robust upper projection locates the visible tip along the blade axis.
    t1 = float(np.percentile(proj, 99.2))
    visible_tip = high_right + d * t1
    visible_length = float((visible_tip - hilt) @ d)
    touches = visible_tip[0] <= 5 or visible_tip[1] >= h - 5
    return hilt, visible_tip, d, visible_length, touches


def draw_blade_extension(canvas, start, tip):
    a = tuple(np.rint(start + [PAD_X, PAD_Y]).astype(int))
    b = tuple(np.rint(tip + [PAD_X, PAD_Y]).astype(int))
    cv2.line(canvas, a, b, (45, 41, 55), 7, cv2.LINE_AA)
    cv2.line(canvas, a, b, (170, 174, 186), 5, cv2.LINE_AA)
    cv2.line(canvas, a, b, (248, 244, 240), 2, cv2.LINE_AA)
    v = np.array(a, np.float32) - np.array(b, np.float32)
    v /= max(float(np.linalg.norm(v)), 1.0)
    cap = tuple(np.rint(np.array(b) + v * 11).astype(int))
    cv2.line(canvas, b, cap, (255, 249, 235), 3, cv2.LINE_AA)


def main():
    paths = sorted(SOURCE_DIR.glob("*.png"))
    frames = [np.array(Image.open(p).convert("RGB")) for p in paths]
    detected = [detect_blade(f) for f in frames]
    # Complete nearby frames define the weapon length; no guessed fixed extension is used.
    reference_lengths = [
        item[3] for index, item in enumerate(detected, 1)
        if item is not None and 36 <= index <= 50 and not item[4] and item[3] > 270
    ]
    if not reference_lengths:
        raise RuntimeError("No intact sword reference lengths detected")
    target_length = float(np.median(reference_lengths))

    # Keep frame-to-frame length constant while allowing a small perspective tolerance.
    target_by_frame = np.full(len(frames), target_length, np.float32)
    FRAMES_OUT.mkdir(parents=True, exist_ok=True)
    POINTS_OUT.mkdir(parents=True, exist_ok=True)
    output = []
    records = []
    for frame_no, (rgb, item) in enumerate(zip(frames, detected), 1):
        canvas = extended_background(rgb)
        record = {"frame": frame_no, "detected": item is not None}
        if item is not None:
            hilt, visible_tip, direction, visible_length, touches = item
            desired_tip = hilt + direction * target_by_frame[frame_no - 1]
            missing = float(np.linalg.norm(desired_tip - visible_tip))
            # Draw only the genuinely missing interval; overlap hides the old clipped cap.
            patched = bool(touches and missing > 3 and missing < 55)
            if patched:
                start = visible_tip - direction * 4
                draw_blade_extension(canvas, start, desired_tip)
            record.update({
                "hilt": np.round(hilt, 2).tolist(),
                "visible_tip": np.round(visible_tip, 2).tolist(),
                "desired_tip": np.round(desired_tip, 2).tolist(),
                "visible_length": round(float(visible_length), 2),
                "target_length": round(float(target_by_frame[frame_no - 1]), 2),
                "missing_length": round(missing, 2), "touches_border": bool(touches),
                "patched": patched,
            })

            annotated = canvas.copy()
            hp = tuple(np.rint(hilt + [PAD_X, PAD_Y]).astype(int))
            vp = tuple(np.rint(visible_tip + [PAD_X, PAD_Y]).astype(int))
            dp = tuple(np.rint(desired_tip + [PAD_X, PAD_Y]).astype(int))
            cv2.circle(annotated, hp, 7, (20, 230, 50), -1, cv2.LINE_AA)
            cv2.circle(annotated, vp, 7, (245, 45, 45), -1, cv2.LINE_AA)
            cv2.circle(annotated, dp, 7, (20, 220, 255), -1, cv2.LINE_AA)
            cv2.line(annotated, hp, dp, (20, 220, 255), 2, cv2.LINE_AA)
            cv2.putText(annotated, f"F{frame_no:03d} L={target_length:.1f}", (20, 34),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.75, (255, 255, 255), 2, cv2.LINE_AA)
            Image.fromarray(annotated).save(POINTS_OUT / f"sword_points_{frame_no:03d}.png")
        repair_fire(canvas, rgb, frame_no)
        Image.fromarray(canvas).save(FRAMES_OUT / f"cast_s_repaired_{frame_no:03d}.png")
        output.append(canvas)
        records.append(record)

    h, w = output[0].shape[:2]
    writer = cv2.VideoWriter(str(OUT / "cast_s_v2_landmark_length_preview.mp4"),
                             cv2.VideoWriter_fourcc(*"mp4v"), FPS, (w, h))
    for rgb in output:
        writer.write(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR))
    writer.release()
    gif = [Image.fromarray(x).resize((640, 480), Image.Resampling.LANCZOS) for x in output]
    gif[0].save(OUT / "cast_s_v2_landmark_length_preview.gif", save_all=True,
                append_images=gif[1:], duration=round(1000 / FPS), loop=0, disposal=2)

    sample_ids = [45, 48, 50, 51, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 74]
    tw, th = 400, 300
    sheet = Image.new("RGB", (tw * 4, (th + 22) * 4), (20, 20, 20))
    draw = ImageDraw.Draw(sheet)
    for slot, frame_no in enumerate(sample_ids):
        path = POINTS_OUT / f"sword_points_{frame_no:03d}.png"
        if not path.exists():
            continue
        thumb = Image.open(path).convert("RGB").resize((tw, th), Image.Resampling.LANCZOS)
        x, y = slot % 4 * tw, slot // 4 * (th + 22)
        sheet.paste(thumb, (x, y + 22))
        draw.text((x + 5, y + 4), f"frame {frame_no:03d}", fill="white")
    sheet.save(OUT / "sword_landmark_contact.jpg", quality=95)

    report = {
        "reference_frames": "36-50 intact detections",
        "reference_lengths": [round(float(x), 2) for x in reference_lengths],
        "target_length": round(target_length, 2),
        "legend": {"green": "hilt/blade root", "red": "visible source tip", "cyan": "constant-length target tip"},
        "frames": records,
    }
    (OUT / "sword_landmark_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "target_length": report["target_length"],
        "reference_lengths": report["reference_lengths"],
        "patched_frames": [x["frame"] for x in records if x.get("patched")],
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
