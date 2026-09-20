from pathlib import Path

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO = ROOT / "art_candidates" / "video_source" / "attack_sword_v2.mp4"
OUT = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_v1" / "input"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    capture = cv2.VideoCapture(str(VIDEO))
    ok, bgr = capture.read()
    if not ok:
        raise RuntimeError(f"Cannot read first frame: {VIDEO}")
    cv2.imwrite(str(OUT / "first_frame.png"), bgr)

    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    h, w = rgb.shape[:2]
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    background = np.median(border, axis=0)
    distance = np.max(np.abs(rgb.astype(np.int16) - background.astype(np.int16)), axis=2)
    saturation = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)[..., 1]

    # Only near-neutral pixels close to the measured border color may be
    # background. Keeping only the border-connected component preserves the
    # white sword blade and socks enclosed by their outlines.
    candidate = ((distance <= 28) & (saturation <= 35)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(candidate, 8)
    border_labels = set(labels[0]) | set(labels[-1]) | set(labels[:, 0]) | set(labels[:, -1])
    bg = np.zeros((h, w), dtype=np.uint8)
    for label in border_labels:
        if label != 0 and stats[label, cv2.CC_STAT_AREA] > 100:
            bg[labels == label] = 255
    mask = 255 - bg

    # Keep all sizeable foreground components: the sword can be separated by
    # one antialiased pixel in generated footage, so do not assume one blob.
    count, labels, stats, _ = cv2.connectedComponentsWithStats((mask > 0).astype(np.uint8), 8)
    cleaned = np.zeros_like(mask)
    for label in range(1, count):
        if stats[label, cv2.CC_STAT_AREA] >= 20:
            cleaned[labels == label] = 255
    cv2.imwrite(str(OUT / "first_mask.png"), cleaned)

    rgba = np.dstack((rgb, cleaned))
    Image.fromarray(rgba, "RGBA").save(OUT / "first_mask_preview.png")
    print(f"background={background.tolist()} foreground_pixels={int((cleaned > 0).sum())}")


if __name__ == "__main__":
    main()
