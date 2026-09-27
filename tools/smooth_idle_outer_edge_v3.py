from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_alpha_v2_nearest_color" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_alpha_v4_smoother_edge"
FRAMES = OUT / "alpha_frames"
COUNT = 180


def smooth_outer_edge(path: Path):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy()
    rgb = rgba[:, :, :3].astype(np.float32)
    old_alpha = rgba[:, :, 3]

    # Use the stable half-alpha silhouette as geometry.  Only the outside
    # contour is processed; internal hair lines, face and clothing remain
    # pixel-identical.
    solid = old_alpha >= 128
    inward = cv2.distanceTransform(solid.astype(np.uint8), cv2.DIST_L2, 5)

    # Distinguish the true outer background from transparent holes enclosed by
    # hair, arms or clothing.  Only the component connected to the canvas edge
    # is allowed to participate in silhouette antialiasing.
    transparent = (~solid).astype(np.uint8)
    flood = transparent.copy()
    flood_mask = np.zeros((flood.shape[0] + 2, flood.shape[1] + 2), np.uint8)
    cv2.floodFill(flood, flood_mask, (0, 0), 2)
    exterior = flood == 2
    distance_to_exterior = cv2.distanceTransform((~exterior).astype(np.uint8), cv2.DIST_L2, 5)
    distance_to_subject = cv2.distanceTransform((~solid).astype(np.uint8), cv2.DIST_L2, 5)

    # A mild sub-pixel alpha ramp.  The 0.78 px Gaussian is narrow enough to
    # avoid a halo, but removes the one-pixel staircase visible at 1:1 scale.
    soft_alpha = cv2.GaussianBlur(
        (solid.astype(np.float32) * 255.0), (0, 0), sigmaX=1.05, sigmaY=1.05,
        borderType=cv2.BORDER_REPLICATE,
    )
    new_alpha = old_alpha.astype(np.float32)
    alpha_zone = ((solid & (distance_to_exterior <= 3.5))
                  | (exterior & (distance_to_subject <= 3.0)))
    new_alpha[alpha_zone] = soft_alpha[alpha_zone]
    new_alpha[new_alpha < 3.0] = 0.0
    new_alpha[new_alpha > 252.0] = 255.0

    # Average only colors already belonging to the character.  Background
    # pixels never enter the convolution, so pink cannot bleed back in.
    weight = (old_alpha.astype(np.float32) / 255.0)
    denom = cv2.GaussianBlur(weight, (0, 0), 1.05, borderType=cv2.BORDER_REPLICATE)
    averaged = np.zeros_like(rgb)
    for c in range(3):
        numerator = cv2.GaussianBlur(
            rgb[:, :, c] * weight, (0, 0), 1.05,
            borderType=cv2.BORDER_REPLICATE,
        )
        averaged[:, :, c] = numerator / np.maximum(denom, 1e-5)

    # Newly antialiased outside pixels need a valid subject color.  Borrow
    # from the nearest opaque subject texel, then blend the existing outer
    # 2.6 px toward the local weighted average with a smooth inward falloff.
    donor = solid & (inward >= 4.0) & (old_alpha >= 245)
    _, nearest = distance_transform_edt(~donor, return_indices=True)
    nearest_rgb = rgb[nearest[0], nearest[1]]
    new_pixels = exterior & (new_alpha > 0) & (old_alpha == 0)
    rgb[new_pixels] = nearest_rgb[new_pixels]
    averaged[new_pixels] = nearest_rgb[new_pixels]

    inner_ring = solid & (distance_to_exterior <= 4.0)
    blend = np.clip((4.0 - distance_to_exterior) / 4.0, 0.0, 1.0)
    # Keep some original texture even at the outermost pixel; this avoids a
    # waxy painted rim while suppressing blocky single-color steps.
    blend *= 0.86
    rgb[inner_ring] = (
        rgb[inner_ring] * (1.0 - blend[inner_ring, None])
        + averaged[inner_ring] * blend[inner_ring, None]
    )
    rgb[new_pixels] = nearest_rgb[new_pixels]

    out = np.empty_like(rgba)
    out[:, :, :3] = np.clip(np.rint(rgb), 0, 255).astype(np.uint8)
    out[:, :, 3] = np.clip(np.rint(new_alpha), 0, 255).astype(np.uint8)
    out[out[:, :, 3] == 0, :3] = 0

    alpha_changed = int(np.count_nonzero(old_alpha != out[:, :, 3]))
    rgb_changed = int(np.count_nonzero(np.any(rgba[:, :, :3] != out[:, :, :3], axis=2)))
    return Image.fromarray(out, "RGBA"), alpha_changed, rgb_changed


def composite(frame, color):
    return Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)


def make_contact(frames, name, color, picks):
    cols, cw, ch = 6, 240, 205
    sheet = Image.new("RGB", (cols * cw, 5 * ch), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        thumb = composite(frames[index], color).resize((240, 180), Image.Resampling.LANCZOS).convert("RGB")
        x, y = (n % cols) * cw, (n // cols) * ch
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 4, y + 4), f"{index + 1:03d}  {index/60:.2f}s", fill="white")
    sheet.save(OUT / f"idle_{name}_contact.jpg", quality=98)


def make_before_after(after_frames, picks):
    before_paths = sorted(SOURCE.glob("idle_*.png"))
    crop_box = (300, 45, 875, 520)
    scale = 2
    crop_w = (crop_box[2] - crop_box[0]) * scale
    crop_h = (crop_box[3] - crop_box[1]) * scale
    sheet = Image.new("RGB", (crop_w * 2, (crop_h + 34) * len(picks)), (45, 45, 45))
    draw = ImageDraw.Draw(sheet)
    for row, index in enumerate(picks):
        before = Image.open(before_paths[index]).convert("RGBA")
        y = row * (crop_h + 34)
        for col, frame in enumerate((before, after_frames[index])):
            view = composite(frame, (45, 45, 45, 255))
            crop = view.crop(crop_box).resize((crop_w, crop_h), Image.Resampling.NEAREST).convert("RGB")
            sheet.paste(crop, (col * crop_w, y + 34))
        draw.text((8, y + 8), f"frame {index + 1:03d}  V2 (left) / SMOOTHER V4 (right) / 200% nearest", fill="white")
    sheet.save(OUT / "idle_head_edge_before_after_darkgray.png", optimize=True)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    if len(paths) != COUNT:
        raise RuntimeError(f"Expected {COUNT} frames, found {len(paths)}")

    frames = []
    alpha_changes = []
    rgb_changes = []
    for i, path in enumerate(paths, 1):
        frame, alpha_changed, rgb_changed = smooth_outer_edge(path)
        frame.save(FRAMES / f"idle_{i:03d}.png", optimize=True)
        frames.append(frame)
        alpha_changes.append(alpha_changed)
        rgb_changes.append(rgb_changed)

    picks = np.linspace(0, COUNT - 1, 30).round().astype(int).tolist()
    colors = {
        "black": (0, 0, 0, 255),
        "gray": (45, 45, 45, 255),
        "green": (0, 255, 0, 255),
        "white": (255, 255, 255, 255),
    }
    for name, color in colors.items():
        make_contact(frames, name, color, picks)
    make_before_after(frames, [0, 59, 111, 148, 179])

    for name, color in (("black", colors["black"]), ("gray", colors["gray"])):
        views = [composite(f, color).resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for f in frames]
        views[0].save(
            OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
            append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False,
        )

    report = {
        "frames": COUNT,
        "fps": 60,
        "duration_seconds": 3.0,
        "alpha_pixels_changed_total": int(sum(alpha_changes)),
        "alpha_pixels_changed_per_frame_mean": float(np.mean(alpha_changes)),
        "rgb_pixels_changed_total": int(sum(rgb_changes)),
        "rgb_pixels_changed_per_frame_mean": float(np.mean(rgb_changes)),
        "scope": "outer silhouette only; internal line art unchanged",
    }
    (OUT / "smooth_edge_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
