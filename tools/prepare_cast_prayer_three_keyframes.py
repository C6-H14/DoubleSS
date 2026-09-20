from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "attack_sword_v2_final39_swordfixed_v6" / "frames" / "attack_sword_01.png"
KEYFRAMES = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "keyframes"
MIDDLE = KEYFRAMES / "cast_prayer_middle_kneel_s_rune_v1.png"
START = KEYFRAMES / "cast_prayer_start_1254.png"
END = KEYFRAMES / "cast_prayer_end_1254.png"
CONTACT = KEYFRAMES / "cast_prayer_three_keyframes_contact.jpg"
TARGET_SIZE = (1254, 1254)
TARGET_HEIGHT = 760
TARGET_BOTTOM = 1080


def premultiplied_resize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    rgba[..., :3] *= rgba[..., 3:4]
    channels = []
    for channel in range(4):
        plane = Image.fromarray(np.uint8(np.round(rgba[..., channel] * 255)), "L")
        channels.append(np.asarray(plane.resize(size, Image.Resampling.LANCZOS), dtype=np.float32) / 255.0)
    premul = np.stack(channels, axis=2)
    alpha = np.clip(premul[..., 3:4], 0, 1)
    rgb = np.divide(premul[..., :3], alpha, out=np.zeros_like(premul[..., :3]), where=alpha > 1e-6)
    return Image.fromarray(np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), alpha), axis=2) * 255)), "RGBA")


def main() -> None:
    KEYFRAMES.mkdir(parents=True, exist_ok=True)
    middle = Image.open(MIDDLE).convert("RGB")
    if middle.size != TARGET_SIZE:
        raise RuntimeError(f"Middle frame is {middle.size}, expected {TARGET_SIZE}")
    pink = middle.getpixel((0, 0))

    source = Image.open(SOURCE).convert("RGBA")
    bbox = source.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox()
    if bbox is None:
        raise RuntimeError("Start frame has no visible pixels")
    subject = source.crop(bbox)
    target_width = round(subject.width * TARGET_HEIGHT / subject.height)
    subject = premultiplied_resize(subject, (target_width, TARGET_HEIGHT))

    frame = Image.new("RGBA", TARGET_SIZE, (*pink, 255))
    x = (TARGET_SIZE[0] - target_width) // 2
    y = TARGET_BOTTOM - TARGET_HEIGHT
    frame.alpha_composite(subject, (x, y))
    frame = frame.convert("RGB")
    frame.save(START, quality=100)
    frame.save(END, quality=100)

    thumb = 418
    sheet = Image.new("RGB", (thumb * 3, thumb), "white")
    draw = ImageDraw.Draw(sheet)
    for index, (label, image) in enumerate((("START", frame), ("MIDDLE", middle), ("END", frame))):
        preview = image.resize((thumb, thumb), Image.Resampling.LANCZOS)
        sheet.paste(preview, (index * thumb, 0))
        draw.rectangle((index * thumb, 0, index * thumb + 95, 28), fill="black")
        draw.text((index * thumb + 8, 7), label, fill="white")
    sheet.save(CONTACT, quality=95)
    print(f"pink={pink}; start={START}; middle={MIDDLE}; end={END}")


if __name__ == "__main__":
    main()
