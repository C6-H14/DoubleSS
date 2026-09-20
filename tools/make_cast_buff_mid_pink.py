from pathlib import Path

from PIL import Image


SOURCE = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\cast_buff_v1\refs\cast_buff_mid_transparent_v2.png")
OUTPUT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\cast_buff_v1\refs\cast_buff_mid_pink_v2.png")
PINK = (244, 72, 148, 255)


def main() -> None:
    foreground = Image.open(SOURCE).convert("RGBA")
    background = Image.new("RGBA", foreground.size, PINK)
    background.alpha_composite(foreground)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    background.convert("RGB").save(OUTPUT, quality=100)


if __name__ == "__main__":
    main()
