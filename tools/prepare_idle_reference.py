from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "character_runtime_v24_hurt" / "images" / "attack_sword_01.png"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "references"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SOURCE).convert("RGBA")
    source.save(OUT / "idle_anchor_transparent.png", optimize=True)
    pink = Image.new("RGBA", source.size, (255, 0, 128, 255))
    pink.alpha_composite(source)
    pink.convert("RGB").save(OUT / "idle_anchor_pink.png", quality=100)
    black = Image.new("RGBA", source.size, (0, 0, 0, 255))
    black.alpha_composite(source)
    black.convert("RGB").save(OUT / "idle_anchor_black.png", quality=100)
    print(OUT)


if __name__ == "__main__":
    main()
