from pathlib import Path

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO = ROOT / "art_candidates" / "video_source" / "cast_v1" / "cast_attack_1_source.mp4"
TEST = ROOT / "art_candidates" / "matting_tests" / "matanyone_cast_attack_1_v1"
INPUT = TEST / "input"


def smoothstep(edge0, edge1, value):
    t = np.clip((value - edge0) / (edge1 - edge0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def main():
    INPUT.mkdir(parents=True, exist_ok=True)
    capture = cv2.VideoCapture(str(VIDEO))
    ok, bgr = capture.read()
    capture.release()
    if not ok:
        raise RuntimeError(f"Cannot decode {VIDEO}")
    cv2.imwrite(str(INPUT / "first_frame.png"), bgr)

    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    hue = hsv[..., 0].astype(np.float32)
    sat = hsv[..., 1].astype(np.float32)
    red = rgb[..., 0].astype(np.float32)
    green = rgb[..., 1].astype(np.float32)
    blue = rgb[..., 2].astype(np.float32)
    border_h = np.concatenate((hue[0], hue[-1], hue[:, 0], hue[:, -1]))
    border_s = np.concatenate((sat[0], sat[-1], sat[:, 0], sat[:, -1]))
    h0 = float(np.median(border_h[border_s > 80]))
    delta = np.abs(hue - h0)
    delta = np.minimum(delta, 180.0 - delta)
    hue_match = 1.0 - smoothstep(6.0, 24.0, delta)
    sat_match = smoothstep(45.0, 115.0, sat)
    magenta_match = smoothstep(7.0, 50.0, np.minimum(red - green, blue - green))
    alpha = 1.0 - hue_match * sat_match * magenta_match
    mask = np.uint8(alpha >= 0.45) * 255

    # Keep meaningful components and close codec pinholes inside the subject.
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    clean = np.zeros_like(mask)
    for component in range(1, count):
        if stats[component, cv2.CC_STAT_AREA] >= 24:
            clean[labels == component] = 255
    clean = cv2.morphologyEx(clean, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))
    cv2.imwrite(str(INPUT / "first_mask.png"), clean)
    Image.fromarray(np.dstack((rgb, clean)), "RGBA").save(INPUT / "first_mask_preview.png")
    print(f"video={VIDEO}")
    print(f"background_hue={h0:.2f}")
    print(f"foreground_pixels={int((clean > 0).sum())}")
    print(f"bbox={cv2.boundingRect(clean)}")


if __name__ == "__main__":
    main()
