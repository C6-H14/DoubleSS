from pathlib import Path

import cv2
import numpy as np
from PIL import Image

from test_repair_combo_first_thrust_sword import NEW_WIDTH, SOURCE, repair_frame


ROOT = Path(r"D:\StSmod")
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v4_first_thrust_repaired"
FRAMES = OUTPUT / "frames_120f_60fps"
GIF = OUTPUT / "combo_attack_v4_first_thrust_repaired_120f_60fps.gif"
MP4 = OUTPUT / "combo_attack_v4_first_thrust_repaired_120f_60fps.mp4"
FPS = 60.0
REPAIRED = set(range(21, 38))


def extend_background(image: np.ndarray) -> np.ndarray:
    """Add right-side breathing room without copying any clipped object pixels."""
    height, width = image.shape[:2]
    canvas = np.empty((height, NEW_WIDTH, 3), dtype=np.uint8)
    canvas[:, :width] = image
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
    sample = image[:, width - 180 : width - 50]
    sample_hsv = hsv[:, width - 180 : width - 50]
    for y in range(height):
        # Purple background is strongly saturated; ignore neutral sword/person pixels.
        usable = sample_hsv[y, :, 1] >= 95
        pixels = sample[y, usable]
        if len(pixels) < 8:
            pixels = sample[y]
        canvas[y, width:] = np.median(pixels, axis=0).astype(np.uint8)
    return canvas


def main() -> None:
    FRAMES.mkdir(parents=True, exist_ok=True)
    images: list[Image.Image] = []
    for number in range(1, 121):
        if number in REPAIRED:
            image = repair_frame(number)
        else:
            bgr = cv2.imread(str(SOURCE / f"frame_{number:03d}.png"), cv2.IMREAD_COLOR)
            if bgr is None:
                raise FileNotFoundError(number)
            extended = extend_background(bgr)
            image = Image.fromarray(cv2.cvtColor(extended, cv2.COLOR_BGR2RGB))
        image.save(FRAMES / f"frame_{number:03d}.png")
        images.append(image)

    images[0].save(
        GIF,
        save_all=True,
        append_images=images[1:],
        # GIF delay is quantized to 10 ms.  20/20/10 ms repeats average to
        # 16.667 ms, so 120 source frames still play for exactly two seconds.
        duration=[20, 20, 10] * 40,
        loop=0,
        optimize=False,
        disposal=2,
    )
    writer = cv2.VideoWriter(
        str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (NEW_WIDTH, images[0].height)
    )
    for image in images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()
    (OUTPUT / "repair_notes.txt").write_text(
        "Frames 021-037: reconstructed the first-thrust blade and point beyond the clipped source edge.\n"
        "All frames: expanded from 1112x834 to 1200x834, preserving character coordinates.\n",
        encoding="utf-8",
    )
    print(FRAMES)
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
