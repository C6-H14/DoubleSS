from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
TEST = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_pink_v1"
OUT = TEST / "selected47_50fps"
FRAMES = OUT / "frames"
SELECTED = (
    list(range(0, 17, 2))
    + [18, 26, 34, 42, 50, 52]
    + list(range(53, 73))
    + list(range(74, 87, 2))
    + list(range(88, 97, 2))
)
FPS = 50


def checker(w, h, tile=20):
    yy, xx = np.indices((h, w))
    value = np.where(((xx // tile + yy // tile) % 2) == 0, 210, 145)
    return np.repeat(value[..., None], 3, axis=2).astype(np.uint8)


def composite(rgba, background):
    alpha = rgba[..., 3:4].astype(np.float32) / 255.0
    return np.uint8(np.round(rgba[..., :3] * alpha + background * (1.0 - alpha)))


def clean_alpha(rgb, alpha, background):
    delta = rgb.astype(np.float32) - background.reshape(1, 1, 3)
    distance = np.sqrt(np.sum(delta * delta, axis=2))
    candidate = (distance <= 52).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(candidate, 8)
    connected = np.zeros(alpha.shape, dtype=bool)
    edge_labels = set(labels[0]) | set(labels[-1]) | set(labels[:, 0]) | set(labels[:, -1])
    for label in edge_labels:
        if label and stats[label, cv2.CC_STAT_AREA] >= 100:
            connected |= labels == label
    confidence = np.clip((distance - 4.0) / 42.0, 0.0, 1.0)
    limited = np.uint8(np.round(confidence * 255.0))
    result = alpha.copy()
    result[connected] = np.minimum(result[connected], limited[connected])
    return result


def decontaminate(rgb, alpha, background):
    a = alpha[..., None].astype(np.float32) / 255.0
    source = rgb.astype(np.float32)
    safe = np.maximum(a, 0.08)
    foreground = (source - (1.0 - a) * background.reshape(1, 1, 3)) / safe
    foreground = np.clip(foreground, 0, 255)
    opaque = alpha >= 235
    if np.any(opaque):
        _, nearest = distance_transform_edt(~opaque, return_indices=True)
        nearest_rgb = source[nearest[0], nearest[1]]
        fringe = (alpha > 2) & (alpha < 235)
        foreground[fringe] = nearest_rgb[fringe]
    foreground[a[..., 0] <= 0.01] = 0
    foreground = np.where(a >= 0.995, source, foreground)
    return np.uint8(np.round(foreground))


def main():
    original_video = Path(json.loads((TEST / "source.json").read_text(encoding="utf-8"))["video"])
    source_video = TEST / "input" / "attack_pink_source.mp4"
    pha_dirs = list((TEST / "output_768_ascii").glob("*/pha"))
    if len(pha_dirs) != 1:
        raise RuntimeError(f"Expected one alpha directory, found {pha_dirs}")
    alpha_paths = sorted(pha_dirs[0].glob("*.png"))
    capture = cv2.VideoCapture(str(source_video))
    decoded = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        decoded.append(bgr)
    if len(decoded) != len(alpha_paths):
        raise RuntimeError(f"Source/alpha mismatch: {len(decoded)} vs {len(alpha_paths)}")

    FRAMES.mkdir(parents=True, exist_ok=True)
    selected_rgba = []
    mapping = []
    for output_index, source_index in enumerate(SELECTED, 1):
        alpha = cv2.imread(str(alpha_paths[source_index]), cv2.IMREAD_GRAYSCALE)
        h, w = alpha.shape
        rgb = cv2.cvtColor(
            cv2.resize(decoded[source_index], (w, h), interpolation=cv2.INTER_AREA),
            cv2.COLOR_BGR2RGB,
        )
        border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
        background = np.median(border, axis=0).astype(np.float32)
        alpha = clean_alpha(rgb, alpha, background)
        clean_rgb = decontaminate(rgb, alpha, background)
        rgba = np.dstack((clean_rgb, alpha))
        Image.fromarray(rgba, "RGBA").save(FRAMES / f"attack_sword_{output_index:02d}.png", optimize=True)
        selected_rgba.append(rgba)
        mapping.append(f"{output_index:02d} <- source {source_index:03d} ({source_index / 24.149377593360995:.6f}s)")

    gif_frames = []
    for rgba in selected_rgba:
        h, w = rgba.shape[:2]
        small = np.asarray(Image.fromarray(rgba, "RGBA").resize((512, round(h * 512 / w)), Image.Resampling.LANCZOS))
        sh, sw = small.shape[:2]
        panels = [
            composite(small, np.zeros((sh, sw, 3), np.uint8)),
            composite(small, np.full((sh, sw, 3), 255, np.uint8)),
            composite(small, checker(sw, sh)),
        ]
        gif_frames.append(Image.fromarray(np.concatenate(panels, axis=1), "RGB"))
    gif_frames[0].save(
        OUT / "attack_sword_pink47_50fps.gif", save_all=True,
        append_images=gif_frames[1:], duration=round(1000/FPS), loop=0, optimize=False,
    )

    thumbs = []
    for index, rgba in enumerate(selected_rgba, 1):
        h, w = rgba.shape[:2]
        small = np.asarray(Image.fromarray(rgba, "RGBA").resize((240, round(h * 240 / w)), Image.Resampling.LANCZOS))
        black = composite(small, np.zeros((*small.shape[:2], 3), np.uint8))
        thumb = Image.fromarray(black, "RGB")
        draw = ImageDraw.Draw(thumb)
        draw.rectangle((0, 0, 38, 21), fill=(0,0,0))
        draw.text((5,4), f"{index:02d}", fill=(255,255,255))
        thumbs.append(thumb)
    cols = 6
    rows = (len(thumbs)+cols-1)//cols
    sheet = Image.new("RGB", (cols*thumbs[0].width, rows*thumbs[0].height), (0,0,0))
    for i, thumb in enumerate(thumbs):
        sheet.paste(thumb, ((i%cols)*thumb.width, (i//cols)*thumb.height))
    sheet.save(OUT / "black_contact_sheet.png")

    (OUT / "frame_mapping.txt").write_text(
        f"Source: {original_video}\nWorking copy: {source_video}\nFrames: {len(SELECTED)}\nPlayback: {FPS} fps\n"
        f"Duration: {len(SELECTED)/FPS:.6f}s\n\n" + "\n".join(mapping) + "\n",
        encoding="utf-8",
    )
    print(f"frames={len(SELECTED)} fps={FPS} duration={len(SELECTED)/FPS:.6f}s")


if __name__ == "__main__":
    main()
