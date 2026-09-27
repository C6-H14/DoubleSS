from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_idle" / "idle_start_chroma_v3_tonematched.png"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_basic_attack"
START = OUTPUT / "basic_attack_start_chroma_2048x1536.png"
END = OUTPUT / "basic_attack_end_chroma_2048x1536.png"

CANVAS = (2048, 1536)
PASTE = (128, 0)


def main() -> None:
    source = Image.open(SOURCE).convert("RGB")
    if source.size != (1536, 1536):
        raise RuntimeError(f"Expected 1536x1536 source, got {source.size}")

    # Use the exact source corner color so the extension is chroma-key uniform.
    background = source.getpixel((0, 0))
    result = Image.new("RGB", CANVAS, background)
    result.paste(source, PASTE)

    OUTPUT.mkdir(parents=True, exist_ok=True)
    result.save(START)
    result.save(END)

    print(f"source={SOURCE}")
    print(f"source_size={source.size} canvas={CANVAS} paste={PASTE} background={background}")
    print(START)
    print(END)


if __name__ == "__main__":
    main()
