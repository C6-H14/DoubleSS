from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO_ROOT = ROOT / "art_candidates" / "video_source"
OUT = VIDEO_ROOT / "hurt" / "final_v4_42f_700ms"
RAW = OUT / "raw_frames"
ALPHA = OUT / "alpha_frames"
ANCHORS = [0, 2, 4, 6, 8, 15, 47, 59, 71, 85, 96]
COUNT = 42
FPS = 60


def expanded_indices():
    picks = set(ANCHORS)
    while len(picks) < COUNT:
        ordered = sorted(picks)
        gaps = [(b - a, a, b) for a, b in zip(ordered, ordered[1:]) if b - a > 1]
        _, a, b = max(gaps)
        picks.add((a + b) // 2)
    return sorted(picks)


def matte(rgb):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    background = (h >= 157) & (h <= 171) & (s >= 60) & (v >= 120)
    background = cv2.morphologyEx(background.astype(np.uint8), cv2.MORPH_CLOSE,
                                  np.ones((3, 3), np.uint8)).astype(bool)
    foreground = ~background
    distance = cv2.distanceTransform(foreground.astype(np.uint8), cv2.DIST_L2, 5)
    alpha = np.uint8(np.clip(distance / 1.25, 0.0, 1.0) * 255.0)
    alpha[background] = 0
    alpha[alpha > 250] = 255
    opaque = alpha >= 250
    _, nearest = distance_transform_edt(~opaque, return_indices=True)
    clean = rgb.copy()
    edge = (alpha > 0) & (alpha < 250)
    clean[edge] = rgb[nearest[0], nearest[1]][edge]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def preview(frames, name, color):
    views = [Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)
             .resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for frame in frames]
    views[0].save(OUT / f"hurt_{name}_42f_60fps_700ms.gif", save_all=True,
                  append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)
    cols, cell_w, cell_h = 7, 190, 160
    sheet = Image.new("RGB", (cols * cell_w, 6 * cell_h), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for i, view in enumerate(views, 1):
        thumb = view.resize((190, 143), Image.Resampling.LANCZOS)
        x, y = ((i - 1) % cols) * cell_w, ((i - 1) // cols) * cell_h
        sheet.paste(thumb, (x, y + 17))
        draw.text((x + 3, y + 2), f"{i:02d}", fill="white")
    sheet.save(OUT / f"hurt_{name}_contact.jpg", quality=95)


def main():
    videos = list(VIDEO_ROOT.glob("jimeng-2026-09-21-5339-*.mp4"))
    if len(videos) != 1:
        raise RuntimeError(f"Expected one 5339 hurt video, found {len(videos)}")
    cap = cv2.VideoCapture(str(videos[0]))
    source_fps = float(cap.get(cv2.CAP_PROP_FPS))
    decoded = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        decoded.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    cap.release()

    picks = expanded_indices()
    if max(picks) >= len(decoded):
        raise RuntimeError(f"Source has {len(decoded)} frames")
    if OUT.exists():
        shutil.rmtree(OUT)
    RAW.mkdir(parents=True)
    ALPHA.mkdir(parents=True)
    frames = []
    for i, source_index in enumerate(picks, 1):
        raw = Image.fromarray(decoded[source_index], "RGB")
        raw.save(RAW / f"hurt_{i:03d}.png", optimize=True)
        frame = matte(decoded[source_index])
        frame.save(ALPHA / f"hurt_{i:03d}.png", optimize=True)
        frames.append(frame)

    preview(frames, "black", (0, 0, 0, 255))
    preview(frames, "white", (255, 255, 255, 255))
    meta = {"source": str(videos[0]), "source_fps": source_fps,
            "anchors": ANCHORS, "selected_source_frames": picks,
            "frames": COUNT, "fps": FPS, "duration": COUNT / FPS}
    (OUT / "metadata.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(meta, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
