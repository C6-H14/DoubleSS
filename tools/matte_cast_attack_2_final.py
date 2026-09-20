from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
WORK = ROOT / "art_candidates" / "video_source" / "cast_attack_2_final_cut"
SOURCE = WORK / "frames_115s_30fps"
OUTPUT = WORK / "alpha_frames_115s_30fps"
QA = WORK / "alpha_qa_115s"


def matte_frame(path: Path) -> Image.Image:
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, _, v = cv2.split(hsv)

    rf = rgb[..., 0].astype(np.float32)
    gf = rgb[..., 1].astype(np.float32)
    bf = rgb[..., 2].astype(np.float32)
    magenta_bias = (rf + bf) * 0.5 - gf

    # The generated background occupies the narrow high-magenta band around
    # OpenCV hue 150..179. Purple sword light remains below this band.
    hue_gate = np.clip((h.astype(np.float32) - 147.0) / 9.0, 0.0, 1.0)
    pink_strength = np.clip((magenta_bias - 1.0) / 27.0, 0.0, 1.0) * hue_gate
    pink_strength *= np.clip((v.astype(np.float32) - 24.0) / 68.0, 0.0, 1.0)
    alpha = np.uint8(np.round((1.0 - pink_strength) * 255.0))
    alpha[alpha < 8] = 0
    alpha[alpha > 249] = 255

    # Remove pink contamination from partially transparent edge texels. This
    # is important for Spine's linear texture filtering on dark backgrounds.
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


def composite(image: Image.Image, color: tuple[int, int, int, int]) -> Image.Image:
    return Image.alpha_composite(Image.new("RGBA", image.size, color), image).convert("RGB")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    QA.mkdir(parents=True, exist_ok=True)
    for old in OUTPUT.glob("*.png"):
        old.unlink()

    sources = sorted(SOURCE.glob("frame_*.png"))
    if not sources:
        raise RuntimeError(f"No source frames in {SOURCE}")

    frames: list[Image.Image] = []
    for index, source in enumerate(sources, 1):
        frame = matte_frame(source)
        frame.save(OUTPUT / f"cast_attack_2_{index:03d}.png", optimize=True)
        frames.append(frame)

    picks = np.linspace(0, len(frames) - 1, min(12, len(frames))).round().astype(int)
    thumb = (278, 209)
    cols = 6
    rows = (len(picks) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb[0], rows * (thumb[1] * 2 + 24)), "#202020")
    draw = ImageDraw.Draw(sheet)
    for slot, frame_index in enumerate(picks):
        frame = frames[int(frame_index)]
        x = (slot % cols) * thumb[0]
        y = (slot // cols) * (thumb[1] * 2 + 24)
        black = composite(frame, (0, 0, 0, 255)).resize(thumb, Image.Resampling.LANCZOS)
        white = composite(frame, (255, 255, 255, 255)).resize(thumb, Image.Resampling.LANCZOS)
        sheet.paste(black, (x, y))
        sheet.paste(white, (x, y + thumb[1]))
        draw.text((x + 5, y + thumb[1] * 2 + 4), f"frame {int(frame_index) + 1:03d}", fill="white")
    sheet.save(QA / "alpha_contact_black_white.jpg", quality=95)

    # Animated QA previews make temporal edge flicker easy to spot.
    black_frames = [composite(frame, (0, 0, 0, 255)).resize((556, 417), Image.Resampling.LANCZOS) for frame in frames]
    white_frames = [composite(frame, (255, 255, 255, 255)).resize((556, 417), Image.Resampling.LANCZOS) for frame in frames]
    duration_ms = round(1000 / 30)
    black_frames[0].save(QA / "preview_black.gif", save_all=True, append_images=black_frames[1:], duration=duration_ms, loop=0, disposal=2)
    white_frames[0].save(QA / "preview_white.gif", save_all=True, append_images=white_frames[1:], duration=duration_ms, loop=0, disposal=2)
    print(f"frames={len(frames)}")
    print(f"output={OUTPUT}")
    print(f"qa={QA}")


if __name__ == "__main__":
    main()
