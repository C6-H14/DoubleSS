from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_aligned39_v1" / "frames"
WORK = ROOT / "attack_sword_v2_fourpoint_affine39_v1"
MARKERS = WORK / "markers_before"
FRAMES = WORK / "frames"
CANVAS = (1400, 1320)


def component_stats(mask):
    n, labels, stats, centroids = cv2.connectedComponentsWithStats(np.uint8(mask) * 255)
    return labels, [(i, *stats[i], *centroids[i]) for i in range(1, n)]


def detect_points(image):
    rgba = np.asarray(image.convert("RGBA"))
    r, g, b, a = [rgba[..., i].astype(np.int32) for i in range(4)]
    opaque = a >= 32

    green = opaque & (r < 100) & (g < 130) & (b < 145) & (g > r * 0.9)
    green_labels, green_components = component_stats(green)
    torso = max(green_components, key=lambda item: item[5])
    _, tx, ty, tw, th, _, tcx, tcy = torso

    skin = opaque & (r > 205) & (g > 145) & (b > 125) & ((r - g) > 15) & ((g - b) < 55)
    skin_labels, skin_components = component_stats(skin)
    face_candidates = [item for item in skin_components
                       if 900 <= item[5] <= 7000 and item[2] < ty + 110 and abs(item[6] - tcx) < 190]
    if not face_candidates:
        face_candidates = [item for item in skin_components if item[2] < ty + 110 and item[5] > 300]
    face = min(face_candidates, key=lambda item: abs(item[6] - tcx) + abs(item[7] - (ty - 25)))
    head = np.array([face[6], face[7]], dtype=np.float32)

    leg_candidates = [item for item in skin_components if item[5] > 1800 and item[7] > ty + th * 0.55]
    leg_candidates = sorted(leg_candidates, key=lambda item: item[5], reverse=True)[:2]
    if len(leg_candidates) == 2:
        leg_candidates.sort(key=lambda item: item[6])
        left, right = leg_candidates
        ly, ry = left[2], right[2]
        band_y0 = max(ly, ry)
        band_y1 = band_y0 + 35
        left_pixels = np.where((skin_labels == left[0]) & (np.indices(skin.shape)[0] >= band_y0) &
                               (np.indices(skin.shape)[0] <= band_y1))
        right_pixels = np.where((skin_labels == right[0]) & (np.indices(skin.shape)[0] >= band_y0) &
                                (np.indices(skin.shape)[0] <= band_y1))
        lx = float(np.max(left_pixels[1])) if len(left_pixels[1]) else left[6]
        rx = float(np.min(right_pixels[1])) if len(right_pixels[1]) else right[6]
        crotch = np.array([(lx + rx) / 2, band_y0 + 8], dtype=np.float32)
    else:
        crotch = np.array([tcx, ty + th * 0.78], dtype=np.float32)

    ys, xs = np.where(opaque)
    bottom = int(ys.max())
    foot_y0 = bottom - 65
    fy, fx = np.where(opaque & (np.indices(opaque.shape)[0] >= foot_y0))
    samples = fx.astype(np.float32).reshape(-1, 1)
    _, groups, centers = cv2.kmeans(samples, 2, None,
                                    (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 50, 0.1),
                                    10, cv2.KMEANS_PP_CENTERS)
    order = np.argsort(centers[:, 0])
    feet = []
    for cluster in order:
        selection = groups[:, 0] == cluster
        feet.append(np.array([float(np.mean(fx[selection])), float(np.max(fy[selection]))], dtype=np.float32))
    return np.stack([head, crotch, feet[0], feet[1]])


def affine_least_squares(source, target):
    rows, values = [], []
    for (x, y), (u, v) in zip(source, target):
        rows.append([x, y, 1, 0, 0, 0]); values.append(u)
        rows.append([0, 0, 0, x, y, 1]); values.append(v)
    params, *_ = np.linalg.lstsq(np.asarray(rows), np.asarray(values), rcond=None)
    return params.reshape(2, 3).astype(np.float32)


def warp_rgba(image, matrix):
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    alpha = rgba[..., 3:4]
    premul = np.concatenate((rgba[..., :3] * alpha, alpha), axis=2)
    warped = cv2.warpAffine(premul, matrix, CANVAS, flags=cv2.INTER_LANCZOS4,
                            borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))
    wa = warped[..., 3:4]
    rgb = np.divide(warped[..., :3], wa, out=np.zeros_like(warped[..., :3]), where=wa > 1e-5)
    result = np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), np.clip(wa, 0, 1)), axis=2) * 255))
    # Lanczos can leave 1-alpha ringing on the outermost canvas pixel.
    result[:2, :, :] = 0
    result[-2:, :, :] = 0
    result[:, :2, :] = 0
    result[:, -2:, :] = 0
    return Image.fromarray(result, "RGBA")


def checker(size, cell=18):
    w, h = size
    yy, xx = np.indices((h, w))
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[((xx // cell + yy // cell) % 2).astype(np.uint8)], "RGB")


def composite(frame):
    bg = checker(CANVAS)
    bg.paste(frame, mask=frame.getchannel("A"))
    return bg


def contact(images, filename, point_sets=None):
    tw, th, cols = 280, 264, 8
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGB", (tw * cols, th * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    colors = [(255, 80, 80), (255, 220, 60), (70, 210, 255), (110, 255, 120)]
    for i, image in enumerate(images, 1):
        tile = composite(image).resize((tw, th), Image.Resampling.LANCZOS)
        x, y = ((i - 1) % cols) * tw, ((i - 1) // cols) * th
        sheet.paste(tile, (x, y))
        draw.rectangle((x + 6, y + 6, x + 45, y + 31), fill=(12, 14, 18))
        draw.text((x + 12, y + 10), f"{i:02d}", fill=(255, 222, 105))
        if point_sets is not None:
            for point, color in zip(point_sets[i - 1], colors):
                px, py = x + point[0] * tw / CANVAS[0], y + point[1] * th / CANVAS[1]
                draw.ellipse((px - 5, py - 5, px + 5, py + 5), fill=color, outline=(0, 0, 0), width=2)
    sheet.save(WORK / filename, optimize=True)


def main():
    MARKERS.mkdir(parents=True, exist_ok=True)
    FRAMES.mkdir(parents=True, exist_ok=True)
    images = [Image.open(SOURCE / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 40)]
    points = [detect_points(image) for image in images]
    point_array = np.stack(points)
    canonical = np.median(point_array, axis=0).astype(np.float32)

    marker_colors = [(255, 80, 80, 255), (255, 220, 60, 255), (70, 210, 255, 255), (110, 255, 120, 255)]
    for i, (image, frame_points) in enumerate(zip(images, points), 1):
        marked = image.copy()
        draw = ImageDraw.Draw(marked)
        for (x, y), color in zip(frame_points, marker_colors):
            draw.ellipse((x - 14, y - 14, x + 14, y + 14), fill=color, outline=(0, 0, 0, 255), width=4)
        marked.save(MARKERS / f"attack_sword_{i:02d}_markers.png", optimize=True)
    contact(images, "markers_before_contact_sheet.png", points)

    transformed, transformed_points = [], []
    csv_lines = ["frame,head_x,head_y,crotch_x,crotch_y,left_foot_x,left_foot_y,right_foot_x,right_foot_y"]
    for i, (image, frame_points) in enumerate(zip(images, points), 1):
        matrix = affine_least_squares(frame_points, canonical)
        result = warp_rgba(image, matrix)
        transformed.append(result)
        homo = np.concatenate((frame_points, np.ones((4, 1), dtype=np.float32)), axis=1)
        mapped = homo @ matrix.T
        transformed_points.append(mapped)
        result.save(FRAMES / f"attack_sword_{i:02d}.png", optimize=True)
        flat = ",".join(f"{value:.2f}" for value in frame_points.flatten())
        csv_lines.append(f"{i:02d},{flat}")
    (WORK / "detected_points.csv").write_text("\n".join(csv_lines) + "\n", encoding="utf-8")
    (WORK / "canonical_points.txt").write_text(
        "head,crotch,left_foot,right_foot\n" + "\n".join(f"{p[0]:.2f},{p[1]:.2f}" for p in canonical) + "\n",
        encoding="utf-8")
    contact(transformed, "affine_after_contact_sheet.png", transformed_points)

    previews = [composite(image).resize((700, 660), Image.Resampling.LANCZOS) for image in transformed]
    # Encode a visually negligible frame id in the bottom-right pixel so GIF
    # writers do not coalesce deliberately duplicated hold frames.
    for i, preview in enumerate(previews):
        preview.putpixel((699, 659), (i, i, i))
    durations = [33] * len(previews)
    previews[0].save(WORK / "attack_sword_fourpoint_affine39_30fps.gif", save_all=True,
                     append_images=previews[1:], duration=durations, loop=0, disposal=2, optimize=False)


if __name__ == "__main__":
    main()
