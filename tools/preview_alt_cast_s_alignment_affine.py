from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates")
IDLE = ROOT / "character_runtime_v34_idle_60fps" / "images" / "alt_idle_001.png"
CAST = ROOT / "character_runtime_v44_alt_cast_s" / "images" / "cast_s_alpha_045.png"
OUT = ROOT / "character_runtime_v44_alt_cast_s" / "alignment_affine_test.png"
MATRIX = np.float32([[0.932, 0, 38], [0, 0.974, 20]])


def main() -> None:
    idle = np.asarray(Image.open(IDLE).convert("RGBA"))
    cast = np.asarray(Image.open(CAST).convert("RGBA"))
    corrected = cv2.warpAffine(cast, MATRIX, (1024, 1024), flags=cv2.INTER_LINEAR,
                               borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))
    bg = Image.new("RGBA", (1024, 1024), (28, 28, 28, 255))
    bg.alpha_composite(Image.fromarray(idle, "RGBA"))
    canvas = bg.convert("RGB")
    draw = ImageDraw.Draw(canvas)
    for image, color in ((idle, (80, 255, 110)), (corrected, (255, 70, 70))):
        alpha = image[:, :, 3]
        edge = cv2.Canny((alpha[340:745, 340:680] > 24).astype(np.uint8) * 255, 50, 150)
        ys, xs = np.where(edge > 0)
        for x, y in zip(xs, ys):
            draw.point((int(x) + 340, int(y) + 340), fill=color)
    canvas.crop((330, 340, 690, 750)).resize((1080, 1230), Image.Resampling.NEAREST).save(OUT)
    for label, image in (("idle", idle), ("adjusted", corrected)):
        a = image[:, :, 3]
        def box(x0, y0, x1, y1):
            yy, xx = np.where(a[y0:y1, x0:x1] > 24)
            return x0 + int(xx.min()), y0 + int(yy.min()), x0 + int(xx.max()), y0 + int(yy.max())
        print(label, "head", box(410, 300, 570, 500),
              "left", box(340, 680, 460, 760), "right", box(535, 680, 680, 760))


if __name__ == "__main__":
    main()
