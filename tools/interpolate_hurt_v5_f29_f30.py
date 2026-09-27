from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "hurt_v5_direct_cuts" / "hurt_v5_44f_frames_60fps"
OUT = ROOT / "art_preview" / "hurt_v6_f29_f30_two_inbetweens"
FRAMES = OUT / "hurt_v6_46f_frames_60fps"
GIF = OUT / "hurt_v6_46f_60fps.gif"
MP4 = OUT / "hurt_v6_46f_60fps.mp4"
FPS = 60

# Centers are marked on the visible feature, not on its bounding box. The
# covered-eye point is the anatomical eye center underneath the rigid mask.
LANDMARKS_29 = {
    "visible_eye": (552, 148),
    "covered_eye": (587, 149),
    "sword_hand": (599, 349),
    "abdomen_hand": (528, 342),
}
LANDMARKS_30 = {
    "visible_eye": (542, 141),
    "covered_eye": (580, 141),
    "sword_hand": (599, 331),
    "abdomen_hand": (530, 337),
}
COLORS = {
    "visible_eye": (255, 70, 70),
    "covered_eye": (255, 190, 40),
    "sword_hand": (60, 220, 255),
    "abdomen_hand": (80, 255, 100),
}


def flow_inbetween(a: np.ndarray, b: np.ndarray, t: float) -> np.ndarray:
    gray_a = cv2.cvtColor(a, cv2.COLOR_RGB2GRAY)
    gray_b = cv2.cvtColor(b, cv2.COLOR_RGB2GRAY)
    flow_ab = cv2.calcOpticalFlowFarneback(
        gray_a, gray_b, None, 0.5, 5, 25, 5, 7, 1.5, 0
    )
    flow_ba = cv2.calcOpticalFlowFarneback(
        gray_b, gray_a, None, 0.5, 5, 25, 5, 7, 1.5, 0
    )
    h, w = gray_a.shape
    grid_x, grid_y = np.meshgrid(np.arange(w), np.arange(h))
    map_ax = (grid_x - t * flow_ab[..., 0]).astype(np.float32)
    map_ay = (grid_y - t * flow_ab[..., 1]).astype(np.float32)
    map_bx = (grid_x - (1.0 - t) * flow_ba[..., 0]).astype(np.float32)
    map_by = (grid_y - (1.0 - t) * flow_ba[..., 1]).astype(np.float32)
    warped_a = cv2.remap(a, map_ax, map_ay, cv2.INTER_LANCZOS4,
                         borderMode=cv2.BORDER_REFLECT_101)
    warped_b = cv2.remap(b, map_bx, map_by, cv2.INTER_LANCZOS4,
                         borderMode=cv2.BORDER_REFLECT_101)
    return np.clip(warped_a * (1.0 - t) + warped_b * t, 0, 255).astype(np.uint8)


def lerp_point(a: tuple[int, int], b: tuple[int, int], t: float) -> tuple[int, int]:
    return (round(a[0] + (b[0] - a[0]) * t),
            round(a[1] + (b[1] - a[1]) * t))


def annotated(images: list[Image.Image], ts: list[float]) -> Image.Image:
    views = []
    for image, t in zip(images, ts):
        view = image.copy()
        draw = ImageDraw.Draw(view)
        for name in LANDMARKS_29:
            start = LANDMARKS_29[name]
            end = LANDMARKS_30[name]
            point = lerp_point(start, end, t)
            color = COLORS[name]
            draw.line((start, end), fill=color, width=2)
            x, y = point
            draw.ellipse((x - 6, y - 6, x + 6, y + 6), fill=color,
                         outline=(255, 255, 255), width=2)
        draw.rectangle((8, 8, 175, 38), fill=(15, 15, 15))
        draw.text((15, 15), f"t={t:.3f}", fill="white")
        views.append(view.resize((556, 417), Image.Resampling.LANCZOS))
    sheet = Image.new("RGB", (2 * 556, 2 * 417), (20, 20, 20))
    for i, view in enumerate(views):
        sheet.paste(view, ((i % 2) * 556, (i // 2) * 417))
    return sheet


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 44:
        raise RuntimeError(f"Expected 44 frames, got {len(paths)}")
    images = [Image.open(p).convert("RGB") for p in paths]
    a = np.asarray(images[28])
    b = np.asarray(images[29])
    between_1 = Image.fromarray(flow_inbetween(a, b, 1 / 3), "RGB")
    between_2 = Image.fromarray(flow_inbetween(a, b, 2 / 3), "RGB")
    sequence = images[:29] + [between_1, between_2] + images[29:]

    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    for i, image in enumerate(sequence, 1):
        image.save(FRAMES / f"frame_{i:03d}.png", optimize=True)
    between_1.save(OUT / "transition_1_t0333.png", optimize=True)
    between_2.save(OUT / "transition_2_t0667.png", optimize=True)
    annotated([images[28], between_1, between_2, images[29]],
              [0.0, 1 / 3, 2 / 3, 1.0]).save(OUT / "landmark_validation.png")

    sequence[0].save(
        GIF, save_all=True, append_images=sequence[1:],
        duration=round(1000 / FPS), loop=0, disposal=2, optimize=False,
    )
    width, height = sequence[0].size
    writer = cv2.VideoWriter(
        str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height)
    )
    if not writer.isOpened():
        raise RuntimeError("Failed to open MP4 writer")
    for image in sequence:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    report = {"fps": FPS, "frames": len(sequence),
              "duration": len(sequence) / FPS, "insert_after": 29,
              "fractions": [1 / 3, 2 / 3], "landmarks": {}}
    for name in LANDMARKS_29:
        points = [lerp_point(LANDMARKS_29[name], LANDMARKS_30[name], t)
                  for t in [0.0, 1 / 3, 2 / 3, 1.0]]
        distances = [float(np.linalg.norm(np.subtract(points[i + 1], points[i])))
                     for i in range(3)]
        report["landmarks"][name] = {
            "points": points,
            "step_distances_px": distances,
            "max_step_difference_px": max(distances) - min(distances),
        }
    (OUT / "landmark_report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
