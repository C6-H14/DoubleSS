from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE_DIR = ROOT / "art_candidates" / "video_source"
TEST = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_pink_v1"
INPUT = TEST / "input"


def main():
    candidates = sorted(SOURCE_DIR.glob("jimeng-*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not candidates:
        raise RuntimeError(f"No jimeng MP4 in {SOURCE_DIR}")
    video = candidates[0]
    INPUT.mkdir(parents=True, exist_ok=True)
    capture = cv2.VideoCapture(str(video))
    ok, bgr = capture.read()
    if not ok:
        raise RuntimeError(f"Cannot decode {video}")
    cv2.imwrite(str(INPUT / "first_frame.png"), bgr)

    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    h, w = rgb.shape[:2]
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    background = np.median(border, axis=0)
    distance = np.max(np.abs(rgb.astype(np.int16) - background.astype(np.int16)), axis=2)
    saturation = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)[..., 1]
    candidate_bg = ((distance <= 34) & (saturation <= 48)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(candidate_bg, 8)
    border_labels = set(labels[0]) | set(labels[-1]) | set(labels[:, 0]) | set(labels[:, -1])
    bg = np.zeros((h, w), np.uint8)
    for label in border_labels:
        if label and stats[label, cv2.CC_STAT_AREA] >= 100:
            bg[labels == label] = 255
    mask = 255 - bg
    count, labels, stats, _ = cv2.connectedComponentsWithStats((mask > 0).astype(np.uint8), 8)
    cleaned = np.zeros_like(mask)
    for label in range(1, count):
        if stats[label, cv2.CC_STAT_AREA] >= 20:
            cleaned[labels == label] = 255
    cv2.imwrite(str(INPUT / "first_mask.png"), cleaned)
    Image.fromarray(np.dstack((rgb, cleaned)), "RGBA").save(INPUT / "first_mask_preview.png")
    (TEST / "source.json").write_text(json.dumps({"video":str(video)},ensure_ascii=False,indent=2),encoding="utf-8")
    print(video)
    print(f"background={background.tolist()} foreground_pixels={int((cleaned>0).sum())}")


if __name__ == "__main__":
    main()
