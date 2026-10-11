"""Compare the regenerated buff video's endpoints with the active idle pose."""

from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


video = next(Path(r"D:\StSmod\art_preview\video").glob("jimeng-2026-09-27-4191-*.mp4"))
idle_path = Path(r"D:\StSmod\DoubleSS\art_candidates\character_runtime_v34_idle_60fps\images\alt_idle_001.png")
out = Path(r"D:\StSmod\art_preview\video\buff_video_4191_vs_runtime_idle.png")
capture = cv2.VideoCapture(str(video))
source = []
for index in (0, 96):
    capture.set(cv2.CAP_PROP_POS_FRAMES, index)
    ok, bgr = capture.read()
    if not ok:
        raise RuntimeError(f"Cannot read frame {index}")
    source.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
capture.release()
idle = np.asarray(Image.open(idle_path).convert("RGBA"), dtype=np.float32)
scale = 563.0 / 336.0
matrix = np.float32([[scale, 0, 143 - 380 * scale], [0, scale, 122 - 393 * scale]])
warped = cv2.warpAffine(idle, matrix, (1112, 834), flags=cv2.INTER_LINEAR,
                        borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))
background = np.empty((834, 1112, 3), dtype=np.float32)
background[:] = [62, 3, 108]
runtime = np.clip(warped[:, :, :3] + background * (1 - warped[:, :, 3:4] / 255), 0, 255).astype(np.uint8)
sheet = Image.new("RGB", (3 * 556, 460), (32, 32, 32))
draw = ImageDraw.Draw(sheet)
for col, (label, frame) in enumerate((
    ("new video frame 000", source[0]),
    ("new video frame 096", source[1]),
    ("active runtime idle 001", runtime),
)):
    sheet.paste(Image.fromarray(frame).crop((80, 105, 700, 680)).resize((540, 430)), (col * 556, 0))
    draw.text((col * 556 + 4, 436), label, fill=(255, 255, 255))
sheet.save(out)
print(out)
