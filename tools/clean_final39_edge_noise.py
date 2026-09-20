from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_fourpoint_affine39_v4" / "frames"
OUT = ROOT / "attack_sword_v2_final39_edgeclean_v5"
FRAMES = OUT / "frames"
CANVAS = (1400, 1320)


def clean_frame(image: Image.Image):
    rgba = np.asarray(image.convert("RGBA")).copy()
    alpha = rgba[..., 3]

    # Remove near-transparent residue, then discard only genuinely tiny islands.
    alpha[alpha < 12] = 0
    mask = alpha > 0
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask.astype(np.uint8), 8)
    removed_components = 0
    removed_pixels = 0
    for index in range(1, count):
        area = int(stats[index, cv2.CC_STAT_AREA])
        if area < 28:
            selection = labels == index
            removed_pixels += int(selection.sum())
            removed_components += 1
            alpha[selection] = 0

    foreground = alpha > 0
    # Build an uncontaminated interior reference. A 5x5 erosion removes the
    # colored fringe while retaining cores even in the sword and hair strands.
    core = cv2.erode((alpha >= 96).astype(np.uint8), np.ones((5, 5), np.uint8), iterations=1) > 0
    if not np.any(core):
        core = alpha >= 160

    inside_distance = cv2.distanceTransform(foreground.astype(np.uint8), cv2.DIST_L2, 5)
    edge_band = foreground & (inside_distance <= 4.25)
    # nearest zero of ~core == nearest true pixel in core
    _, nearest = distance_transform_edt(~core, return_indices=True)
    nearest_y, nearest_x = nearest
    source_rgb = rgba[..., :3].copy()
    rgba[edge_band, :3] = source_rgb[nearest_y[edge_band], nearest_x[edge_band], :]
    rgba[..., 3] = alpha
    rgba[alpha == 0, :3] = 0
    return Image.fromarray(rgba, "RGBA"), {
        "removed_components": removed_components,
        "removed_pixels": removed_pixels,
        "recolored_edge_pixels": int(edge_band.sum()),
    }


def checker(size, cell=18):
    w, h = size
    yy, xx = np.indices((h, w))
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[((xx // cell + yy // cell) % 2).astype(np.uint8)], "RGB")


def composite(image):
    bg = checker(CANVAS)
    bg.paste(image, mask=image.getchannel("A"))
    return bg


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    cleaned = []
    stats = []
    for frame in range(1, 40):
        source = Image.open(SOURCE / f"attack_sword_{frame:02d}.png").convert("RGBA")
        result, record = clean_frame(source)
        result.save(FRAMES / f"attack_sword_{frame:02d}.png", optimize=True)
        record["frame"] = frame
        stats.append(record)
        cleaned.append(result)

    previews = [composite(image).resize((700, 660), Image.Resampling.LANCZOS) for image in cleaned]
    for i, preview in enumerate(previews):
        ImageDraw.Draw(preview).rectangle(
            (696, 656, 699, 659),
            fill=((i * 67) % 256, (i * 131) % 256, (i * 197) % 256),
        )
    previews[0].save(
        OUT / "attack_sword_edgeclean39_40fps.gif", save_all=True,
        append_images=previews[1:], duration=[25] * 39, loop=0,
        disposal=2, optimize=False,
    )

    tw, th, cols = 280, 264, 8
    rows = 5
    sheet = Image.new("RGB", (tw * cols, th * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    for i, image in enumerate(previews, 1):
        x, y = ((i - 1) % cols) * tw, ((i - 1) // cols) * th
        sheet.paste(image.resize((tw, th), Image.Resampling.LANCZOS), (x, y))
        draw.rectangle((x + 6, y + 6, x + 45, y + 31), fill=(12, 14, 18))
        draw.text((x + 12, y + 10), f"{i:02d}", fill=(255, 222, 105))
    sheet.save(OUT / "edgeclean_contact_sheet.png", optimize=True)
    (OUT / "cleanup_stats.json").write_text(
        json.dumps(stats, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()
