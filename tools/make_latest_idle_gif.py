from pathlib import Path

import cv2
from PIL import Image


VIDEO_DIR = Path(r"D:\StSmod\art_preview\video")
OUTPUT = VIDEO_DIR / "idle_new_full_24fps.gif"
PREVIEW_OUTPUT = VIDEO_DIR / "idle_new_preview_12fps.gif"


def main() -> None:
    source = max(VIDEO_DIR.glob("*.mp4"), key=lambda path: path.stat().st_mtime)
    capture = cv2.VideoCapture(str(source))
    fps = capture.get(cv2.CAP_PROP_FPS)
    frames: list[Image.Image] = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        frames.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)))
    capture.release()
    if not frames:
        raise RuntimeError(f"No frames decoded from {source}")

    # Derive one shared palette so the GIF does not shimmer between frames.
    samples = []
    for frame in frames[:: max(1, len(frames) // 16)]:
        samples.append(frame.resize((160, 160), Image.Resampling.LANCZOS))
    sheet = Image.new("RGB", (160 * len(samples), 160))
    for index, sample in enumerate(samples):
        sheet.paste(sample, (index * 160, 0))
    palette = sheet.quantize(colors=256, method=Image.Quantize.MEDIANCUT)
    indexed = [
        frame.quantize(palette=palette, dither=Image.Dither.FLOYDSTEINBERG)
        for frame in frames
    ]
    indexed[0].save(
        OUTPUT,
        save_all=True,
        append_images=indexed[1:],
        duration=round(1000 / fps),
        loop=0,
        disposal=1,
        optimize=False,
    )
    preview = indexed[::2]
    preview[0].save(
        PREVIEW_OUTPUT,
        save_all=True,
        append_images=preview[1:],
        duration=round(2000 / fps),
        loop=0,
        disposal=1,
        optimize=True,
    )
    print(f"source={source}")
    print(f"frames={len(frames)} fps={fps:.6f} duration={len(frames)/fps:.6f}")
    print(f"output={OUTPUT} bytes={OUTPUT.stat().st_size}")
    print(f"preview={PREVIEW_OUTPUT} frames={len(preview)} bytes={PREVIEW_OUTPUT.stat().st_size}")


if __name__ == "__main__":
    main()
