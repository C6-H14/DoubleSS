from pathlib import Path
import json
import math

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUT = ROOT / "art_preview" / "alternate_fencer_cast_s" / "cast_s_v1_repaired"
SOURCE_FRAMES = OUT / "source_frames_97"
REPAIRED_FRAMES = OUT / "repaired_frames_97"
PAD_X = 84
PAD_Y = 63


def extended_background(rgb: np.ndarray) -> np.ndarray:
    h, w = rgb.shape[:2]
    canvas = cv2.copyMakeBorder(rgb, PAD_Y, PAD_Y, PAD_X, PAD_X, cv2.BORDER_REFLECT_101)
    blurred = cv2.GaussianBlur(canvas, (0, 0), 26)
    blurred[PAD_Y:PAD_Y + h, PAD_X:PAD_X + w] = rgb
    return blurred


def repair_sword(canvas: np.ndarray, source: np.ndarray) -> bool:
    """Extend only blades that are visibly cut by the original left/bottom border."""
    h, w = source.shape[:2]
    hsv = cv2.cvtColor(source, cv2.COLOR_RGB2HSV)
    grayish = (hsv[:, :, 1] < 78) & (hsv[:, :, 2] > 105)
    roi = np.zeros((h, w), np.uint8)
    roi[340:h, 0:min(310, w)] = 1
    mask = (grayish & (roi > 0)).astype(np.uint8) * 255
    lines = cv2.HoughLinesP(mask, 1, np.pi / 180, threshold=42,
                            minLineLength=95, maxLineGap=14)
    if lines is None:
        return False
    candidates = []
    for raw in lines.reshape(-1, 4):
        x1, y1, x2, y2 = map(int, raw)
        if x1 > x2:
            x1, y1, x2, y2 = x2, y2, x1, y1
        dx, dy = x2 - x1, y2 - y1
        length = math.hypot(dx, dy)
        if dx < 50:
            continue
        slope = dy / dx
        if -2.4 < slope < -0.35 and x1 < 55 and y1 > 500:
            candidates.append((length, x1, y1, x2, y2))
    if not candidates:
        return False
    _, x1, y1, x2, y2 = max(candidates)

    # Only patch a blade that genuinely meets the original image boundary.
    edge_hit = x1 <= 3 or y1 >= h - 3
    if not edge_hit:
        return False
    vx, vy = x2 - x1, y2 - y1
    norm = math.hypot(vx, vy)
    vx, vy = vx / norm, vy / norm
    # Reference proportions come from complete sword-tip frames: a short tapered cap.
    start = (round(x1 + PAD_X), round(y1 + PAD_Y))
    tip = (round(start[0] - vx * 72), round(start[1] - vy * 72))
    cv2.line(canvas, start, tip, (48, 43, 58), 8, cv2.LINE_AA)
    cv2.line(canvas, start, tip, (176, 177, 188), 5, cv2.LINE_AA)
    cv2.line(canvas, start, tip, (246, 242, 242), 2, cv2.LINE_AA)
    back = (round(tip[0] + vx * 13), round(tip[1] + vy * 13))
    cv2.line(canvas, tip, back, (255, 250, 242), 3, cv2.LINE_AA)
    return True


def repair_fire(canvas: np.ndarray, source: np.ndarray, frame_no: int) -> int:
    """Continue small clipped fire rays into the padding with tapered, fading ends."""
    if not 62 <= frame_no <= 80:
        return 0
    h, w = source.shape[:2]
    hsv = cv2.cvtColor(source, cv2.COLOR_RGB2HSV)
    fire = ((hsv[:, :, 0] < 35) & (hsv[:, :, 1] > 95) & (hsv[:, :, 2] > 155))
    repairs = []
    # Group boundary hits so a wide ray gets one continuation, not one per pixel.
    for side in ("right", "top"):
        values = fire[:, w - 3:w].any(axis=1) if side == "right" else fire[0:3, :].any(axis=0)
        ids = np.flatnonzero(values)
        groups = []
        for value in ids:
            if not groups or value > groups[-1][-1] + 1:
                groups.append([int(value)])
            else:
                groups[-1].append(int(value))
        for group in groups:
            if len(group) < 1:
                continue
            center = int(round(sum(group) / len(group)))
            if side == "right":
                p0 = np.array([w - 1 + PAD_X, center + PAD_Y], np.float32)
            else:
                p0 = np.array([center + PAD_X, PAD_Y], np.float32)
            # Explosion center is stable in this range; continue radially away from it.
            center_pt = np.array([710 + PAD_X, 325 + PAD_Y], np.float32)
            direction = p0 - center_pt
            direction /= max(float(np.linalg.norm(direction)), 1.0)
            length = 26 + min(len(group) * 2, 30)
            p1 = p0 + direction * length
            repairs.append((p0, p1, max(1, min(4, len(group) // 2 + 1))))

    for p0, p1, width in repairs:
        a = tuple(np.rint(p0).astype(int))
        b = tuple(np.rint(p1).astype(int))
        cv2.line(canvas, a, b, (255, 118, 18), width + 7, cv2.LINE_AA)
        cv2.line(canvas, a, b, (255, 181, 45), width + 3, cv2.LINE_AA)
        cv2.line(canvas, a, b, (255, 245, 180), width, cv2.LINE_AA)
        # Fade the final third into the padded background.
        q = p0 + (p1 - p0) * 0.72
        overlay = canvas.copy()
        cv2.line(overlay, tuple(np.rint(q).astype(int)), b, tuple(map(int, canvas[b[1], b[0]])), width + 8, cv2.LINE_AA)
        canvas[:] = cv2.addWeighted(canvas, 0.78, overlay, 0.22, 0)
    return len(repairs)


def main() -> None:
    videos = sorted(VIDEO_DIR.glob("*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not videos:
        raise RuntimeError("No input video")
    source_path = videos[0]
    cap = cv2.VideoCapture(str(source_path))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    cap.release()
    if not frames:
        raise RuntimeError(f"Cannot decode {source_path}")

    SOURCE_FRAMES.mkdir(parents=True, exist_ok=True)
    REPAIRED_FRAMES.mkdir(parents=True, exist_ok=True)
    repaired = []
    sword_frames = []
    fire_counts = {}
    for index, rgb in enumerate(frames, 1):
        Image.fromarray(rgb).save(SOURCE_FRAMES / f"cast_s_source_{index:03d}.png")
        canvas = extended_background(rgb)
        if repair_sword(canvas, rgb):
            sword_frames.append(index)
        count = repair_fire(canvas, rgb, index)
        if count:
            fire_counts[index] = count
        repaired.append(canvas)
        Image.fromarray(canvas).save(REPAIRED_FRAMES / f"cast_s_repaired_{index:03d}.png")

    out_h, out_w = repaired[0].shape[:2]
    writer = cv2.VideoWriter(str(OUT / "cast_s_v1_repaired_preview.mp4"),
                             cv2.VideoWriter_fourcc(*"mp4v"), fps, (out_w, out_h))
    for rgb in repaired:
        writer.write(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR))
    writer.release()

    gif_frames = [Image.fromarray(x).resize((640, 480), Image.Resampling.LANCZOS) for x in repaired]
    gif_frames[0].save(OUT / "cast_s_v1_repaired_preview.gif", save_all=True,
                       append_images=gif_frames[1:], duration=round(1000 / fps), loop=0,
                       disposal=2, optimize=False)

    picks = [0, 12, 24, 36, 45, 49, 54, 59, 63, 67, 71, 75, 79, 84, 90, 96]
    tw, th = 400, 300
    sheet = Image.new("RGB", (tw * 4, (th + 22) * 4), (22, 22, 22))
    draw = ImageDraw.Draw(sheet)
    for slot, idx in enumerate(picks):
        thumb = Image.fromarray(repaired[idx]).resize((tw, th), Image.Resampling.LANCZOS)
        x, y = slot % 4 * tw, slot // 4 * (th + 22)
        sheet.paste(thumb, (x, y + 22))
        draw.text((x + 5, y + 4), f"frame {idx + 1:03d}", fill="white")
    sheet.save(OUT / "contact_repaired.jpg", quality=95)

    report = {
        "source": str(source_path), "source_frames": len(frames), "fps": fps,
        "source_size": [frames[0].shape[1], frames[0].shape[0]],
        "repaired_size": [out_w, out_h], "padding": [PAD_X, PAD_Y],
        "sword_extended_frames": sword_frames,
        "fire_boundary_continuations": fire_counts,
        "method": "Sword continuation follows detected blade axis and complete-frame tip proportions; clipped fire rays receive short tapered continuations into safety padding.",
    }
    (OUT / "repair_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
