from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_special" / "keyframes_v1"
OUTPUT = SOURCE / "purple_1536"

# Uniform background for video generation and later chroma-key extraction.
PURPLE = (126, 24, 190, 255)
CANVAS = (1536, 1536)
# Viewer-left shoe (the character's anatomical right foot) measured from the
# untouched purple start/end frame after its uniform scale and placement.
RIGHT_FOOT_TARGET_X = 481

# output name, source path, scale, foreground-bottom anchor,
# right-foot x in source pixels (None keeps centered), crop alpha bbox
FRAMES = [
    ("cast_special_00_start_purple.png", ROOT / "art_candidates" / "video_source" / "cast_v1" / "refs" / "cast_first_transparent.png", 1.55, 1490, None, True),
    ("cast_special_01_purple.png", SOURCE / "middle_01_ground_ready_tied.png", 1.00, 1490, 693.0, False),
    ("cast_special_02_purple.png", SOURCE / "middle_02_airborne_chant_tied.png", 1.00, 1370, 688.0, False),
    ("cast_special_03_purple.png", SOURCE / "middle_03_left_shoulder_charge_upright_side_v4_clean.png", 0.61, 1370, 425.5, False),
    ("cast_special_04_purple.png", SOURCE / "middle_04_airborne_slash_finish_upright_side_v5_clean.png", 0.61, 1370, 620.5, False),
    ("cast_special_05_purple.png", SOURCE / "middle_05_grounded_finish_corrected_v2_clean.png", 0.61, 1490, 591.6, False),
    ("cast_special_06_end_purple.png", ROOT / "art_candidates" / "video_source" / "cast_v1" / "refs" / "cast_last_transparent.png", 1.55, 1490, None, True),
]


def prepare(
    source: Path,
    scale: float,
    foreground_bottom: int,
    right_foot_x: float | None,
    crop_foreground: bool,
) -> Image.Image:
    image = Image.open(source).convert("RGBA")
    if crop_foreground:
        bbox = image.getchannel("A").getbbox()
        if bbox is None:
            raise RuntimeError(f"No visible pixels: {source}")
        image = image.crop(bbox)
    if scale != 1.0:
        size = (round(image.width * scale), round(image.height * scale))
        image = image.resize(size, Image.Resampling.LANCZOS)

    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        raise RuntimeError(f"No visible pixels: {source}")

    if right_foot_x is None:
        x = (CANVAS[0] - image.width) // 2
    else:
        x = RIGHT_FOOT_TARGET_X - round(right_foot_x * scale)
    y = foreground_bottom - bbox[3]
    canvas = Image.new("RGBA", CANVAS, PURPLE)
    canvas.alpha_composite(image, (x, y))
    return canvas.convert("RGB")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for output_name, source, scale, bottom, right_foot_x, crop_foreground in FRAMES:
        result = prepare(source, scale, bottom, right_foot_x, crop_foreground)
        target = OUTPUT / output_name
        result.save(target, optimize=True)
        print(target)


if __name__ == "__main__":
    main()
