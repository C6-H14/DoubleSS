"""Visual-only comparison of buff preview endpoints with the game idle frame."""

from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_buff\buff_v1_60f_60fps\frames_60fps")
IDLE = Path(r"D:\StSmod\DoubleSS\art_candidates\character_runtime_v34_idle_60fps\images\alt_idle_001.png")
OUT = ROOT.parent / "buff_vs_idle_end.png"

first = np.asarray(Image.open(ROOT / "buff_001.png").convert("RGB"), dtype=np.uint8)
last = np.asarray(Image.open(ROOT / "buff_060.png").convert("RGB"), dtype=np.uint8)
idle = np.asarray(Image.open(IDLE).convert("RGBA"), dtype=np.float32)

# Compare at the source video's display size, matching head top and feet.
scale = 551.0 / 336.0
matrix = np.float32([[scale, 0, 142 - scale * 380], [0, scale, 135 - scale * 393]])
rgba = cv2.warpAffine(idle, matrix, (1112, 834), flags=cv2.INTER_LINEAR,
                      borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))
background = np.asarray(first, dtype=np.float32)
background[:] = [62, 3, 108]
idle_rgb = np.clip(rgba[:, :, :3] + background * (1 - rgba[:, :, 3:4] / 255), 0, 255).astype(np.uint8)

canvas = Image.new("RGB", (3 * 556, 460), (32, 32, 32))
draw = ImageDraw.Draw(canvas)
for col, (label, image) in enumerate((
    ("buff 001 / source 000", first),
    ("buff 060 / source 096", last),
    ("actual runtime idle 001", idle_rgb),
)):
    crop = Image.fromarray(image).crop((80, 105, 700, 680)).resize((540, 430))
    canvas.paste(crop, (col * 556, 0))
    draw.text((col * 556 + 4, 436), label, fill=(255, 255, 255))
canvas.save(OUT)

roi = np.s_[105:680, 80:700]
print(f"buff first-vs-last mean RGB difference: {np.abs(first[roi].astype(float)-last[roi].astype(float)).mean():.2f}")
print(f"buff last-vs-idle mean RGB difference: {np.abs(last[roi].astype(float)-idle_rgb[roi].astype(float)).mean():.2f}")
print(OUT)
