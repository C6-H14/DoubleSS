from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
IDLE = ROOT / "art_candidates" / "character_runtime_v34_idle_60fps" / "images"
CAST = ROOT / "art_candidates" / "character_runtime_v45_alt_cast_s_aligned" / "images"
OUT = ROOT / "art_candidates" / "character_runtime_v45_alt_cast_s_aligned" / "alignment_review.png"


def box(alpha: np.ndarray, roi: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    x0, y0, x1, y1 = roi
    yy, xx = np.where(alpha[y0:y1, x0:x1] > 24)
    return x0 + int(xx.min()), y0 + int(yy.min()), x0 + int(xx.max()), y0 + int(yy.max())


def main() -> None:
    files = {
        "idle_001": IDLE / "alt_idle_001.png",
        "idle_241": IDLE / "alt_idle_241.png",
        "cast_001": CAST / "cast_s_alpha_001.png",
        "cast_045": CAST / "cast_s_alpha_045.png",
    }
    for label, path in files.items():
        alpha = np.asarray(Image.open(path).convert("RGBA"))[:, :, 3]
        head = box(alpha, (410, 300, 570, 500))
        left = box(alpha, (340, 680, 460, 760))
        right = box(alpha, (535, 680, 680, 760))
        print(label, "head", head, "left_foot", left, "right_foot", right)

    idle = Image.open(files["idle_001"]).convert("RGBA")
    cast = Image.open(files["cast_045"]).convert("RGBA")
    base = Image.new("RGBA", (1024, 1024), (28, 28, 28, 255))
    base.alpha_composite(idle)
    canvas = base.convert("RGB")
    draw = ImageDraw.Draw(canvas)
    for frame, color in ((idle, (80, 255, 110)), (cast, (255, 70, 70))):
        alpha = np.asarray(frame)[:, :, 3]
        region = alpha[340:745, 340:680]
        edge = cv2.Canny((region > 24).astype(np.uint8) * 255, 50, 150)
        ys, xs = np.where(edge > 0)
        for x, y in zip(xs, ys):
            draw.point((int(x) + 340, int(y) + 340), fill=color)
    crop = canvas.crop((330, 340, 690, 750)).resize((1080, 1230), Image.Resampling.NEAREST)
    crop.save(OUT)


if __name__ == "__main__":
    main()
