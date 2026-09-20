from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = (
    ROOT
    / "art_candidates"
    / "matting_tests"
    / "matanyone_attack_pink_v1"
    / "selected47_50fps"
    / "transparent_original_endpoint_aligned.png"
)
OUT = ROOT / "art_candidates" / "video_source" / "cast_v1" / "refs"
PINK = (255, 64, 160, 255)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    transparent = Image.open(SOURCE).convert("RGBA")
    pink = Image.new("RGBA", transparent.size, PINK)
    pink.alpha_composite(transparent)
    pink = pink.convert("RGB")

    for endpoint in ("first", "last"):
        transparent.save(OUT / f"cast_{endpoint}_transparent.png", optimize=True)
        pink.save(OUT / f"cast_{endpoint}_pink.png", quality=100, subsampling=0)

    print(OUT)
    print(f"size={transparent.size}")


if __name__ == "__main__":
    main()
