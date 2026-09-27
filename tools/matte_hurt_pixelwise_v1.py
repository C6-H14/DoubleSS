from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "hurt" / "preview_v3_11f_60fps" / "frames"
OUTPUT = ROOT / "art_candidates" / "video_source" / "hurt" / "final_v3_alpha"
FRAMES = OUTPUT / "alpha_frames"


def matte(path: Path) -> Image.Image:
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)

    # All eleven source frames have a tightly clustered pink background around
    # H=163..165. Loose saturation/value margins include compressed edge pixels,
    # while skin, hair, teal cloth, shoes and the silver/gold sword stay outside.
    background = (h >= 157) & (h <= 171) & (s >= 60) & (v >= 120)
    yy = np.indices(h.shape)[0]
    floor_smear = ((yy > int(h.shape[0] * 0.68)) &
                   (h >= 140) & (h <= 179) & (s >= 30) &
                   (v >= 70) & (v <= 230))
    background |= floor_smear
    background = cv2.morphologyEx(background.astype(np.uint8), cv2.MORPH_CLOSE,
                                  np.ones((3, 3), np.uint8)).astype(bool)

    foreground = ~background
    distance = cv2.distanceTransform(foreground.astype(np.uint8), cv2.DIST_L2, 5)
    alpha = np.uint8(np.clip(distance / 1.25, 0.0, 1.0) * 255.0)
    alpha[background] = 0
    alpha[alpha > 250] = 255

    opaque = alpha >= 250
    _, nearest = distance_transform_edt(~opaque, return_indices=True)
    clean = rgb.copy()
    edge = (alpha > 0) & (alpha < 250)
    clean[edge] = rgb[nearest[0], nearest[1]][edge]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("hurt_*.png"))
    if len(paths) != 11:
        raise RuntimeError(f"Expected 11 frames, found {len(paths)}")

    frames = []
    for index, path in enumerate(paths, 1):
        frame = matte(path)
        frame.save(FRAMES / f"hurt_{index:03d}.png", optimize=True)
        frames.append(frame)

    for name, color in (("black", (0, 0, 0, 255)), ("white", (255, 255, 255, 255))):
        views = [Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)
                 .resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for frame in frames]
        views[0].save(OUTPUT / f"hurt_{name}_11f_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2,
                      optimize=False)

        columns, cell_w, cell_h = 4, 278, 234
        sheet = Image.new("RGB", (columns * cell_w, 3 * cell_h), (18, 18, 18))
        draw = ImageDraw.Draw(sheet)
        for index, view in enumerate(views, 1):
            thumb = view.resize((278, 209), Image.Resampling.LANCZOS)
            x = ((index - 1) % columns) * cell_w
            y = ((index - 1) // columns) * cell_h
            sheet.paste(thumb, (x, y + 25))
            draw.text((x + 5, y + 5), f"{index:02d}", fill="white")
        sheet.save(OUTPUT / f"hurt_{name}_contact.jpg", quality=96)

    metadata = {"frames": 11, "fps": 60, "duration": 11 / 60,
                "method": "per-pixel HSV pink removal plus lower-frame floor-smear cleanup"}
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
