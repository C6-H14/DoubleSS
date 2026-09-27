from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.interpolate import RBFInterpolator


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "hurt_v5_direct_cuts" / "hurt_v5_44f_frames_60fps"
OUT = ROOT / "art_preview" / "hurt_v8_measured_landmark_warp"
FRAMES = OUT / "hurt_v8_46f_frames_60fps"
GIF = OUT / "hurt_v8_46f_60fps.gif"
MP4 = OUT / "hurt_v8_46f_60fps.mp4"
FPS = 60

# First five are the measured motion features. Remaining controls preserve
# anatomy and keep the background/canvas fixed while the RBF deforms locally.
NAMES = ["visible_eye", "covered_eye", "sword_hand", "abdomen_hand", "sword_tip"]
SRC = np.array([
    (552, 148), (587, 149), (599, 349), (528, 342), (865, 364),
    (560, 55), (570, 190), (470, 205), (505, 414), (348, 180),
    (681, 520), (395, 545), (750, 745), (300, 765),
    (0, 0), (556, 0), (1111, 0), (0, 417), (1111, 417),
    (0, 833), (556, 833), (1111, 833),
], dtype=np.float64)
DST = np.array([
    (541, 131), (577, 136), (608, 330), (532, 335), (863, 315),
    (550, 45), (559, 176), (465, 195), (505, 402), (338, 166),
    (682, 520), (396, 545), (750, 745), (300, 765),
    (0, 0), (556, 0), (1111, 0), (0, 417), (1111, 417),
    (0, 833), (556, 833), (1111, 833),
], dtype=np.float64)
COLORS = [(255, 70, 70), (255, 190, 40), (60, 220, 255),
          (80, 255, 100), (255, 90, 220)]


def warp_at(rgb: np.ndarray, t: float) -> np.ndarray:
    target = SRC + (DST - SRC) * t
    # Inverse mapping: destination control positions map back to the original
    # source positions. Thin-plate RBF passes exactly through all landmarks.
    inverse = RBFInterpolator(target, SRC, kernel="thin_plate_spline", degree=1)
    h, w = rgb.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    query = np.column_stack((xx.ravel(), yy.ravel())).astype(np.float64)
    mapped = inverse(query)
    map_x = mapped[:, 0].reshape(h, w).astype(np.float32)
    map_y = mapped[:, 1].reshape(h, w).astype(np.float32)
    return cv2.remap(rgb, map_x, map_y, cv2.INTER_LANCZOS4,
                     borderMode=cv2.BORDER_REFLECT_101)


def validation(images: list[Image.Image], ts: list[float]) -> Image.Image:
    sheet = Image.new("RGB", (1112, 834), (20, 20, 20))
    for panel, (image, t) in enumerate(zip(images, ts)):
        view = image.copy()
        draw = ImageDraw.Draw(view)
        for i, name in enumerate(NAMES):
            start = SRC[i]
            end = DST[i]
            point = start + (end - start) * t
            color = COLORS[i]
            draw.line((tuple(start), tuple(end)), fill=color, width=2)
            x, y = np.rint(point).astype(int)
            draw.ellipse((x - 6, y - 6, x + 6, y + 6), fill=color,
                         outline="white", width=2)
        draw.rectangle((8, 8, 175, 38), fill=(15, 15, 15))
        draw.text((15, 15), f"t={t:.3f}", fill="white")
        view = view.resize((556, 417), Image.Resampling.LANCZOS)
        sheet.paste(view, ((panel % 2) * 556, (panel // 2) * 417))
    return sheet


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    images = [Image.open(p).convert("RGB") for p in paths]
    if len(images) != 44:
        raise RuntimeError(f"Expected 44 frames, got {len(images)}")
    base = np.asarray(images[28])
    mid1 = Image.fromarray(warp_at(base, 1 / 3), "RGB")
    mid2 = Image.fromarray(warp_at(base, 2 / 3), "RGB")
    sequence = images[:29] + [mid1, mid2] + images[29:]

    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    for i, image in enumerate(sequence, 1):
        image.save(FRAMES / f"frame_{i:03d}.png", optimize=True)
    mid1.save(OUT / "transition_1_t0333.png", optimize=True)
    mid2.save(OUT / "transition_2_t0667.png", optimize=True)
    validation([images[28], mid1, mid2, images[29]],
               [0.0, 1 / 3, 2 / 3, 1.0]).save(OUT / "landmark_validation.png")

    sequence[0].save(GIF, save_all=True, append_images=sequence[1:],
                     duration=round(1000 / FPS), loop=0, disposal=2,
                     optimize=False)
    w, h = sequence[0].size
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"),
                             FPS, (w, h))
    for image in sequence:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    report = {"fps": FPS, "frames": len(sequence), "insert_after": 29,
              "method": "thin-plate RBF landmark warp of original frame 29",
              "landmarks": {}}
    for i, name in enumerate(NAMES[:4]):
        points = [SRC[i] + (DST[i] - SRC[i]) * t for t in (0, 1/3, 2/3, 1)]
        distances = [float(np.linalg.norm(points[j + 1] - points[j])) for j in range(3)]
        report["landmarks"][name] = {
            "points": [[round(float(v), 3) for v in p] for p in points],
            "step_distances_px": distances,
            "max_step_difference_px": max(distances) - min(distances),
        }
    (OUT / "landmark_report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
