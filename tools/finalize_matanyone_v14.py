from pathlib import Path
import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt

ROOT = Path(r"D:\StSmod\DoubleSS")
C = ROOT / "art_candidates" / "video_source" / "new_animation_candidates_v1"
W = ROOT / "art_candidates" / "matting_tests" / "matanyone_v14_three"
OUT = W / "final_rgba"
SETS = {
    "attack_heavy": (C / "attack_heavy" / "pink_frames", "attack_heavy_*.png"),
    "cast_debuff": (C / "cast_debuff" / "pink_frames", "cast_debuff_*.png"),
    "cast_buff": (C / "cast_buff_transition_test_v6" / "pink_frames", "cast_buff_*.png"),
}


def add_debuff_smoke(rgb, alpha):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    # Purple smoke is well separated from the pink plate (H about 165). Start
    # from unambiguous purple cores and only admit a narrow local fringe around
    # them, so no unrelated plate region can enter the matte.
    seed = (h >= 112) & (h <= 151) & (s >= 18) & (v >= 45)
    region = cv2.dilate(seed.astype(np.uint8), np.ones((9, 9), np.uint8), iterations=1).astype(bool)
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    bg = np.median(border, axis=0).astype(np.float32)
    distance = np.linalg.norm(rgb.astype(np.float32) - bg.reshape(1, 1, 3), axis=2)
    smoke_alpha = np.uint8(np.round(np.clip((distance - 3.0) / 25.0, 0.0, 1.0) * 255.0))
    smoke_alpha[~region] = 0
    return np.maximum(alpha, smoke_alpha)


def recover_connected_foreground(rgb, alpha):
    """Recover sword/body/effect regions missed by temporal semantic tracking."""
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    bg = np.median(border, axis=0).astype(np.float32)
    distance = np.linalg.norm(rgb.astype(np.float32) - bg.reshape(1, 1, 3), axis=2)
    seed = (distance >= 58.0).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(seed, 8)
    near_subject = cv2.dilate((alpha >= 24).astype(np.uint8), np.ones((41, 41), np.uint8), iterations=1).astype(bool)
    selected = np.zeros(alpha.shape, dtype=np.uint8)
    for label in range(1, count):
        component = labels == label
        if stats[label, cv2.CC_STAT_AREA] >= 8 and np.any(component & near_subject):
            selected[component] = 1
    region = cv2.dilate(selected, np.ones((9, 9), np.uint8), iterations=1).astype(bool)
    candidate = np.uint8(np.round(np.clip((distance - 9.0) / 46.0, 0.0, 1.0) * 255.0))
    candidate[~region] = 0
    return np.maximum(alpha, candidate)


def remove_pink_plate(rgb, alpha):
    """Remove residual generated plate pixels that the semantic matte tracked.

    The character palette has no saturated magenta. Debuff smoke ends around
    H=151, while the source plate and its vivid remnants measure H=158..179.
    A soft hue/saturation ramp therefore removes the plate without cutting the
    purple smoke, skin, gold hair, gold spell light, or steel sword.
    """
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    hue_strength = np.clip((h.astype(np.float32) - 151.0) / 8.0, 0.0, 1.0)
    sat_strength = np.clip((s.astype(np.float32) - 34.0) / 58.0, 0.0, 1.0)
    value_strength = np.clip((v.astype(np.float32) - 45.0) / 85.0, 0.0, 1.0)
    pink_strength = hue_strength * sat_strength * value_strength
    rf = rgb[:, :, 0].astype(np.float32)
    gf = rgb[:, :, 1].astype(np.float32)
    bf = rgb[:, :, 2].astype(np.float32)
    magenta_bias = (rf + bf) * 0.5 - gf
    salmon_strength = np.clip((magenta_bias - 18.0) / 24.0, 0.0, 1.0)
    salmon_strength *= np.clip((v.astype(np.float32) - 65.0) / 90.0, 0.0, 1.0)
    # The remaining pale plate mix can have low saturation, but unlike skin,
    # hair and gold light its blue channel is not lower than green.
    blue_green_strength = np.clip((bf - gf + 3.0) / 18.0, 0.0, 1.0)
    blue_green_strength *= np.clip((v.astype(np.float32) - 75.0) / 85.0, 0.0, 1.0)
    blue_green_strength *= np.clip((s.astype(np.float32) - 14.0) / 34.0, 0.0, 1.0)
    purple_effect = (h >= 108) & (h <= 153) & (s >= 12)
    salmon_strength[purple_effect] = 0.0
    blue_green_strength[purple_effect] = 0.0
    pink_strength = np.maximum.reduce((pink_strength, salmon_strength, blue_green_strength))
    cleaned = np.uint8(np.round(alpha.astype(np.float32) * (1.0 - pink_strength)))
    cleaned[cleaned < 3] = 0
    return cleaned


def repair_plate_mix_at_boundary(rgb, alpha):
    """Recolour salmon plate mix without shrinking the character silhouette."""
    foreground = (alpha >= 12).astype(np.uint8)
    interior = cv2.erode(foreground, np.ones((9, 9), np.uint8), iterations=1).astype(bool)
    boundary = foreground.astype(bool) & ~interior
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    bg = np.median(border, axis=0).astype(np.float32)
    distance = np.linalg.norm(rgb.astype(np.float32) - bg.reshape(1, 1, 3), axis=2)
    # Purple debuff smoke is an intentional effect, including its soft edge.
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, _ = cv2.split(hsv)
    purple = (h >= 108) & (h <= 153) & (s >= 12)
    repair = boundary & (distance < 112.0) & ~purple
    safe = interior & (distance >= 105.0) & (alpha >= 210)
    if not safe.any():
        return rgb
    _, nearest = distance_transform_edt(~safe, return_indices=True)
    result = rgb.copy()
    result[repair] = rgb[nearest[0], nearest[1]][repair]
    return result


def edge_clean_rgb(rgb, alpha):
    solid = alpha >= 238
    if not solid.any():
        raise RuntimeError("No solid foreground")
    _, nearest = distance_transform_edt(~solid, return_indices=True)
    clean = rgb.copy()
    # Semantic alpha gives the right boundary; replacing only its soft edge RGB
    # removes the pink pre-multiplication that the generator baked into pixels.
    fringe = (alpha > 2) & (alpha < 238)
    clean[fringe] = rgb[nearest[0], nearest[1]][fringe]
    clean[alpha <= 2] = 0
    return clean


def composite(rgba, color):
    return Image.alpha_composite(Image.new("RGBA", rgba.size, color), rgba).convert("RGB")


def main():
    tiles = []
    for name, (folder, pattern) in SETS.items():
        sources = sorted(folder.glob(pattern))
        pha_dirs = list((W / name / "output").glob("*/pha"))
        if len(pha_dirs) != 1:
            raise RuntimeError((name, pha_dirs))
        phas = sorted(pha_dirs[0].glob("*.png"))
        if len(sources) != len(phas):
            raise RuntimeError(f"{name}: {len(sources)} source vs {len(phas)} alpha")
        out_dir = OUT / name
        out_dir.mkdir(parents=True, exist_ok=True)
        made = []
        for index, (source, pha) in enumerate(zip(sources, phas), 1):
            rgb = np.asarray(Image.open(source).convert("RGB"))
            alpha = cv2.imread(str(pha), cv2.IMREAD_GRAYSCALE)
            alpha = cv2.resize(alpha, (rgb.shape[1], rgb.shape[0]), interpolation=cv2.INTER_LINEAR)
            alpha[alpha < 3] = 0
            alpha = recover_connected_foreground(rgb, alpha)
            if name == "cast_debuff":
                alpha = add_debuff_smoke(rgb, alpha)
            alpha = remove_pink_plate(rgb, alpha)
            repaired_rgb = repair_plate_mix_at_boundary(rgb, alpha)
            clean = edge_clean_rgb(repaired_rgb, alpha)
            rgba = Image.fromarray(np.dstack((clean, alpha)), "RGBA")
            rgba.save(out_dir / f"{name}_{index:03d}.png", optimize=True)
            made.append(rgba)
        for i in np.linspace(0, len(made)-1, min(12, len(made))).round().astype(int):
            im = made[int(i)].resize((280, 210), Image.Resampling.LANCZOS)
            tile = Image.new("RGB", (280, 444), (32, 32, 32))
            tile.paste(composite(im, "black"), (0, 0)); tile.paste(composite(im, "white"), (0, 210))
            ImageDraw.Draw(tile).text((5, 423), f"{name} {int(i)+1:03d}", fill="white")
            tiles.append(tile)
    cols = 6; rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (cols*280, rows*444), (20,20,20))
    for i, tile in enumerate(tiles):
        sheet.paste(tile, ((i%cols)*280, (i//cols)*444))
    sheet.save(W / "final_alpha_black_white.jpg", quality=95)
    print(W / "final_alpha_black_white.jpg")


if __name__ == "__main__":
    main()
