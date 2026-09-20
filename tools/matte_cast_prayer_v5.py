from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
WORK = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v5"
SOURCE = WORK / "frames"
OUTPUT = WORK / "alpha_frames"
QA = WORK / "alpha_qa"


def matte_frame(path: Path) -> Image.Image:
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, _, v = cv2.split(hsv)
    rf, gf, bf = (rgb[..., i].astype(np.float32) for i in range(3))
    magenta_bias = (rf + bf) * 0.5 - gf

    # Generated backgrounds contain both bright pink and dark radial compression
    # texture. Key the whole magenta hue band without a brightness gate; the old
    # brightness multiplier retained the dark pattern as opaque debris. Skin is
    # near hue 0, while hair/light are yellow and therefore remain untouched.
    hue_gate = np.clip((h.astype(np.float32) - 134.0) / 12.0, 0.0, 1.0)
    pink_strength = np.clip((magenta_bias - 0.0) / 20.0, 0.0, 1.0) * hue_gate
    alpha = np.uint8(np.round((1.0 - pink_strength) * 255.0))
    alpha[alpha < 8] = 0
    alpha[alpha > 249] = 255

    solid = alpha >= 248
    if not solid.any():
        raise RuntimeError(f"No solid foreground in {path}")
    _, nearest = distance_transform_edt(~solid, return_indices=True)
    clean = rgb.copy()
    fringe = (alpha > 0) & (alpha < 248)
    nearest_rgb = rgb[nearest[0], nearest[1]]
    clean[fringe] = nearest_rgb[fringe]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def composite(image: Image.Image, color):
    return Image.alpha_composite(Image.new("RGBA", image.size, color), image).convert("RGB")


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    if QA.exists():
        shutil.rmtree(QA)
    OUTPUT.mkdir(parents=True)
    QA.mkdir(parents=True)

    frames = []
    for index, source in enumerate(sorted(SOURCE.glob("cast_prayer_*.png")), 1):
        frame = matte_frame(source)
        frame.save(OUTPUT / f"cast_prayer_{index:03d}.png", optimize=True)
        frames.append(frame)
    if len(frames) != 31:
        raise RuntimeError(f"Expected 31 frames, found {len(frames)}")

    picks = np.linspace(0, len(frames) - 1, 16).round().astype(int)
    thumb = (256, 256)
    cols = 4
    rows = (len(picks) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb[0], rows * (thumb[1] * 2 + 24)), "#222")
    draw = ImageDraw.Draw(sheet)
    for slot, frame_index in enumerate(picks):
        frame = frames[int(frame_index)]
        x = slot % cols * thumb[0]
        y = slot // cols * (thumb[1] * 2 + 24)
        sheet.paste(composite(frame, (0, 0, 0, 255)).resize(thumb, Image.Resampling.LANCZOS), (x, y))
        sheet.paste(composite(frame, (255, 255, 255, 255)).resize(thumb, Image.Resampling.LANCZOS), (x, y + thumb[1]))
        draw.text((x + 4, y + thumb[1] * 2 + 4), f"frame {int(frame_index)+1:03d}", fill="white")
    sheet.save(QA / "alpha_contact_black_white.jpg", quality=95)

    for name, bg in (("black", (0, 0, 0, 255)), ("white", (255, 255, 255, 255))):
        preview = [composite(f, bg).resize((512, 512), Image.Resampling.LANCZOS) for f in frames]
        preview[0].save(QA / f"preview_{name}_60fps.gif", save_all=True, append_images=preview[1:], duration=17, loop=0, disposal=2)
    print(f"frames={len(frames)}")
    print(f"output={OUTPUT}")
    print(f"qa={QA}")


if __name__ == "__main__":
    main()
