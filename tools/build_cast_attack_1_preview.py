from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO = ROOT / "art_candidates" / "video_source" / "cast_v1" / "cast_attack_1_source.mp4"
MASTER = (
    ROOT
    / "art_candidates"
    / "matting_tests"
    / "matanyone_attack_pink_v1"
    / "selected47_50fps"
    / "transparent_original_endpoint_aligned.png"
)
OUT = ROOT / "art_candidates" / "video_source" / "cast_v1" / "preview_v1_41f_50fps"
FRAMES = OUT / "frames"

TARGET_SIZE = (1024, 768)
SOURCE_INDICES = list(range(6, 87, 2))  # zero-based: source frames 7,9,...,87
FPS = 50


def smoothstep(edge0, edge1, value):
    t = np.clip((value - edge0) / (edge1 - edge0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def alpha_from_pink(rgb, reference_rgb):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    hue = hsv[..., 0].astype(np.float32)
    sat = hsv[..., 1].astype(np.float32)
    red = rgb[..., 0].astype(np.float32)
    green = rgb[..., 1].astype(np.float32)
    blue = rgb[..., 2].astype(np.float32)

    # Estimate pink hue from the outer border; circular hue distance uses the
    # OpenCV 0..179 hue scale.
    border_h = np.concatenate((hue[0], hue[-1], hue[:, 0], hue[:, -1]))
    border_s = np.concatenate((sat[0], sat[-1], sat[:, 0], sat[:, -1]))
    h0 = float(np.median(border_h[border_s > 80]))
    hue_delta = np.abs(hue - h0)
    hue_delta = np.minimum(hue_delta, 180.0 - hue_delta)

    hue_match = 1.0 - smoothstep(7.0, 25.0, hue_delta)
    saturation_match = smoothstep(45.0, 120.0, sat)
    magenta_rb = np.minimum(red - green, blue - green)
    magenta_match = smoothstep(8.0, 55.0, magenta_rb)
    background_confidence = hue_match * saturation_match * magenta_match
    alpha = 1.0 - background_confidence

    # The rune did not exist in the reference frame. Preserve changes in the
    # right-side spell area even where a faint gold glow is still pink-ish.
    diff = np.max(np.abs(rgb.astype(np.int16) - reference_rgb.astype(np.int16)), axis=2).astype(np.float32)
    change_alpha = smoothstep(8.0, 42.0, diff)
    x = np.arange(rgb.shape[1], dtype=np.float32)[None, :]
    spell_area = smoothstep(rgb.shape[1] * 0.56, rgb.shape[1] * 0.68, x)
    alpha = np.maximum(alpha, change_alpha * spell_area)

    # Stabilize tiny codec noise without erasing antialiased boundaries.
    alpha = cv2.GaussianBlur(alpha, (0, 0), 0.65)
    alpha[alpha < 0.035] = 0.0
    alpha[alpha > 0.965] = 1.0
    return np.uint8(np.round(alpha * 255.0))


def decontaminate(rgb, alpha):
    solid = alpha >= 245
    if not solid.any():
        return rgb
    # For translucent silhouette pixels use the nearest solid foreground color,
    # avoiding a pink RGB fringe when Spine filters against transparent texels.
    _, indices = distance_transform_edt(~solid, return_indices=True)
    nearest = rgb[indices[0], indices[1]]
    result = rgb.copy()
    edge = (alpha > 0) & (alpha < 245)
    result[edge] = nearest[edge]
    result[alpha == 0] = 0
    return result


def read_frame(capture, index):
    capture.set(cv2.CAP_PROP_POS_FRAMES, index)
    ok, bgr = capture.read()
    if not ok:
        raise RuntimeError(f"Could not read source frame {index + 1}")
    bgr = cv2.resize(bgr, TARGET_SIZE, interpolation=cv2.INTER_AREA)
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def fit_endpoint(master, target):
    target_bbox = target.getchannel("A").getbbox()
    source_bbox = master.getchannel("A").getbbox()
    if target_bbox is None or source_bbox is None:
        raise RuntimeError("Cannot align an empty endpoint")
    crop = master.crop(source_bbox)
    x0, y0, x1, y1 = target_bbox
    crop = crop.resize((x1 - x0, y1 - y0), Image.Resampling.LANCZOS)
    result = Image.new("RGBA", target.size, (0, 0, 0, 0))
    result.alpha_composite(crop, (x0, y0))
    return result


def on_background(image, color):
    background = Image.new("RGBA", image.size, (*color, 255))
    return Image.alpha_composite(background, image).convert("RGB")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    FRAMES.mkdir(parents=True, exist_ok=True)
    for old in FRAMES.glob("cast_attack_1_*.png"):
        old.unlink()

    capture = cv2.VideoCapture(str(VIDEO))
    if not capture.isOpened():
        raise RuntimeError(f"Cannot open {VIDEO}")
    reference = read_frame(capture, 0)
    images = []
    for source_index in SOURCE_INDICES:
        rgb = read_frame(capture, source_index)
        alpha = alpha_from_pink(rgb, reference)
        clean_rgb = decontaminate(rgb, alpha)
        images.append(Image.fromarray(np.dstack((clean_rgb, alpha)), "RGBA"))
    capture.release()

    # Exact transparent endpoints, aligned to the extracted first silhouette.
    master = Image.open(MASTER).convert("RGBA")
    endpoint = fit_endpoint(master, images[0])
    images[0] = endpoint
    images[-1] = endpoint.copy()

    for index, image in enumerate(images, 1):
        image.save(FRAMES / f"cast_attack_1_{index:02d}.png", optimize=True)

    black = [on_background(image, (0, 0, 0)) for image in images]
    black[0].save(
        OUT / "cast_attack_1_41f_50fps_black.gif",
        save_all=True,
        append_images=black[1:],
        duration=20,
        loop=0,
        disposal=2,
    )
    checker = Image.new("RGB", TARGET_SIZE, "white")
    tile = 24
    draw_checker = ImageDraw.Draw(checker)
    for y in range(0, TARGET_SIZE[1], tile):
        for x in range(0, TARGET_SIZE[0], tile):
            if (x // tile + y // tile) % 2:
                draw_checker.rectangle((x, y, x + tile - 1, y + tile - 1), fill="#b8b8b8")
    checker_frames = []
    for image in images:
        base = checker.convert("RGBA")
        checker_frames.append(Image.alpha_composite(base, image).convert("RGB"))
    checker_frames[0].save(
        OUT / "cast_attack_1_41f_50fps_checker.gif",
        save_all=True,
        append_images=checker_frames[1:],
        duration=20,
        loop=0,
        disposal=2,
    )

    thumb_w, thumb_h = 256, 192
    cols = 7
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + 22)), "black")
    draw = ImageDraw.Draw(sheet)
    for i, image in enumerate(images):
        thumb = on_background(image, (0, 0, 0)).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = (i % cols) * thumb_w
        y = (i // cols) * (thumb_h + 22)
        sheet.paste(thumb, (x, y))
        draw.text((x + 5, y + thumb_h + 3), f"{i + 1:02d} <- src {SOURCE_INDICES[i] + 1:03d}", fill="white")
    sheet.save(OUT / "black_contact_sheet.png", optimize=True)

    metadata = {
        "source": str(VIDEO),
        "source_frames_one_based": [i + 1 for i in SOURCE_INDICES],
        "output_frames": len(images),
        "fps": FPS,
        "duration_seconds": len(images) / FPS,
        "endpoint_source": str(MASTER),
    }
    (OUT / "metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
