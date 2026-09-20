from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw

from matte_cast_prayer_v5 import matte_frame, composite


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "cast_prayer"
SOURCE = BASE / "preview_v5" / "frames"
OUTPUT = BASE / "preview_v6"
PINK = OUTPUT / "frames"
ALPHA = OUTPUT / "alpha_frames"
QA = OUTPUT / "alpha_qa"


def optical_midpoint(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    gray_a = cv2.cvtColor(a, cv2.COLOR_BGR2GRAY)
    gray_b = cv2.cvtColor(b, cv2.COLOR_BGR2GRAY)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    flow_ab = dis.calc(gray_a, gray_b, None)
    flow_ba = dis.calc(gray_b, gray_a, None)
    h, w = gray_a.shape
    xx, yy = np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))
    wa = cv2.remap(a, xx - flow_ab[..., 0] * 0.5, yy - flow_ab[..., 1] * 0.5, cv2.INTER_LANCZOS4,
                   borderMode=cv2.BORDER_REFLECT)
    wb = cv2.remap(b, xx - flow_ba[..., 0] * 0.5, yy - flow_ba[..., 1] * 0.5, cv2.INTER_LANCZOS4,
                   borderMode=cv2.BORDER_REFLECT)
    return cv2.addWeighted(wa, 0.5, wb, 0.5, 0.0)


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    PINK.mkdir(parents=True)
    ALPHA.mkdir(parents=True)
    QA.mkdir(parents=True)

    source_paths = sorted(SOURCE.glob("cast_prayer_*.png"))
    if len(source_paths) != 31:
        raise RuntimeError(f"Expected 31 source frames, found {len(source_paths)}")
    source_frames = [cv2.imread(str(path)) for path in source_paths]

    dense = []
    inserted_after = []
    for index, frame in enumerate(source_frames, 1):
        dense.append(frame)
        if index % 3 == 0 and index < len(source_frames):
            dense.append(optical_midpoint(frame, source_frames[index]))
            inserted_after.append(index)

    alpha_frames = []
    for index, frame in enumerate(dense, 1):
        pink_path = PINK / f"cast_prayer_{index:03d}.png"
        cv2.imwrite(str(pink_path), frame)
        alpha = matte_frame(pink_path)
        alpha.save(ALPHA / f"cast_prayer_{index:03d}.png", optimize=True)
        alpha_frames.append(alpha)

    for name, bg in (("black", (0, 0, 0, 255)), ("white", (255, 255, 255, 255))):
        preview = [composite(f, bg).resize((512, 512), Image.Resampling.LANCZOS) for f in alpha_frames]
        preview[0].save(QA / f"preview_{name}_60fps.gif", save_all=True, append_images=preview[1:],
                        duration=17, loop=0, disposal=2)

    picks = np.linspace(0, len(alpha_frames) - 1, 20).round().astype(int)
    thumb, cols = 256, 5
    rows = (len(picks) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb, rows * (thumb * 2 + 24)), "#222")
    draw = ImageDraw.Draw(sheet)
    for slot, picked in enumerate(picks):
        frame = alpha_frames[int(picked)]
        x, y = slot % cols * thumb, slot // cols * (thumb * 2 + 24)
        sheet.paste(composite(frame, (0, 0, 0, 255)).resize((thumb, thumb), Image.Resampling.LANCZOS), (x, y))
        sheet.paste(composite(frame, (255, 255, 255, 255)).resize((thumb, thumb), Image.Resampling.LANCZOS), (x, y + thumb))
        draw.text((x + 4, y + thumb * 2 + 4), f"frame {int(picked)+1:03d}", fill="white")
    sheet.save(QA / "alpha_contact_black_white.jpg", quality=95)
    print(f"frames={len(dense)}")
    print(f"inserted_after={inserted_after}")


if __name__ == "__main__":
    main()
