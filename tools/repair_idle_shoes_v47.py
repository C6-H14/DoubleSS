from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SOURCE = BASE / "short_blink_final_v46_loop_tail_rebuilt/alpha_frames"
OUT = BASE / "short_blink_final_v47_shoes_repaired"
FRAMES = OUT / "alpha_frames"
DONOR_FRAME = 5
TARGETS = range(1, 5)


def premultiply(rgba):
    value = rgba.astype(np.float32)
    value[:, :, :3] *= value[:, :, 3:4] / 255.0
    return value


def unpremultiply(value):
    alpha = np.clip(value[:, :, 3:4], 0, 255)
    rgb = np.zeros_like(value[:, :, :3])
    np.divide(value[:, :, :3] * 255.0, alpha, out=rgb, where=alpha > 1e-5)
    return np.rint(np.clip(np.concatenate((rgb, alpha), axis=2), 0, 255)).astype(np.uint8)


def visible_rgb(rgba):
    alpha = rgba[:, :, 3:4].astype(np.float32) / 255.0
    return np.rint(rgba[:, :, :3] * alpha + 45.0 * (1.0 - alpha)).astype(np.uint8)


def flow_from_donor(donor, target):
    size = (donor.shape[1] // 2, donor.shape[0] // 2)
    a = cv2.cvtColor(cv2.resize(visible_rgb(donor), size, interpolation=cv2.INTER_AREA),
                     cv2.COLOR_RGB2GRAY)
    b = cv2.cvtColor(cv2.resize(visible_rgb(target), size, interpolation=cv2.INTER_AREA),
                     cv2.COLOR_RGB2GRAY)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    flow = dis.calc(a, b, None)
    return cv2.resize(flow, (donor.shape[1], donor.shape[0]),
                      interpolation=cv2.INTER_LINEAR) * 2.0


def warp_forward(image, flow):
    # Invert the donor->target field approximately by remapping at target
    # coordinates. Motion is below five pixels in this range, so this is stable.
    height, width = image.shape[:2]
    x, y = np.meshgrid(np.arange(width, dtype=np.float32),
                       np.arange(height, dtype=np.float32))
    return cv2.remap(image, x - flow[:, :, 0], y - flow[:, :, 1],
                     cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT,
                     borderValue=0)


def donor_shoe_mask(rgba):
    rgb = rgba[:, :, :3]
    alpha = rgba[:, :, 3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    yy, xx = np.indices(alpha.shape)
    shoe_rois = (((xx >= 330) & (xx < 475)) |
                 ((xx >= 680) & (xx < 835))) & (yy >= 675) & (yy < 805)
    brown = ((hsv[:, :, 0] <= 20) & (hsv[:, :, 1] >= 42) &
             (hsv[:, :, 2] <= 205) & (alpha >= 16) & shoe_rois)
    # Include antialias pixels and the dark outline around the brown body.
    mask = cv2.dilate(brown.astype(np.uint8), np.ones((5, 5), np.uint8), iterations=1)
    mask &= ((alpha > 0) & shoe_rois).astype(np.uint8)
    return cv2.GaussianBlur(mask.astype(np.float32), (5, 5), 0.8)


def repair(donor, target):
    flow = flow_from_donor(donor, target)
    warped = unpremultiply(warp_forward(premultiply(donor), flow))
    mask = warp_forward(donor_shoe_mask(donor), flow)
    mask = np.clip(mask[:, :, None], 0.0, 1.0)
    # Composite in premultiplied space to keep the shoe outline clean.
    target_pm = premultiply(target)
    donor_pm = premultiply(warped)
    merged = target_pm * (1.0 - mask) + donor_pm * mask
    return unpremultiply(merged), mask[:, :, 0]


def composite(array):
    image = Image.fromarray(array, "RGBA")
    return Image.alpha_composite(Image.new("RGBA", image.size, (45, 45, 45, 255)), image)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    for path in SOURCE.glob("idle_*.png"):
        shutil.copy2(path, FRAMES / path.name)

    donor = np.asarray(Image.open(SOURCE / f"idle_{DONOR_FRAME:03d}.png").convert("RGBA"), np.uint8)
    panels = []
    reports = []
    for frame_no in TARGETS:
        path = SOURCE / f"idle_{frame_no:03d}.png"
        target = np.asarray(Image.open(path).convert("RGBA"), np.uint8)
        fixed, mask = repair(donor, target)
        Image.fromarray(fixed, "RGBA").save(FRAMES / path.name, optimize=True)
        before = composite(target).crop((300, 640, 860, 820))
        after = composite(fixed).crop((300, 640, 860, 820))
        panel = Image.new("RGB", (1120, 210), (25, 25, 25))
        panel.paste(before.convert("RGB"), (0, 30))
        panel.paste(after.convert("RGB"), (560, 30))
        draw = ImageDraw.Draw(panel)
        draw.text((5, 6), f"F{frame_no:03d} BEFORE", fill="white")
        draw.text((565, 6), f"F{frame_no:03d} AFTER", fill="white")
        panels.append(panel)
        reports.append({"frame": frame_no, "mask_pixels": int((mask > 0.05).sum())})

    sheet = Image.new("RGB", (1120, len(panels) * 210), (25, 25, 25))
    for index, panel in enumerate(panels):
        sheet.paste(panel, (0, index * 210))
    sheet.save(OUT / "shoe_repair_before_after.png", optimize=True)
    (OUT / "report.json").write_text(json.dumps({"donor": DONOR_FRAME,
        "targets": reports, "method": "optical-flow tracked rigid shoe donor"}, indent=2) + "\n",
        encoding="utf-8")
    print(OUT)


if __name__ == "__main__":
    main()
