from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SOURCE = BASE / "short_blink_final_v44_semantic_color_stable/alpha_frames"
OUT = BASE / "short_blink_final_v46_loop_tail_rebuilt"
FRAMES = OUT / "alpha_frames"
ANCHOR_FRAME = 173
TARGET_FRAME = 1
FIRST_REBUILT = 174
LAST_REBUILT = 180


def premultiply(rgba):
    value = rgba.astype(np.float32)
    alpha = value[:, :, 3:4] / 255.0
    return np.concatenate((value[:, :, :3] * alpha, value[:, :, 3:4]), axis=2)


def unpremultiply(value):
    alpha = np.clip(value[:, :, 3:4], 0, 255)
    rgb = np.zeros_like(value[:, :, :3])
    np.divide(value[:, :, :3] * 255.0, alpha, out=rgb, where=alpha > 1e-5)
    return np.rint(np.clip(np.concatenate((rgb, alpha), axis=2), 0, 255)).astype(np.uint8)


def flow_pair(a, b):
    # Optical flow sees the same dark neutral background used for review, so
    # transparent RGB cannot pull the face or hair motion in arbitrary ways.
    def visible_rgb(rgba):
        alpha = rgba[:, :, 3:4].astype(np.float32) / 255.0
        return np.rint(rgba[:, :, :3] * alpha + 45.0 * (1.0 - alpha)).astype(np.uint8)
    aa = visible_rgb(a)
    bb = visible_rgb(b)
    size = (a.shape[1] // 2, a.shape[0] // 2)
    ga = cv2.cvtColor(cv2.resize(aa, size, interpolation=cv2.INTER_AREA), cv2.COLOR_RGB2GRAY)
    gb = cv2.cvtColor(cv2.resize(bb, size, interpolation=cv2.INTER_AREA), cv2.COLOR_RGB2GRAY)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    forward = dis.calc(ga, gb, None)
    backward = dis.calc(gb, ga, None)
    forward = cv2.resize(forward, (a.shape[1], a.shape[0]), interpolation=cv2.INTER_LINEAR) * 2.0
    backward = cv2.resize(backward, (a.shape[1], a.shape[0]), interpolation=cv2.INTER_LINEAR) * 2.0
    return forward, backward


def warp(image, flow, amount):
    height, width = image.shape[:2]
    x, y = np.meshgrid(np.arange(width, dtype=np.float32), np.arange(height, dtype=np.float32))
    return cv2.remap(
        image,
        x - flow[:, :, 0] * amount,
        y - flow[:, :, 1] * amount,
        cv2.INTER_LINEAR,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=0,
    )


def composite(array):
    image = Image.fromarray(array, "RGBA")
    return Image.alpha_composite(Image.new("RGBA", image.size, (45, 45, 45, 255)), image).convert("RGB")


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    source = [np.asarray(Image.open(SOURCE / f"idle_{i:03d}.png").convert("RGBA"), np.uint8) for i in range(1, 181)]
    output = [frame.copy() for frame in source]
    anchor = source[ANCHOR_FRAME - 1]
    target = source[TARGET_FRAME - 1]
    forward, backward = flow_pair(anchor, target)
    anchor_pm = premultiply(anchor)
    target_pm = premultiply(target)
    steps = LAST_REBUILT - ANCHOR_FRAME + 1  # Includes the unseen final step to frame 1.
    details = []
    for frame_no in range(FIRST_REBUILT, LAST_REBUILT + 1):
        t = (frame_no - ANCHOR_FRAME) / steps
        warped_a = warp(anchor_pm, forward, t)
        warped_b = warp(target_pm, backward, 1.0 - t)
        mixed = warped_a * (1.0 - t) + warped_b * t
        output[frame_no - 1] = unpremultiply(mixed)
        details.append({"frame": frame_no, "progress_to_frame_1": round(float(t), 4)})
    for number, frame in enumerate(output, 1):
        Image.fromarray(frame, "RGBA").save(FRAMES / f"idle_{number:03d}.png", optimize=True)

    panels = []
    for frame_no in range(170, 181):
        before = composite(source[frame_no - 1])
        after = composite(output[frame_no - 1])
        before.thumbnail((278, 209), Image.Resampling.LANCZOS)
        after.thumbnail((278, 209), Image.Resampling.LANCZOS)
        panel = Image.new("RGB", (556, 231), (25, 25, 25))
        panel.paste(before, (0, 22))
        panel.paste(after, (278, 22))
        draw = ImageDraw.Draw(panel)
        draw.text((4, 4), f"V44 F{frame_no:03d}", fill="white")
        draw.text((282, 4), f"V46 F{frame_no:03d}", fill="white")
        panels.append(panel)
    sheet = Image.new("RGB", (556, 231 * len(panels)), (20, 20, 20))
    for i, panel in enumerate(panels):
        sheet.paste(panel, (0, i * 231))
    sheet.save(OUT / "tail_170_180_before_after.png", optimize=True)

    report = {
        "anchor_frame": ANCHOR_FRAME,
        "target_frame": TARGET_FRAME,
        "rebuilt_frames": [FIRST_REBUILT, LAST_REBUILT],
        "method": "bidirectional DIS optical flow on premultiplied RGBA",
        "details": details,
    }
    (OUT / "v46_loop_tail_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
