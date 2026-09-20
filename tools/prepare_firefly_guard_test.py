from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\firefly_tests\defend_guard_v1")
CANVAS = (1400, 1320)
GROUND_Y = 1080
CENTER_X = 723


def main():
    raw = Image.open(ROOT / "end_frame_raw.png").convert("RGBA")
    bbox = raw.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError("Generated end frame is empty")
    crop = raw.crop(bbox)
    scale = 0.76
    crop = crop.resize(
        (round(crop.width * scale), round(crop.height * scale)),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", CANVAS, (0, 0, 0, 0))
    x = round(CENTER_X - crop.width / 2)
    y = GROUND_Y - crop.height
    canvas.alpha_composite(crop, (x, y))
    canvas.save(ROOT / "end_frame.png", optimize=True)
    print(ROOT / "end_frame.png")


if __name__ == "__main__":
    main()
