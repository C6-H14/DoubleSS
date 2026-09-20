from pathlib import Path
import csv
import importlib.util

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_aligned39_v1" / "frames"
V1 = ROOT / "attack_sword_v2_fourpoint_affine39_v1"
V3 = ROOT / "attack_sword_v2_fourpoint_affine39_v3"
OUT = ROOT / "attack_sword_v2_fourpoint_affine39_v4"
FRAMES = OUT / "frames"
CANVAS = (1400, 1320)

spec = importlib.util.spec_from_file_location(
    "v3tools", r"D:\StSmod\DoubleSS\tools\orthogonalize_selected_frames_v3.py"
)
v3tools = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v3tools)


def tapered_width(image, center_x, full_until_y, fixed_from_y, factor):
    """Scale width above the legs while making the foot line an identity map."""
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    alpha = rgba[..., 3:4]
    premul = np.concatenate((rgba[..., :3] * alpha, alpha), axis=2)
    h, w = rgba.shape[:2]
    yy, xx = np.indices((h, w), dtype=np.float32)

    t = np.clip((yy - full_until_y) / max(1.0, fixed_from_y - full_until_y), 0.0, 1.0)
    smooth = t * t * (3.0 - 2.0 * t)
    scale = factor + (1.0 - factor) * smooth
    map_x = center_x + (xx - center_x) / scale
    map_y = yy
    warped = cv2.remap(
        premul, map_x, map_y, interpolation=cv2.INTER_LANCZOS4,
        borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0),
    )
    wa = warped[..., 3:4]
    rgb = np.divide(warped[..., :3], wa, out=np.zeros_like(warped[..., :3]), where=wa > 1e-5)
    result = np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), np.clip(wa, 0, 1)), axis=2) * 255))
    # The user explicitly requires the feet to remain untouched. Restore the
    # identity zone byte-for-byte, avoiding even interpolation color changes.
    identity_y = int(np.ceil(fixed_from_y))
    result[identity_y:, :, :] = np.asarray(image.convert("RGBA"), dtype=np.uint8)[identity_y:, :, :]
    result[:2, :, :] = result[-2:, :, :] = 0
    result[:, :2, :] = result[:, -2:, :] = 0
    return Image.fromarray(result, "RGBA")


def taper_points(points, center_x, full_until_y, fixed_from_y, factor):
    result = points.copy()
    t = np.clip((result[:, 1] - full_until_y) / max(1.0, fixed_from_y - full_until_y), 0.0, 1.0)
    smooth = t * t * (3.0 - 2.0 * t)
    scale = factor + (1.0 - factor) * smooth
    result[:, 0] = center_x + (result[:, 0] - center_x) * scale
    return result


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    canonical = np.loadtxt(V1 / "canonical_points.txt", delimiter=",", skiprows=1)
    rows = list(csv.DictReader(open(V1 / "detected_points.csv", encoding="utf-8")))
    names = ("head", "crotch", "left_foot", "right_foot")
    source_points = [np.array([[float(r[f"{n}_x"]), float(r[f"{n}_y"])] for n in names]) for r in rows]

    images = [Image.open(V3 / "frames" / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 40)]
    # Reconstruct the anchor positions used by v3. Frames 31/32 were exchanged there.
    anchors = []
    old_matrices = []
    for points in source_points:
        matrix = v3tools.affine(points, canonical)
        old_matrices.append(matrix)
        anchors.append(v3tools.mapped(points, matrix))
    for frame in (3, 9, 10, 11, 13, 14):
        matrix = v3tools.axis_aligned_matrix(
            source_points[frame - 1], old_matrices[frame - 1], anchors[frame - 1], frame
        )
        anchors[frame - 1] = v3tools.mapped(source_points[frame - 1], matrix)
    anchors[30], anchors[31] = anchors[31], anchors[30]

    # Frame 12: remove its measured shear/rotation, using its real basis lengths.
    frame = 12
    matrix12 = v3tools.axis_aligned_matrix(
        source_points[frame - 1], old_matrices[frame - 1], anchors[frame - 1], frame
    )
    source12 = Image.open(SOURCE / "attack_sword_12.png").convert("RGBA")
    images[11] = v3tools.fourpoint.warp_rgba(source12, matrix12)
    anchors[11] = v3tools.mapped(source_points[11], matrix12)

    # Width corrections preserve the complete foot zone. The deformation is
    # full above the crotch, eases through the legs, and becomes identity well
    # before the soles.
    factors = {15: 1.08, 16: 1.08, 17: 0.92}
    for frame, factor in factors.items():
        points = anchors[frame - 1]
        center_x = float(points[2:4, 0].mean())
        crotch_y = float(points[1, 1])
        foot_y = float(points[2:4, 1].mean())
        full_until = crotch_y + 25.0
        fixed_from = foot_y - 105.0
        images[frame - 1] = tapered_width(
            images[frame - 1], center_x, full_until, fixed_from, factor
        )
        anchors[frame - 1] = taper_points(
            points, center_x, full_until, fixed_from, factor
        )

    for i, image in enumerate(images, 1):
        image.save(FRAMES / f"attack_sword_{i:02d}.png", optimize=True)
    v3tools.OUT = OUT
    v3tools.contact(images, anchors)
    (OUT / "orthogonalized_contact_sheet.png").replace(OUT / "refined_contact_sheet.png")

    previews = [v3tools.composite(image).resize((700, 660), Image.Resampling.LANCZOS) for image in images]
    for i, preview in enumerate(previews):
        ImageDraw.Draw(preview).rectangle(
            (696, 656, 699, 659),
            fill=((i * 67) % 256, (i * 131) % 256, (i * 197) % 256),
        )
    previews[0].save(
        OUT / "attack_sword_refined39_30fps.gif", save_all=True,
        append_images=previews[1:], duration=[33] * 39, loop=0,
        disposal=2, optimize=False,
    )
    (OUT / "changes.txt").write_text(
        "12: orthogonalized from measured basis\n"
        "15: upper-body width x1.08, feet fixed\n"
        "16: upper-body width x1.08, feet fixed\n"
        "17: upper-body width x0.92, feet fixed\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
