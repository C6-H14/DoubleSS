"""Create a contact sheet from the newest source video for visual diagnosis."""

from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\art_preview\video")
video = max(ROOT.glob("*.mp4"), key=lambda path: path.stat().st_mtime)
tag = video.name.split("-")[4]
capture = cv2.VideoCapture(str(video))
fps = capture.get(cv2.CAP_PROP_FPS)
total = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
indices = np.linspace(0, total - 1, 20).round().astype(int)
sheet = Image.new("RGB", (5 * 420, 4 * 335), (25, 25, 25))
draw = ImageDraw.Draw(sheet)
for cell, index in enumerate(indices):
    capture.set(cv2.CAP_PROP_POS_FRAMES, int(index))
    ok, frame = capture.read()
    if not ok:
        raise RuntimeError(f"Cannot read frame {index}")
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    thumb = Image.fromarray(rgb).resize((400, 300))
    x, y = (cell % 5) * 420, (cell // 5) * 335
    sheet.paste(thumb, (x, y))
    draw.text((x + 4, y + 306), f"{index:02d} / {total - 1}   {index / fps:.2f}s", fill=(255, 255, 255))
capture.release()
out = ROOT / f"buff_video_{tag}_contact_sheet.png"
sheet.save(out)
detail_indices = [0, 12, 25, 38, 50, 66, 80, 96]
detail_sheet = Image.new("RGB", (4 * 560, 2 * 480), (25, 25, 25))
detail_draw = ImageDraw.Draw(detail_sheet)
capture = cv2.VideoCapture(str(video))
for cell, index in enumerate(detail_indices):
    capture.set(cv2.CAP_PROP_POS_FRAMES, int(index))
    ok, frame = capture.read()
    if not ok:
        raise RuntimeError(f"Cannot read detail frame {index}")
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    thumb = Image.fromarray(rgb).crop((50, 70, 700, 650)).resize((540, 450))
    x, y = (cell % 4) * 560, (cell // 4) * 480
    detail_sheet.paste(thumb, (x, y))
    detail_draw.text((x + 4, y + 453), f"{index:02d}  {index / fps:.2f}s", fill=(255, 255, 255))
capture.release()
detail_out = ROOT / f"buff_video_{tag}_detail_sheet.png"
detail_sheet.save(detail_out)
print(video)
print(f"{total} frames @ {fps:.3f} fps = {total / fps:.3f}s")
print(out)
print(detail_out)
effect_indices = list(range(58, 74))
effect_sheet = Image.new("RGB", (4 * 420, 4 * 335), (25, 25, 25))
effect_draw = ImageDraw.Draw(effect_sheet)
capture = cv2.VideoCapture(str(video))
for cell, index in enumerate(effect_indices):
    capture.set(cv2.CAP_PROP_POS_FRAMES, int(index))
    ok, frame = capture.read()
    if not ok:
        continue
    thumb = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)).resize((400, 300))
    x, y = (cell % 4) * 420, (cell // 4) * 335
    effect_sheet.paste(thumb, (x, y))
    effect_draw.text((x + 4, y + 306), str(index), fill=(255, 255, 255))
capture.release()
effect_out = ROOT / f"buff_video_{tag}_effect_sheet.png"
effect_sheet.save(effect_out)
print(effect_out)
capture = cv2.VideoCapture(str(video))
for index in detail_indices:
    capture.set(cv2.CAP_PROP_POS_FRAMES, int(index))
    ok, frame = capture.read()
    if not ok:
        continue
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    # White costume and blonde hair; excludes the purple background.
    figure = (rgb[:, :, 0] > 115) & (rgb[:, :, 1] > 75) & (np.arange(rgb.shape[1])[None, :] < 700)
    yy, xx = np.where(figure)
    print(f"frame {index:02d}: figure bbox x={xx.min()}..{xx.max()}, y={yy.min()}..{yy.max()}, height={yy.max()-yy.min()+1}")
capture.release()
