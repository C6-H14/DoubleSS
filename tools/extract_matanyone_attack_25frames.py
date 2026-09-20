from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
TEST = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_v1"
VIDEO = ROOT / "art_candidates" / "video_source" / "attack_sword_v2.mp4"
SOURCE_RGBA = TEST / "rgba_frames_768"
SOURCE_ALPHA = TEST / "output_768" / "attack_sword_v2_matanyone768" / "pha"
OUT = TEST / "selected25_30fps"
FRAMES = OUT / "frames"
SELECTED = list(range(0, 97, 4))
FPS = 30


def checker(w, h, tile=24):
    yy, xx = np.indices((h, w))
    value = np.where(((xx // tile + yy // tile) % 2) == 0, 210, 145)
    return np.repeat(value[..., None], 3, axis=2).astype(np.uint8)


def composite(rgba, background):
    alpha = rgba[..., 3:4].astype(np.float32) / 255.0
    return np.uint8(np.round(rgba[..., :3] * alpha + background * (1.0 - alpha)))


def restore_white_clothing(rgba, source_rgb, original_alpha):
    alpha = rgba[..., 3]
    ys, xs = np.where(original_alpha >= 32)
    if len(xs) == 0:
        return rgba
    x0, x1 = xs.min(), xs.max() + 1
    y0, y1 = ys.min(), ys.max() + 1
    bw, bh = x1 - x0, y1 - y0

    hsv = cv2.cvtColor(source_rgb, cv2.COLOR_RGB2HSV)
    # White sailor collar/cuffs are bright and low-saturation, immediately
    # adjacent to the dark teal garment. This semantic guard avoids restoring
    # white background holes inside hair or between the head and hands.
    dark_garment = (hsv[..., 2] < 145) & (hsv[..., 1] > 28)
    near_garment = cv2.dilate(dark_garment.astype(np.uint8), np.ones((17, 17), np.uint8)) > 0
    pale_fabric = (hsv[..., 2] >= 145) & (hsv[..., 1] <= 105)
    yy, xx = np.indices(alpha.shape)
    upper_torso = (
        (xx >= x0 + int(0.18 * bw))
        & (xx <= x0 + int(0.82 * bw))
        & (yy >= y0 + int(0.16 * bh))
        & (yy <= y0 + int(0.58 * bh))
    )
    restore = pale_fabric & near_garment & upper_torso & (original_alpha > alpha)
    result = rgba.copy()
    result[..., 3][restore] = original_alpha[restore]
    result[..., :3][restore] = source_rgb[restore]
    return result


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    capture = cv2.VideoCapture(str(VIDEO))
    source_video = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        source_video.append(bgr)
    if len(source_video) < 97:
        raise RuntimeError(f"Expected at least 97 source frames, got {len(source_video)}")

    results = []
    mapping = []
    for output_index, source_index in enumerate(SELECTED, 1):
        rgba = np.asarray(Image.open(SOURCE_RGBA / f"frame_{source_index:03d}.png").convert("RGBA"))
        h, w = rgba.shape[:2]
        source_rgb = cv2.cvtColor(
            cv2.resize(source_video[source_index], (w, h), interpolation=cv2.INTER_AREA),
            cv2.COLOR_BGR2RGB,
        )
        original_alpha = cv2.imread(
            str(SOURCE_ALPHA / f"{source_index:05d}.png"), cv2.IMREAD_GRAYSCALE
        )
        repaired = restore_white_clothing(rgba, source_rgb, original_alpha)
        Image.fromarray(repaired, "RGBA").save(
            FRAMES / f"attack_sword_{output_index:02d}.png", optimize=True
        )
        results.append(repaired)
        mapping.append(
            f"{output_index:02d} <- source {source_index:03d} "
            f"({source_index / 24.149377593360995:.6f}s)"
        )

    duration = round(1000 / FPS)
    gif_frames = []
    for rgba in results:
        h, w = rgba.shape[:2]
        small = np.asarray(
            Image.fromarray(rgba, "RGBA").resize((512, round(h * 512 / w)), Image.Resampling.LANCZOS)
        )
        sh, sw = small.shape[:2]
        black = composite(small, np.zeros((sh, sw, 3), np.uint8))
        white = composite(small, np.full((sh, sw, 3), 255, np.uint8))
        grid = composite(small, checker(sw, sh, 20))
        gif_frames.append(Image.fromarray(np.concatenate((black, white, grid), axis=1), "RGB"))
    gif_frames[0].save(
        OUT / "attack_sword_selected25_30fps.gif",
        save_all=True,
        append_images=gif_frames[1:],
        duration=duration,
        loop=0,
        optimize=False,
    )

    # Black-background contact sheet is the acceptance view.
    thumbs = []
    for output_index, rgba in enumerate(results, 1):
        h, w = rgba.shape[:2]
        small = np.asarray(
            Image.fromarray(rgba, "RGBA").resize((256, round(h * 256 / w)), Image.Resampling.LANCZOS)
        )
        rendered = composite(small, np.zeros((*small.shape[:2], 3), np.uint8))
        thumb = Image.fromarray(rendered, "RGB")
        draw = ImageDraw.Draw(thumb)
        draw.rectangle((0, 0, 38, 22), fill=(0, 0, 0))
        draw.text((5, 4), f"{output_index:02d}", fill=(255, 255, 255))
        thumbs.append(thumb)
    cols = 5
    rows = (len(thumbs) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumbs[0].width, rows * thumbs[0].height), (0, 0, 0))
    for index, thumb in enumerate(thumbs):
        sheet.paste(thumb, ((index % cols) * thumb.width, (index // cols) * thumb.height))
    sheet.save(OUT / "black_contact_sheet.png")

    (OUT / "frame_mapping.txt").write_text(
        "Source: attack_sword_v2.mp4 / MatAnyone 1024x768 benchmark\n"
        f"Selection: every 4th decoded frame, source 000-096\nFrames: {len(results)}\n"
        f"Playback: {FPS} fps\nDuration: {len(results) / FPS:.6f}s\n"
        "White clothing: restored only for pale regions adjacent to dark garment in upper torso\n\n"
        + "\n".join(mapping)
        + "\n",
        encoding="utf-8",
    )
    print(f"frames={len(results)} fps={FPS} duration={len(results) / FPS:.6f}s")


if __name__ == "__main__":
    main()
