from pathlib import Path

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\attack_sword_frame_animation_v1")
STRIPS = [
    ROOT / "attack_sword_strip_01_05.png",
    ROOT / "attack_sword_strip_06_10.png",
    ROOT / "attack_sword_strip_11_15.png",
]
FRAMES = ROOT / "frames"
CANVAS_SIZE = (1024, 768)


def main() -> None:
    FRAMES.mkdir(parents=True, exist_ok=True)
    output_frames: list[Image.Image] = []

    for strip_index, strip_path in enumerate(STRIPS):
        strip = Image.open(strip_path).convert("RGBA")
        width, height = strip.size

        pixels = np.array(strip)
        alpha = pixels[:, :, 3]
        count, labels, stats, centroids = cv2.connectedComponentsWithStats(
            (alpha > 24).astype(np.uint8), connectivity=8
        )
        components = [
            label
            for label in range(1, count)
            if stats[label, cv2.CC_STAT_AREA] > 5000
        ]
        components.sort(key=lambda label: centroids[label][0])
        if len(components) != 5:
            raise RuntimeError(f"{strip_path.name}: expected 5 sprite components, found {len(components)}")

        for cell_index, label in enumerate(components):
            isolated = np.zeros_like(pixels)
            isolated[labels == label] = pixels[labels == label]
            sprite = Image.fromarray(isolated, "RGBA")

            canvas = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
            nominal_center = (cell_index + 0.5) * width / 5
            x = round(CANVAS_SIZE[0] / 2 - nominal_center)
            y = (CANVAS_SIZE[1] - height) // 2
            canvas.alpha_composite(sprite, (x, y))

            frame_number = strip_index * 5 + cell_index + 1
            output_path = FRAMES / f"attack_sword_{frame_number:02d}.png"
            canvas.save(output_path)
            output_frames.append(canvas)

    preview_frames = [frame.copy() for frame in output_frames]
    preview_frames[0].save(
        ROOT / "attack_sword_15frame_preview.gif",
        save_all=True,
        append_images=preview_frames[1:],
        duration=53,
        loop=0,
        disposal=2,
        transparency=0,
    )

    sheet = Image.new("RGBA", (CANVAS_SIZE[0] * 5, CANVAS_SIZE[1] * 3), (0, 0, 0, 0))
    for index, frame in enumerate(output_frames):
        sheet.alpha_composite(frame, ((index % 5) * CANVAS_SIZE[0], (index // 5) * CANVAS_SIZE[1]))
    sheet.save(ROOT / "attack_sword_15frame_final_sheet.png")


if __name__ == "__main__":
    main()
