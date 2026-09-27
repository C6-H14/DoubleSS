from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_idle" / "idle_start_chroma_v3_tonematched.png"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack"
START = OUTPUT / "combo_attack_start_safe_2048x1536.png"
END = OUTPUT / "combo_attack_end_safe_2048x1536.png"

CANVAS = (2048, 1536)
SCALE = 0.82
PASTE = (40, 138)


def main() -> None:
    source = Image.open(SOURCE).convert("RGB")
    background = source.getpixel((0, 0))
    resized = source.resize(
        (round(source.width * SCALE), round(source.height * SCALE)),
        Image.Resampling.LANCZOS,
    )
    result = Image.new("RGB", CANVAS, background)
    result.paste(resized, PASTE)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    result.save(START)
    result.save(END)
    print(f"source={source.size} scaled={resized.size} canvas={CANVAS} paste={PASTE}")
    print(START)
    print(END)


if __name__ == "__main__":
    main()
