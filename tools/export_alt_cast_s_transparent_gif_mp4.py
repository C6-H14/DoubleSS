from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_cast_s")
SOURCE = ROOT / "cast_s_v5_alpha_clean_45f_60fps" / "alpha_frames_45f_60fps"
OUT = ROOT / "cast_s_v8_transparent_gif_mp4"
BG = np.array([32, 32, 32], dtype=np.float32)


def main() -> None:
    paths = sorted(SOURCE.glob("cast_s_alpha_*.png"))
    if len(paths) != 45:
        raise RuntimeError(f"Expected 45 frames; found {len(paths)}")
    OUT.mkdir(parents=True, exist_ok=True)
    rgba_frames = [Image.open(path).convert("RGBA") for path in paths]

    # Reserve index 255 for transparency across every frame. Give it one
    # global palette, with no per-frame dithering or palette reordering.
    previews = []
    masks = []
    for image in rgba_frames:
        arr = np.asarray(image.resize((640, 480), Image.Resampling.LANCZOS)).astype(np.float32)
        alpha = arr[:, :, 3:4] / 255.0
        rgb_on_dark = np.clip(np.rint(arr[:, :, :3] * alpha + BG * (1 - alpha)), 0, 255).astype(np.uint8)
        previews.append(Image.fromarray(rgb_on_dark, "RGB"))
        masks.append(arr[:, :, 3] >= 160)
    sample = Image.new("RGB", (640 * 9, 480 * 5))
    for i, image in enumerate(previews):
        sample.paste(image, ((i % 9) * 640, (i // 9) * 480))
    palette = sample.quantize(colors=255, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    palette_data = palette.getpalette()[:255 * 3] + [0, 0, 0]
    indexed = []
    for image, mask in zip(previews, masks):
        frame = image.quantize(palette=palette, dither=Image.Dither.NONE)
        index = np.asarray(frame).copy()
        index[~mask] = 255
        frame = Image.fromarray(index, "P")
        frame.putpalette(palette_data)
        indexed.append(frame)
    delays = [20 if i % 3 != 1 else 10 for i in range(len(indexed))]
    gif_path = OUT / "cast_s_transparent_45f_60fps.gif"
    indexed[0].save(gif_path, save_all=True, append_images=indexed[1:],
                    duration=delays, loop=0, disposal=2, transparency=255, optimize=False)

    mp4_path = OUT / "cast_s_dark_preview_1280x960_60fps.mp4"
    writer = cv2.VideoWriter(str(mp4_path), cv2.VideoWriter_fourcc(*"mp4v"), 60.0, (1280, 960))
    if not writer.isOpened():
        raise RuntimeError("OpenCV could not open MP4 encoder")
    for image in rgba_frames:
        arr = np.asarray(image).astype(np.float32)
        alpha = arr[:, :, 3:4] / 255.0
        rgb = np.clip(np.rint(arr[:, :, :3] * alpha + BG * (1 - alpha)), 0, 255).astype(np.uint8)
        writer.write(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR))
    writer.release()

    # Decode the actual GIF frame, not the PNG source, for visible QA.
    decoded = Image.open(gif_path)
    decoded.seek(28)
    frame29 = decoded.convert("RGBA")
    canvas = Image.alpha_composite(Image.new("RGBA", frame29.size, tuple(BG.astype(int)) + (255,)), frame29)
    canvas.save(OUT / "frame_029_transparent_gif_on_dark.png")
    capture = cv2.VideoCapture(str(mp4_path))
    mp4_frames = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
    mp4_fps = capture.get(cv2.CAP_PROP_FPS)
    capture.release()
    check = Image.open(gif_path)
    check.seek(0)
    transparent = np.asarray(check.convert("RGBA"))[:, :, 3]
    print({"gif_frames": check.n_frames, "gif_duration_ms": sum(delays),
           "gif_transparent_fraction_frame1": round(float(np.mean(transparent == 0)), 3),
           "mp4_frames": mp4_frames, "mp4_fps": mp4_fps,
           "gif_bytes": gif_path.stat().st_size, "mp4_bytes": mp4_path.stat().st_size})


if __name__ == "__main__":
    main()
