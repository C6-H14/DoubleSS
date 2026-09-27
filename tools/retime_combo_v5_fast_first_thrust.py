from pathlib import Path

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v4_first_thrust_repaired" / "frames_120f_60fps"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v5_fast_first_thrust"
FRAMES = OUTPUT / "frames_103f_60fps"
GIF = OUTPUT / "combo_attack_v5_103f_60fps.gif"
MP4 = OUTPUT / "combo_attack_v5_103f_60fps.mp4"
FPS = 60.0


def main() -> None:
    # Keep anticipation (1-12), play the complete first thrust (13-47) at
    # approximately 2x speed, then preserve the three quick thrusts/recovery.
    selected = list(range(1, 13)) + list(range(13, 48, 2)) + list(range(48, 121))
    if len(selected) != 103:
        raise RuntimeError(len(selected))
    FRAMES.mkdir(parents=True, exist_ok=True)

    images: list[Image.Image] = []
    for output_number, source_number in enumerate(selected, 1):
        image = Image.open(SOURCE / f"frame_{source_number:03d}.png").convert("RGB")
        image.save(FRAMES / f"frame_{output_number:03d}.png")
        images.append(image)

    # Exact 60 FPS average within GIF's 10 ms delay granularity.
    durations = ([20, 20, 10] * 35)[: len(images)]
    # Correct the truncated final pattern to the nearest 1/60-second total.
    target_ms = round(len(images) * 1000 / FPS / 10) * 10
    durations[-1] += target_ms - sum(durations)
    images[0].save(
        GIF,
        save_all=True,
        append_images=images[1:],
        duration=durations,
        loop=0,
        optimize=False,
        disposal=2,
    )

    width, height = images[0].size
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for image in images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()
    (OUTPUT / "frame_mapping.txt").write_text(
        "\n".join(f"{i:03d} <- source {source:03d}" for i, source in enumerate(selected, 1)),
        encoding="utf-8",
    )
    samples = [images[i] for i in range(0, len(images), 5)]
    thumb_w, thumb_h = 400, 278
    sheet = Image.new("RGB", (thumb_w * 5, thumb_h * 5), (28, 28, 28))
    for slot, image in enumerate(samples):
        thumb = image.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        sheet.paste(thumb, ((slot % 5) * thumb_w, (slot // 5) * thumb_h))
    sheet.save(OUTPUT / "contact_every_5_frames.jpg", quality=92)
    print(f"frames={len(images)} duration={len(images) / FPS:.6f}s")
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
