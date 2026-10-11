from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_cast_s")
SOURCE = ROOT / "cast_s_v5_alpha_clean_45f_60fps" / "alpha_frames_45f_60fps"
OUT = ROOT / "cast_s_v6_hair_edge_45f_60fps"
FRAMES = OUT / "alpha_frames_45f_60fps"


def clean_hair(src: np.ndarray) -> tuple[np.ndarray, dict]:
    out = src.copy()
    # The hair and its flyaway strands remain in this conservative box in all
    # 45 frames. Do not touch the glove, white suit, sword or fire.
    x0, y0, x1, y1 = 270, 175, 570, 370
    roi = out[y0:y1, x0:x1]
    rgb = roi[:, :, :3].astype(np.int16)
    alpha = roi[:, :, 3]
    opaque = alpha > 0
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    yy, xx = np.indices(alpha.shape)
    gx, gy = xx + x0, yy + y0
    hair_zone = ((gy < 305) | ((gx < 415) & (gy < 360)))
    hair_core = opaque & (r > g + 9) & (g > b + 13) & (r > 75) & hair_zone
    near_hair = cv2.dilate(hair_core.astype(np.uint8), np.ones((7, 7), np.uint8)) > 0
    target = hair_zone & near_hair
    purple = (b > g + 7) & (r > g + 7)
    # A one-pixel shrink strips the old pale/purple key outline. The hair's
    # single-pixel wisps remain if their color is actually warm blond.
    inner = cv2.erode(opaque.astype(np.uint8), np.ones((3, 3), np.uint8)) > 0
    edge = opaque & ~inner
    warm_hair = (r > b + 13) & (g >= b + 8) & (r >= g - 12)
    pale = (r > 180) & (g > 165) & (b > 120) & ((r - b) < 82)
    reject = edge & target & (purple | pale | ~warm_hair)
    alpha[reject] = 0

    # Replace contaminated RGB on the surviving antialiased edge with the
    # nearest interior color; this prevents a white/purple halo on dark game
    # backgrounds without shifting the visible hair silhouette wholesale.
    valid = inner & warm_hair & hair_zone & (alpha > 220)
    if np.any(valid):
        _, labels = cv2.distanceTransformWithLabels((~valid).astype(np.uint8), cv2.DIST_L2, 3,
                                                    labelType=cv2.DIST_LABEL_PIXEL)
        ys, xs = np.where(valid)
        # OpenCV labels the zero pixels in row-major order, starting at 1.
        nearest = np.clip(labels.astype(np.int64) - 1, 0, len(ys) - 1)
        band = cv2.dilate(edge.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
        contaminated = band & target & (alpha > 0) & (purple | pale | ((b > g - 4) & (r < g + 15)))
        donor = roi[ys[nearest[contaminated]], xs[nearest[contaminated]], :3]
        roi[:, :, :3][contaminated] = donor
    else:
        contaminated = np.zeros_like(edge)

    count, component, stats, _ = cv2.connectedComponentsWithStats((alpha > 0).astype(np.uint8), 8)
    for label in range(1, count):
        if stats[label, cv2.CC_STAT_AREA] <= 3:
            alpha[(component == label) & hair_zone] = 0

    roi[:, :, :3][alpha == 0] = 0
    return out, {"removed_fringe_pixels": int(reject.sum()), "recolored_edge_pixels": int(contaminated.sum())}


def comparison(before: np.ndarray, after: np.ndarray, path: Path) -> None:
    box = (285, 180, 565, 375)
    tiles = []
    for arr in (before, after):
        im = Image.fromarray(arr, "RGBA")
        crop = im.crop(box)
        bg = Image.new("RGBA", crop.size, (34, 34, 34, 255))
        tiles.append(Image.alpha_composite(bg, crop).convert("RGB").resize((1120, 780), Image.Resampling.NEAREST))
    sheet = Image.new("RGB", (2240, 820), (24, 24, 24))
    draw = ImageDraw.Draw(sheet)
    draw.text((10, 8), "BEFORE v5", fill="white")
    draw.text((1130, 8), "AFTER v6", fill="white")
    sheet.paste(tiles[0], (0, 40))
    sheet.paste(tiles[1], (1120, 40))
    sheet.save(path)


def main() -> None:
    paths = sorted(SOURCE.glob("cast_s_alpha_*.png"))
    if len(paths) != 45:
        raise RuntimeError(f"Expected 45 frames; found {len(paths)}")
    FRAMES.mkdir(parents=True, exist_ok=True)
    report = []
    images = []
    for no, path in enumerate(paths, 1):
        src = np.array(Image.open(path).convert("RGBA"))
        dst, stats = clean_hair(src)
        out_path = FRAMES / path.name
        Image.fromarray(dst, "RGBA").save(out_path, optimize=True)
        images.append(Image.fromarray(dst, "RGBA"))
        report.append({"frame": no, **stats})
        if no in (1, 15, 29, 45):
            comparison(src, dst, OUT / f"hair_compare_{no:03d}.png")
    thumbs = [im.resize((640, 480), Image.Resampling.LANCZOS) for im in images]
    thumbs[0].save(OUT / "cast_s_alpha_hair_clean_45f_60fps.gif", save_all=True,
                   append_images=thumbs[1:], duration=17, loop=0, disposal=2)
    (OUT / "cleanup_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"frames": len(images), "removed": sum(x["removed_fringe_pixels"] for x in report),
                      "recolored": sum(x["recolored_edge_pixels"] for x in report)}))


if __name__ == "__main__":
    main()
