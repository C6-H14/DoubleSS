from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO_ROOT = ROOT / "art_candidates" / "video_source"
OUT = VIDEO_ROOT / "idle" / "loop_v2_short_blink_180f"
FRAMES = OUT / "raw_frames"
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


def interpolate(source, position, cache):
    left = min(int(np.floor(position)), len(source) - 1)
    right = min(left + 1, len(source) - 1)
    t = float(position - left)
    if left == right or t < 1e-6:
        return source[left].copy()
    if left not in cache:
        cache[left] = flow_pair(source[left], source[right])
    forward, backward = cache[left]
    wa = warp(source[left], forward, t)
    wb = warp(source[right], backward, 1.0 - t)
    return cv2.addWeighted(wa, 1.0 - t, wb, t, 0)


def main():
    video = max(VIDEO_ROOT.glob("jimeng-2026-09-21-4865-*.mp4"), key=lambda p: p.stat().st_mtime)
    cap = cv2.VideoCapture(str(video))
    source_fps = float(cap.get(cv2.CAP_PROP_FPS))
    source = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        source.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    cap.release()
    if len(source) < 121:
        raise RuntimeError(f"Expected at least 121 frames, got {len(source)}")

    # 0-90: ordinary open-eye breathing (source 0-58).
    # 91-119: one 0.48s close/open blink. The generated long closed-eye hold
    # is compressed, but source time still advances monotonically in small
    # steps so the head, hair and torso cannot jump after the blink.
    # 120-179: open-eye recovery (source 99-120). Source frame 120 is the
    # generated loop endpoint and closely matches source frame 0.
    positions = np.concatenate([
        np.linspace(0.0, 58.0, 91),
        np.concatenate([
            np.linspace(58.5, 66.0, 8),
            np.linspace(67.0, 90.0, 13),
            np.linspace(91.0, 99.0, 8),
        ]),
        np.linspace(99.4, 120.0, 60),
    ])
    if len(positions) != COUNT:
        raise RuntimeError(len(positions))

    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    cache, output = {}, []
    for n, position in enumerate(positions, 1):
        frame = interpolate(source, float(position), cache)
        Image.fromarray(frame, "RGB").save(FRAMES / f"idle_{n:03d}.png", optimize=True)
        output.append(Image.fromarray(frame, "RGB"))

    preview = [f.resize((533, 400), Image.Resampling.LANCZOS) for f in output]
    preview[0].save(OUT / "idle_short_blink_raw_3s_60fps.gif", save_all=True,
                    append_images=preview[1:], duration=17, loop=0, disposal=2, optimize=False)
    picks = np.linspace(0, COUNT - 1, 30).round().astype(int)
    sheet = Image.new("RGB", (6 * 240, 5 * 205), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        thumb = preview[index].resize((240, 180), Image.Resampling.LANCZOS)
        x, y = (n % 6) * 240, (n // 6) * 205
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 4, y + 4), f"{index+1:03d} {index/FPS:.2f}s src={positions[index]:.1f}", fill="white")
    sheet.save(OUT / "idle_short_blink_raw_contact.jpg", quality=97)
    meta = {
        "source": str(video), "source_fps": source_fps, "frames": COUNT,
        "fps": FPS, "duration": 3.0, "blink_output_frames_inclusive": [92, 120],
        "blink_duration_seconds": 29 / 60,
        "timeline": "open 1-91; continuous compressed blink 92-120; open recovery 121-180",
    }
    (OUT / "metadata.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(meta, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
