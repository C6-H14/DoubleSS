from pathlib import Path

import cv2
import numpy as np
from PIL import Image


SOURCE = Path(
    r"D:\StSmod\DoubleSS\art_candidates\attack_sword_frame_animation_v2\attack_sword_frames_09_14_grid_source_v4.png"
)
OUTPUT = SOURCE.with_name("attack_sword_frames_09_14_grid_transparent_v4.png")


def main() -> None:
    image = cv2.imread(str(SOURCE), cv2.IMREAD_COLOR)
    height, width = image.shape[:2]
    cell_width = width // 3
    cell_height = height // 2
    alpha = np.zeros((height, width), dtype=np.uint8)

    for row in range(2):
        for column in range(3):
            x0 = column * cell_width
            y0 = row * cell_height
            cell = image[y0 : y0 + cell_height, x0 : x0 + cell_width]
            mask = np.full(cell.shape[:2], cv2.GC_BGD, dtype=np.uint8)
            rectangle = (3, 3, cell_width - 6, cell_height - 6)
            background_model = np.zeros((1, 65), np.float64)
            foreground_model = np.zeros((1, 65), np.float64)
            cv2.grabCut(
                cell,
                mask,
                rectangle,
                background_model,
                foreground_model,
                8,
                cv2.GC_INIT_WITH_RECT,
            )
            foreground = np.where(
                (mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0
            ).astype(np.uint8)
            foreground = cv2.morphologyEx(
                foreground, cv2.MORPH_OPEN, np.ones((2, 2), np.uint8)
            )
            foreground = cv2.GaussianBlur(foreground, (3, 3), 0.6)
            alpha[y0 : y0 + cell_height, x0 : x0 + cell_width] = foreground

    rgba = cv2.cvtColor(image, cv2.COLOR_BGR2RGBA)
    rgba[:, :, 3] = alpha
    Image.fromarray(rgba, "RGBA").save(OUTPUT)


if __name__ == "__main__":
    main()
