from pathlib import Path
import csv
import importlib.util

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_aligned39_v1" / "frames"
V1 = ROOT / "attack_sword_v2_fourpoint_affine39_v1"
OUT = ROOT / "attack_sword_v2_fourpoint_affine39_v3"
FRAMES = OUT / "frames"

spec = importlib.util.spec_from_file_location(
    "fourpoint", r"D:\StSmod\DoubleSS\tools\four_point_affine_attack.py"
)
fourpoint = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fourpoint)


def affine(source, target):
    rows, values = [], []
    for (x, y), (u, v) in zip(source, target):
        rows.extend(([x, y, 1, 0, 0, 0], [0, 0, 0, x, y, 1]))
        values.extend((u, v))
    params, *_ = np.linalg.lstsq(np.asarray(rows), np.asarray(values), rcond=None)
    return params.reshape(2, 3)


def mapped(points, matrix):
    return np.c_[points, np.ones(len(points))] @ matrix.T


def axis_aligned_matrix(points, old_matrix, old_mapped, frame):
    linear = old_matrix[:, :2]
    # Preserve the measured length of each transformed basis vector, but make
    # them exactly horizontal and vertical. This removes shear/rotation using
    # the real per-frame matrix rather than a guessed visual angle.
    sx = float(np.linalg.norm(linear[:, 0]))
    sy = float(np.linalg.norm(linear[:, 1]))
    if frame == 3:
        # Frame 03's x basis was the outlier (1.176). Use its y scale so the
        # character retains natural proportions instead of becoming wider.
        sx = sy
    target_linear = np.array([[sx, 0.0], [0.0, sy]], dtype=np.float64)
    source_foot = points[2:4].mean(axis=0)
    desired_foot = old_mapped[2:4].mean(axis=0)
    translation = desired_foot - target_linear @ source_foot
    return np.c_[target_linear, translation]


def checker(size=(1400, 1320), cell=18):
    w, h = size
    yy, xx = np.indices((h, w))
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[((xx // cell + yy // cell) % 2).astype(np.uint8)], "RGB")


def composite(image):
    bg = checker()
    bg.paste(image, mask=image.getchannel("A"))
    return bg


def contact(images, point_sets):
    tw, th, cols = 280, 264, 8
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGB", (tw * cols, th * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    colors = [(255, 80, 80), (255, 220, 60), (70, 210, 255), (110, 255, 120)]
    for i, (image, anchors) in enumerate(zip(images, point_sets), 1):
        tile = composite(image).resize((tw, th), Image.Resampling.LANCZOS)
        x, y = ((i - 1) % cols) * tw, ((i - 1) // cols) * th
        sheet.paste(tile, (x, y))
        draw.rectangle((x + 6, y + 6, x + 45, y + 31), fill=(12, 14, 18))
        draw.text((x + 12, y + 10), f"{i:02d}", fill=(255, 222, 105))
        for p, color in zip(anchors, colors):
            px, py = x + p[0] * tw / 1400, y + p[1] * th / 1320
            draw.ellipse((px - 5, py - 5, px + 5, py + 5), fill=color, outline=(0, 0, 0), width=2)
    sheet.save(OUT / "orthogonalized_contact_sheet.png", optimize=True)


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    canonical = np.loadtxt(V1 / "canonical_points.txt", delimiter=",", skiprows=1)
    rows = list(csv.DictReader(open(V1 / "detected_points.csv", encoding="utf-8")))
    names = ("head", "crotch", "left_foot", "right_foot")
    points = [np.array([[float(r[f"{n}_x"]), float(r[f"{n}_y"])] for n in names]) for r in rows]
    selected = {3, 9, 10, 11, 13, 14}
    images, anchors = [], []
    logs = ["frame,old_basis_x_angle,old_basis_y_angle,new_sx,new_sy"]

    for frame in range(1, 40):
        src_points = points[frame - 1]
        old = affine(src_points, canonical)
        old_points = mapped(src_points, old)
        source_image = Image.open(SOURCE / f"attack_sword_{frame:02d}.png").convert("RGBA")
        if frame in selected:
            matrix = axis_aligned_matrix(src_points, old, old_points, frame)
            image = fourpoint.warp_rgba(source_image, matrix)
            out_points = mapped(src_points, matrix)
            bx = np.degrees(np.arctan2(old[1, 0], old[0, 0]))
            by = np.degrees(np.arctan2(old[1, 1], old[0, 1]))
            logs.append(f"{frame:02d},{bx:.3f},{by:.3f},{matrix[0,0]:.5f},{matrix[1,1]:.5f}")
        else:
            image = Image.open(V1 / "frames" / f"attack_sword_{frame:02d}.png").convert("RGBA")
            out_points = old_points
        images.append(image)
        anchors.append(out_points)

    images[30], images[31] = images[31], images[30]
    anchors[30], anchors[31] = anchors[31], anchors[30]

    for i, image in enumerate(images, 1):
        image.save(FRAMES / f"attack_sword_{i:02d}.png", optimize=True)
    contact(images, anchors)

    previews = [composite(image).resize((700, 660), Image.Resampling.LANCZOS) for image in images]
    for i, preview in enumerate(previews):
        ImageDraw.Draw(preview).rectangle(
            (696, 656, 699, 659),
            fill=((i * 67) % 256, (i * 131) % 256, (i * 197) % 256),
        )
    previews[0].save(
        OUT / "attack_sword_orthogonalized39_30fps.gif",
        save_all=True, append_images=previews[1:], duration=[33] * 39,
        loop=0, disposal=2, optimize=False,
    )
    (OUT / "matrix_changes.csv").write_text("\n".join(logs) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
