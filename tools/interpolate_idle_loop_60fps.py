from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO_ROOT = ROOT / "art_candidates" / "video_source"
OUT = VIDEO_ROOT / "idle" / "loop_v1_180f"
FRAMES = OUT / "raw_frames"
START = 17
END = 94
COUNT = 180
FPS = 60


def flow_pair(a, b):
    small_size = (a.shape[1] // 2, a.shape[0] // 2)
    aa = cv2.resize(a, small_size, interpolation=cv2.INTER_AREA)
    bb = cv2.resize(b, small_size, interpolation=cv2.INTER_AREA)
    ga = cv2.cvtColor(aa, cv2.COLOR_RGB2GRAY)
    gb = cv2.cvtColor(bb, cv2.COLOR_RGB2GRAY)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    forward = dis.calc(ga, gb, None)
    backward = dis.calc(gb, ga, None)
    forward = cv2.resize(forward, (a.shape[1], a.shape[0]), interpolation=cv2.INTER_LINEAR) * 2.0
    backward = cv2.resize(backward, (a.shape[1], a.shape[0]), interpolation=cv2.INTER_LINEAR) * 2.0
    return forward, backward


def warp(image, flow, amount):
    h, w = image.shape[:2]
    x, y = np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))
    return cv2.remap(image, x - flow[:, :, 0] * amount, y - flow[:, :, 1] * amount,
                     cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE)


def main():
    video = max(VIDEO_ROOT.glob("jimeng-2026-09-21-4865-*.mp4"), key=lambda p: p.stat().st_mtime)
    cap = cv2.VideoCapture(str(video))
    source_fps = float(cap.get(cv2.CAP_PROP_FPS))
    decoded = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        decoded.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    cap.release()
    source = decoded[START:END + 1]
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)

    targets = np.linspace(0, len(source) - 1, COUNT)
    cache = {}
    output = []
    for n, position in enumerate(targets, 1):
        left = min(int(np.floor(position)), len(source) - 1)
        right = min(left + 1, len(source) - 1)
        t = float(position - left)
        if left == right or t < 1e-6:
            frame = source[left].copy()
        else:
            if left not in cache:
                cache[left] = flow_pair(source[left], source[right])
            forward, backward = cache[left]
            wa = warp(source[left], forward, t)
            wb = warp(source[right], backward, 1.0 - t)
            frame = cv2.addWeighted(wa, 1.0 - t, wb, t, 0)
        Image.fromarray(frame, "RGB").save(FRAMES / f"idle_{n:03d}.png", optimize=True)
        output.append(Image.fromarray(frame, "RGB"))

    preview = [f.resize((533, 400), Image.Resampling.LANCZOS) for f in output]
    preview[0].save(OUT / "idle_loop_3s_60fps.gif", save_all=True, append_images=preview[1:],
                    duration=17, loop=0, disposal=2, optimize=False)
    picks = np.linspace(0, COUNT - 1, 30).round().astype(int)
    cols, cw, ch = 6, 240, 205
    sheet = Image.new("RGB", (cols * cw, 5 * ch), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        thumb = preview[index].resize((240, 180), Image.Resampling.LANCZOS)
        x, y = (n % cols) * cw, (n // cols) * ch
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 4, y + 4), f"{index + 1:03d}  {index/FPS:.2f}s", fill="white")
    sheet.save(OUT / "idle_loop_contact_30.jpg", quality=96)
    meta = {"source": str(video), "source_fps": source_fps,
            "source_range_inclusive": [START, END], "frames": COUNT,
            "fps": FPS, "duration": COUNT / FPS, "method": "bidirectional DIS optical flow"}
    (OUT / "metadata.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(meta, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
