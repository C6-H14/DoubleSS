from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "cast_prayer"
SOURCE = BASE / "preview_v6" / "frames"
OUTPUT = BASE / "preview_v8_border_connected"
FRAMES = OUTPUT / "alpha_frames"


def border_connected_matte(path: Path) -> Image.Image:
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, _ = cv2.split(hsv)
    rf, gf, bf = (rgb[..., i].astype(np.float32) for i in range(3))
    bias = (rf + bf) * 0.5 - gf

    # Background candidacy requires meaningful magenta saturation/bias. This
    # deliberately excludes white socks, skin highlights and silver sword pixels.
    candidate = (((h >= 132) & (s >= 22) & (bias >= 7)) |
                 (((rf - gf) >= 18) & ((bf - gf) >= 18))).astype(np.uint8)
    candidate = cv2.morphologyEx(candidate, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))

    # Keep only magenta regions connected to the canvas boundary. Internal
    # character colours can no longer punch transparent holes through the model.
    count, labels, stats, _ = cv2.connectedComponentsWithStats(candidate, 8)
    border_labels = set(np.unique(np.concatenate((labels[0], labels[-1], labels[:, 0], labels[:, -1]))))
    background = np.zeros(candidate.shape, dtype=bool)
    for label in border_labels:
        if label:
            background |= labels == label

    # Remove large enclosed pink pockets (for example between hair strands) but
    # never tiny character-colour islands.
    for label in range(1, count):
        if label not in border_labels and stats[label, cv2.CC_STAT_AREA] >= 700:
            background |= labels == label

    # One-pixel antialias transition on the foreground side of the boundary.
    foreground = ~background
    distance = cv2.distanceTransform(foreground.astype(np.uint8), cv2.DIST_L2, 5)
    alpha = np.uint8(np.clip(distance / 1.25, 0.0, 1.0) * 255.0)
    alpha[background] = 0
    alpha[alpha > 250] = 255

    # Edge RGB comes from a safe interior pixel, preventing pink texture bleed
    # under Spine's linear filtering without shrinking the silhouette.
    core = distance >= 3.0
    _, nearest = distance_transform_edt(~core, return_indices=True)
    clean = rgb.copy()
    edge = (alpha > 0) & (distance < 3.0)
    clean[edge] = rgb[nearest[0], nearest[1]][edge]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def checkerboard(size, cell=32):
    image = Image.new("RGBA", size, (220, 220, 220, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], cell):
        for x in range(0, size[0], cell):
            if (x // cell + y // cell) % 2:
                draw.rectangle((x, y, x + cell - 1, y + cell - 1), fill=(165, 165, 165, 255))
    return image


def composite(frame, background):
    return Image.alpha_composite(background.copy(), frame)


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("cast_prayer_*.png"))
    frames = []
    for index, path in enumerate(paths, 1):
        frame = border_connected_matte(path)
        frame.save(FRAMES / f"cast_prayer_{index:03d}.png", optimize=True)
        frames.append(frame)
    if len(frames) != 41:
        raise RuntimeError(f"Expected 41 frames, found {len(frames)}")

    duration = 29  # 41 frames in about 1.2 seconds.
    for name, background in (
        ("black", Image.new("RGBA", frames[0].size, (0, 0, 0, 255))),
        ("checker", checkerboard(frames[0].size)),
    ):
        rendered = [composite(frame, background).resize((512, 512), Image.Resampling.LANCZOS).convert("RGB")
                    for frame in frames]
        rendered[0].save(OUTPUT / f"cast_prayer_{name}_1200ms.gif", save_all=True,
                         append_images=rendered[1:], duration=duration, loop=0, disposal=2)

    cols, rows = 7, 6
    w, h = frames[0].size
    sheet = Image.new("RGBA", (cols * w, rows * h), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        sheet.alpha_composite(frame, ((i % cols) * w, (i // cols) * h))
    sheet.save(OUTPUT / "cast_prayer_alpha_sheet_7x6.png", optimize=True)

    thumb = 320
    overview = Image.new("RGB", (cols * thumb, rows * (thumb + 28)), "#222")
    draw = ImageDraw.Draw(overview)
    bg = checkerboard(frames[0].size)
    for i, frame in enumerate(frames):
        x, y = (i % cols) * thumb, (i // cols) * (thumb + 28)
        overview.paste(composite(frame, bg).resize((thumb, thumb), Image.Resampling.LANCZOS).convert("RGB"), (x, y))
        draw.text((x + 5, y + thumb + 5), f"frame {i+1:02d}", fill="white")
    overview.save(OUTPUT / "cast_prayer_checker_contact.jpg", quality=96)
    (OUTPUT / "metadata.json").write_text(json.dumps({"frames": 41, "duration": 1.2,
                                                       "position_source": "preview_v6 unshifted"}, indent=2) + "\n")
    print(f"frames={len(frames)} output={OUTPUT}")


if __name__ == "__main__":
    main()
