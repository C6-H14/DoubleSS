from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\attack_sword_v2_selected18")
INPUT_DIR = ROOT / "raw_frames"
OUTPUT_DIR = ROOT / "transparent_frames"
CONTACT_SHEET = ROOT / "transparent_contact_sheet.png"


def remove_background(source: Path, target: Path) -> None:
    rgb = Image.open(source).convert("RGB")
    width, height = rgb.size

    # Flood only areas connected to the canvas boundary. This preserves enclosed
    # whites such as socks, eyes, and sword highlights.
    flood = rgb.copy()
    marker = (255, 0, 255)
    seeds = [
        (0, 0),
        (width - 1, 0),
        (0, height - 1),
        (width - 1, height - 1),
        (width // 2, 0),
        (width // 2, height - 1),
        (0, height // 2),
        (width - 1, height // 2),
    ]
    for seed in seeds:
        if flood.getpixel(seed) != marker:
            ImageDraw.floodfill(flood, seed, marker, thresh=38)

    flooded = np.asarray(flood)
    background = np.all(flooded == marker, axis=2)

    alpha = Image.fromarray(np.where(background, 0, 255).astype(np.uint8), "L")
    # A tiny feather retains the original antialiased outline without a hard cut.
    alpha = alpha.filter(ImageFilter.GaussianBlur(radius=0.65))

    rgba = rgb.convert("RGBA")
    rgba.putalpha(alpha)
    rgba.save(target, optimize=True)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    sources = sorted(INPUT_DIR.glob("attack_sword_*.png"))
    if len(sources) != 18:
        raise RuntimeError(f"Expected 18 input frames, found {len(sources)}")
    for source in sources:
        remove_background(source, OUTPUT_DIR / source.name)
    thumbs = []
    for target in sorted(OUTPUT_DIR.glob("attack_sword_*.png")):
        frame = Image.open(target).convert("RGBA")
        frame.thumbnail((278, 209), Image.Resampling.LANCZOS)
        checker = Image.new("RGBA", frame.size, (220, 220, 220, 255))
        draw = ImageDraw.Draw(checker)
        size = 12
        for y in range(0, frame.height, size):
            for x in range(0, frame.width, size):
                if ((x // size) + (y // size)) % 2:
                    draw.rectangle((x, y, x + size - 1, y + size - 1), fill=(245, 245, 245, 255))
        checker.alpha_composite(frame)
        thumbs.append(checker.convert("RGB"))
    sheet = Image.new("RGB", (6 * 278, 3 * 209), (32, 32, 32))
    for index, thumb in enumerate(thumbs):
        sheet.paste(thumb, ((index % 6) * 278, (index // 6) * 209))
    sheet.save(CONTACT_SHEET, optimize=True)
    print(f"wrote {len(sources)} transparent frames to {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
