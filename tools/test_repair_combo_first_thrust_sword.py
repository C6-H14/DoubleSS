from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v4_new_video_2s" / "frames_120f_60fps"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v4_new_video_2s" / "sword_extension_test"
TEST_FRAMES = (21, 29, 37)
NEW_WIDTH = 1200
TIP_X = 1170


def estimate_sword_line(image: np.ndarray) -> tuple[float, float]:
    """Fit the neutral-bright sword pixels close to the clipped right edge."""
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
    yy, xx = np.indices(image.shape[:2])
    mask = (
        (xx >= 900)
        & (yy >= 150)
        & (yy <= 380)
        & (hsv[:, :, 1] < 90)
        & (hsv[:, :, 2] > 105)
    )
    ys, xs = np.where(mask)
    slope, intercept = np.polyfit(xs, ys, 1)
    return float(slope), float(intercept)


def repair_frame(number: int) -> Image.Image:
    src = cv2.imread(str(SOURCE / f"frame_{number:03d}.png"), cv2.IMREAD_COLOR)
    if src is None:
        raise FileNotFoundError(number)
    height, width = src.shape[:2]
    slope, intercept = estimate_sword_line(src)

    # Remove the final clipped blade segment, reconstruct its purple backdrop,
    # then extend that clean edge.  This avoids both a vertical colour seam and
    # the horizontal streak caused by copying the blade-bearing last column.
    clean = src.copy()
    inpaint_mask = np.zeros((height, width), dtype=np.uint8)
    erase_x0 = width - 55
    erase_y0 = int(round(slope * erase_x0 + intercept))
    erase_y1 = int(round(slope * (width - 1) + intercept))
    cv2.line(inpaint_mask, (erase_x0, erase_y0), (width - 1, erase_y1), 255, 25, cv2.LINE_AA)
    clean = cv2.inpaint(clean, inpaint_mask, 9, cv2.INPAINT_TELEA)
    canvas = cv2.copyMakeBorder(clean, 0, 0, 0, NEW_WIDTH - width, cv2.BORDER_REPLICATE)
    canvas[:, :erase_x0] = src[:, :erase_x0]

    x0 = erase_x0 - 3
    x1 = TIP_X - 10
    y0 = int(round(slope * x0 + intercept))
    y1 = int(round(slope * x1 + intercept))

    # Continue the existing blade in three restrained layers: outline, steel, highlight.
    cv2.line(canvas, (x0, y0 + 1), (x1, y1 + 1), (55, 54, 58), 5, cv2.LINE_AA)
    cv2.line(canvas, (x0, y0), (x1, y1), (166, 169, 172), 3, cv2.LINE_AA)
    cv2.line(canvas, (x0, y0 - 1), (x1, y1 - 1), (238, 238, 232), 1, cv2.LINE_AA)

    # Epee point: a short narrow neck and rounded button, aligned with the blade.
    y2 = int(round(slope * TIP_X + intercept))
    cv2.line(canvas, (x1 - 1, y1), (TIP_X - 2, y2), (64, 62, 66), 5, cv2.LINE_AA)
    cv2.line(canvas, (x1 - 1, y1 - 1), (TIP_X - 2, y2 - 1), (202, 201, 195), 2, cv2.LINE_AA)
    cv2.ellipse(canvas, (TIP_X, y2), (4, 3), 0, 0, 360, (68, 66, 70), -1, cv2.LINE_AA)
    cv2.ellipse(canvas, (TIP_X, y2 - 1), (2, 2), 0, 0, 360, (224, 222, 214), -1, cv2.LINE_AA)

    out = OUTPUT / f"frame_{number:03d}_repaired.png"
    cv2.imwrite(str(out), canvas)
    return Image.fromarray(cv2.cvtColor(canvas, cv2.COLOR_BGR2RGB))


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    repaired = [repair_frame(number) for number in TEST_FRAMES]

    crop = (850, 190, NEW_WIDTH, 320)
    scale = 2
    label_h = 30
    tile_w = (crop[2] - crop[0]) * scale
    tile_h = (crop[3] - crop[1]) * scale
    sheet = Image.new("RGB", (tile_w, (tile_h + label_h) * len(repaired)), (24, 24, 24))
    draw = ImageDraw.Draw(sheet)
    for row, (number, image) in enumerate(zip(TEST_FRAMES, repaired)):
        y = row * (tile_h + label_h)
        draw.text((8, y + 7), f"frame {number:03d} - repaired", fill="white")
        tile = image.crop(crop).resize((tile_w, tile_h), Image.Resampling.NEAREST)
        sheet.paste(tile, (0, y + label_h))
    contact = OUTPUT / "repair_test_contact.png"
    sheet.save(contact)
    print(contact)


if __name__ == "__main__":
    main()
