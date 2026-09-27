from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v6_no_outer_halo" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v7_outer_ring_removed"
FRAMES = OUT / "alpha_frames"
COUNT = 180


def remove_halo(path: Path):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy()
    alpha = rgba[:, :, 3].astype(np.float32)
    solid = alpha >= 128

    transparent = (~solid).astype(np.uint8)
    flood = transparent.copy()
    flood_mask = np.zeros((flood.shape[0] + 2, flood.shape[1] + 2), np.uint8)
    cv2.floodFill(flood, flood_mask, (0, 0), 2)
    exterior = flood == 2
    distance_to_exterior = cv2.distanceTransform((~exterior).astype(np.uint8), cv2.DIST_L2, 5)

    # V7 deliberately deletes the contaminated outermost subject ring instead
    # of merely fading it. Rebuild antialiasing from the clean second layer.
    outer_fringe = exterior & (alpha > 0)
    polluted_ring = solid & (distance_to_exterior <= 1.35)
    new_aa_ring = solid & (distance_to_exterior > 1.35) & (distance_to_exterior <= 2.75)
    contour = outer_fringe | polluted_ring | new_aa_ring
    alpha[outer_fringe | polluted_ring] = 0.0
    rebuilt = np.clip((distance_to_exterior - 1.35) / 1.40, 0.0, 1.0) * 255.0
    alpha[new_aa_ring] = np.minimum(alpha[new_aa_ring], rebuilt[new_aa_ring])
    alpha[alpha < 4.0] = 0.0
    alpha[alpha > 252.0] = 255.0

    # Replace only the surviving outer RGB with a donor at least four pixels
    # inside. Linear atlas filtering then sees the true local body color rather
    # than the bright/pink edge mixture baked into the video.
    inward = cv2.distanceTransform(solid.astype(np.uint8), cv2.DIST_L2, 5)
    donor = solid & (inward >= 4.0) & (rgba[:, :, 3] >= 245)
    _, nearest = distance_transform_edt(~donor, return_indices=True)
    recolor = new_aa_ring & (alpha > 0)
    rgba[recolor, :3] = rgba[nearest[0], nearest[1], :3][recolor]
    rgba[:, :, 3] = np.rint(alpha).astype(np.uint8)
    rgba[rgba[:, :, 3] == 0, :3] = 0
    return Image.fromarray(rgba, "RGBA"), int(np.count_nonzero(contour)), int(np.count_nonzero(recolor))


def composite(frame, color):
    return Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)


def make_game_scale_contact(before_paths, after_frames, picks):
    cols = 4
    cell_w, cell_h = 556, 455
    sheet = Image.new("RGB", (cols * cell_w, ((len(picks)+cols-1)//cols) * cell_h), (24, 20, 22))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        before = Image.open(before_paths[index]).convert("RGBA")
        # Approximate the in-game character footprint, on a dark warm dungeon
        # background that makes bright halos easy to spot.
        bg = (36, 25, 25, 255)
        left = composite(before, bg).resize((278, 209), Image.Resampling.LANCZOS).convert("RGB")
        right = composite(after_frames[index], bg).resize((278, 209), Image.Resampling.LANCZOS).convert("RGB")
        x, y = (n % cols) * cell_w, (n // cols) * cell_h
        sheet.paste(left, (x, y + 30))
        sheet.paste(right, (x + 278, y + 30))
        draw.text((x + 5, y + 7), f"frame {index+1:03d}: V6 left / ring-removed V7 right", fill="white")
    sheet.save(OUT / "idle_game_scale_halo_before_after.png", optimize=True)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    if len(paths) != COUNT:
        raise RuntimeError(f"Expected {COUNT} frames, found {len(paths)}")

    frames, contour_counts, recolor_counts = [], [], []
    for i, path in enumerate(paths, 1):
        frame, contour_count, recolor_count = remove_halo(path)
        frame.save(FRAMES / f"idle_{i:03d}.png", optimize=True)
        frames.append(frame)
        contour_counts.append(contour_count)
        recolor_counts.append(recolor_count)

    make_game_scale_contact(paths, frames, [0, 30, 60, 90, 105, 119, 149, 179])
    for name, color in (("game_dark", (36,25,25,255)), ("black", (0,0,0,255)),
                        ("gray", (45,45,45,255)), ("green", (0,255,0,255))):
        views = [composite(f, color).resize((533,400), Image.Resampling.LANCZOS).convert("RGB") for f in frames]
        views[0].save(OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)

    report = {
        "frames": COUNT,
        "contour_pixels_adjusted_total": int(sum(contour_counts)),
        "rgb_pixels_recolored_total": int(sum(recolor_counts)),
        "scope": "external silhouette only",
        "internal_alpha_untouched": True,
    }
    (OUT / "no_outer_halo_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
