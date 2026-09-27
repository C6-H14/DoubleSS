from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "command_ally"
SOURCE = BASE / "final_v1_matanyone" / "alpha_frames"
ORIGINAL = BASE / "recovered_gif_v1" / "frames"
OUTPUT = BASE / "final_v3_degreen_noholes"
FRAMES = OUTPUT / "alpha_frames"


def clean_frame(path: Path, index: int) -> Image.Image:
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)
    rgb = np.asarray(Image.open(ORIGINAL / f"command_ally_{index:03d}.png").convert("RGB"), dtype=np.uint8).copy()
    # MatAnyone's alpha is the authoritative silhouette. Never intersect it
    # with a colour-key mask: that previously erased thousands of legitimate
    # hair, skin and clothing pixels and created transparent holes.
    alpha = rgba[..., 3].copy()
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    rf, gf, bf = (rgb[..., channel].astype(np.int16) for channel in range(3))
    magenta_plate = ((hsv[..., 0] >= 132) & (hsv[..., 1] >= 28) &
                     ((rf - gf) >= 18) & ((bf - gf) >= 12))

    foreground = alpha >= 3
    distance_inside = cv2.distanceTransform(foreground.astype(np.uint8), cv2.DIST_L2, 5)
    core = (alpha >= 245) & (distance_inside >= 5.0) & ~magenta_plate
    if not core.any():
        raise RuntimeError(f"No safe interior core in {path}")

    # Every antialiased outer texel receives RGB from the nearest safe opaque
    # interior pixel. Alpha remains untouched, so the silhouette is preserved
    # while green/magenta spill under linear filtering is eliminated.
    _, nearest = distance_transform_edt(~core, return_indices=True)
    contaminated = foreground & ((alpha < 250) | (distance_inside < 4.5) | magenta_plate)
    rgb[contaminated] = rgb[nearest[0], nearest[1]][contaminated]
    rgb[~foreground] = 0
    return Image.fromarray(np.dstack((rgb, alpha)), "RGBA")


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("command_ally_*.png"))
    if len(paths) != 16:
        raise RuntimeError(f"Expected 16 frames, found {len(paths)}")
    frames = []
    for index, path in enumerate(paths, 1):
        frame = clean_frame(path, index)
        frame.save(FRAMES / f"command_ally_{index:03d}.png", optimize=True)
        frames.append(frame)

    durations = [40] * 16
    durations[-1] = 100
    for name, color in (
        ("black", (0, 0, 0, 255)),
        ("white", (255, 255, 255, 255)),
        ("dark_red", (45, 0, 10, 255)),
    ):
        background = Image.new("RGBA", frames[0].size, color)
        views = [Image.alpha_composite(background.copy(), frame).resize((556, 417), Image.Resampling.LANCZOS).convert("RGB")
                 for frame in frames]
        views[0].save(OUTPUT / f"command_ally_{name}_700ms.gif", save_all=True,
                      append_images=views[1:], duration=durations, loop=0, disposal=2)
    print(f"cleaned={len(frames)} output={OUTPUT}")


if __name__ == "__main__":
    main()
