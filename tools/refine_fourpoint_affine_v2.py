from pathlib import Path
import csv

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
V1 = ROOT / "attack_sword_v2_fourpoint_affine39_v1"
OUT = ROOT / "attack_sword_v2_fourpoint_affine39_v2"
FRAMES = OUT / "frames"
CANVAS = (1400, 1320)


def warp_rgba(image, matrix):
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    alpha = rgba[..., 3:4]
    premul = np.concatenate((rgba[..., :3] * alpha, alpha), axis=2)
    warped = cv2.warpAffine(
        premul, matrix.astype(np.float32), CANVAS,
        flags=cv2.INTER_LANCZOS4,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=(0, 0, 0, 0),
    )
    wa = warped[..., 3:4]
    rgb = np.divide(warped[..., :3], wa, out=np.zeros_like(warped[..., :3]), where=wa > 1e-5)
    result = np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), np.clip(wa, 0, 1)), axis=2) * 255))
    result[:2, :, :] = 0
    result[-2:, :, :] = 0
    result[:, :2, :] = 0
    result[:, -2:, :] = 0
    return Image.fromarray(result, "RGBA")


def original_affine(source, target):
    rows, values = [], []
    for (x, y), (u, v) in zip(source, target):
        rows.extend(([x, y, 1, 0, 0, 0], [0, 0, 0, x, y, 1]))
        values.extend((u, v))
    params, *_ = np.linalg.lstsq(np.asarray(rows), np.asarray(values), rcond=None)
    return params.reshape(2, 3)


def transform_points(points, matrix):
    return np.c_[points, np.ones(len(points))] @ matrix.T


def correction_for_03(points, canonical):
    foot = points[2:4].mean(axis=0)
    target_foot = canonical[2:4].mean(axis=0)
    # Undo the 1.176 horizontal stretch while retaining a small amount needed
    # to make frame 03's stance agree with adjacent frames.
    sx = 0.92
    head_gap = foot[1] - points[0, 1]
    target_gap = target_foot[1] - canonical[0, 1]
    sy = target_gap / head_gap
    return np.array([
        [sx, 0.0, target_foot[0] - sx * foot[0]],
        [0.0, sy, target_foot[1] - sy * foot[1]],
    ], dtype=np.float32)


def upright_correction(points, canonical, angle_degrees):
    foot = points[2:4].mean(axis=0)
    target_foot = canonical[2:4].mean(axis=0)
    # Counter the unwanted affine tilt with a foot-line-anchored x shear.
    # At y == foot_y, x is unchanged, so the already-correct feet stay put.
    shear = np.tan(np.deg2rad(angle_degrees))
    head_gap = foot[1] - points[0, 1]
    target_gap = target_foot[1] - canonical[0, 1]
    sy = target_gap / head_gap
    tx = target_foot[0] - foot[0] - shear * (foot[1] - foot[1])
    ty = target_foot[1] - sy * foot[1]
    return np.array([
        [1.0, shear, tx - shear * foot[1]],
        [0.0, sy, ty],
    ], dtype=np.float32)


def checker(size, cell=18):
    w, h = size
    yy, xx = np.indices((h, w))
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[((xx // cell + yy // cell) % 2).astype(np.uint8)], "RGB")


def composite(image):
    bg = checker(CANVAS)
    bg.paste(image, mask=image.getchannel("A"))
    return bg


def contact(images, points):
    tw, th, cols = 280, 264, 8
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGB", (tw * cols, th * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    colors = [(255, 80, 80), (255, 220, 60), (70, 210, 255), (110, 255, 120)]
    for i, (image, anchors) in enumerate(zip(images, points), 1):
        tile = composite(image).resize((tw, th), Image.Resampling.LANCZOS)
        x, y = ((i - 1) % cols) * tw, ((i - 1) // cols) * th
        sheet.paste(tile, (x, y))
        draw.rectangle((x + 6, y + 6, x + 45, y + 31), fill=(12, 14, 18))
        draw.text((x + 12, y + 10), f"{i:02d}", fill=(255, 222, 105))
        for p, color in zip(anchors, colors):
            px, py = x + p[0] * tw / CANVAS[0], y + p[1] * th / CANVAS[1]
            draw.ellipse((px - 5, py - 5, px + 5, py + 5), fill=color, outline=(0, 0, 0), width=2)
    sheet.save(OUT / "refined_contact_sheet.png", optimize=True)


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    canonical = np.loadtxt(V1 / "canonical_points.txt", delimiter=",", skiprows=1)
    rows = list(csv.DictReader(open(V1 / "detected_points.csv", encoding="utf-8")))
    names = ("head", "crotch", "left_foot", "right_foot")
    source_points = [np.array([[float(r[f"{n}_x"]), float(r[f"{n}_y"])] for n in names]) for r in rows]
    v1_points = []
    for points in source_points:
        v1_points.append(transform_points(points, original_affine(points, canonical)))

    images = [Image.open(V1 / "frames" / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 40)]
    corrections = {i: np.array([[1, 0, 0], [0, 1, 0]], dtype=np.float32) for i in range(1, 40)}
    corrections[3] = correction_for_03(v1_points[2], canonical)
    angles = {9: 6.99, 10: 7.51, 11: 8.08, 13: 7.60, 14: 7.67}
    for frame, angle in angles.items():
        corrections[frame] = upright_correction(v1_points[frame - 1], canonical, angle)

    refined, refined_points = [], []
    log = ["frame,operation"]
    for i, (image, points) in enumerate(zip(images, v1_points), 1):
        result = warp_rgba(image, corrections[i]) if i in ({3} | set(angles)) else image.copy()
        anchors = transform_points(points, corrections[i])
        refined.append(result)
        refined_points.append(anchors)
        op = "frame03_unstretch" if i == 3 else ("foot_locked_upright" if i in angles else "unchanged")
        log.append(f"{i:02d},{op}")

    # Requested timeline correction: exchange frame 31 and frame 32, including anchor metadata.
    refined[30], refined[31] = refined[31], refined[30]
    refined_points[30], refined_points[31] = refined_points[31], refined_points[30]
    log[31] = "31,swapped_from_32"
    log[32] = "32,swapped_from_31"

    for i, image in enumerate(refined, 1):
        image.save(FRAMES / f"attack_sword_{i:02d}.png", optimize=True)
    contact(refined, refined_points)

    previews = [composite(image).resize((700, 660), Image.Resampling.LANCZOS) for image in refined]
    for i, preview in enumerate(previews):
        color = ((i * 67) % 256, (i * 131) % 256, (i * 197) % 256)
        ImageDraw.Draw(preview).rectangle((696, 656, 699, 659), fill=color)
    previews[0].save(
        OUT / "attack_sword_refined39_30fps.gif", save_all=True,
        append_images=previews[1:], duration=[33] * 39, loop=0,
        disposal=2, optimize=False,
    )
    (OUT / "changes.csv").write_text("\n".join(log) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
