from pathlib import Path
import shutil

from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v7_stable_clean" / "alpha_frames"
OUTPUT = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "review_current_game_v7"
FRAMES = OUTPUT / "alpha_frames"


def checkerboard(size, cell=32):
    image = Image.new("RGBA", size, (220, 220, 220, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], cell):
        for x in range(0, size[0], cell):
            if (x // cell + y // cell) % 2:
                draw.rectangle((x, y, x + cell - 1, y + cell - 1), fill=(165, 165, 165, 255))
    return image


def composite(frame, background):
    return Image.alpha_composite(background.copy(), frame)


def main():
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("cast_prayer_*.png"))
    frames = [Image.open(path).convert("RGBA") for path in paths]
    if len(frames) != 41:
        raise RuntimeError(f"Expected 41 frames, found {len(frames)}")
    for path in paths:
        shutil.copy2(path, FRAMES / path.name)

    # Native-resolution transparent sprite sheet: 7 columns x 6 rows.
    w, h = frames[0].size
    cols, rows = 7, 6
    sheet = Image.new("RGBA", (cols * w, rows * h), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        sheet.alpha_composite(frame, ((i % cols) * w, (i // cols) * h))
    sheet.save(OUTPUT / "cast_prayer_alpha_sheet_7x6.png", optimize=True)

    # A labelled checkerboard overview at a manageable inspection size.
    thumb = 320
    overview = Image.new("RGB", (cols * thumb, rows * (thumb + 28)), "#222")
    draw = ImageDraw.Draw(overview)
    for i, frame in enumerate(frames):
        x, y = (i % cols) * thumb, (i // cols) * (thumb + 28)
        bg = checkerboard(frame.size)
        tile = composite(frame, bg).resize((thumb, thumb), Image.Resampling.LANCZOS).convert("RGB")
        overview.paste(tile, (x, y))
        draw.text((x + 5, y + thumb + 5), f"frame {i+1:02d}", fill="white")
    overview.save(OUTPUT / "cast_prayer_checker_contact.jpg", quality=96)

    # 41 frames / 1.2 s = 29.27 ms per frame. GIF uses integer milliseconds.
    duration = 29
    resized = [frame.resize((512, 512), Image.Resampling.LANCZOS) for frame in frames]
    resized[0].save(OUTPUT / "cast_prayer_alpha_1200ms.gif", save_all=True, append_images=resized[1:],
                    duration=duration, loop=0, disposal=2, transparency=0)
    for name, color in (("black", (0, 0, 0, 255)), ("checker", None)):
        rendered = []
        for frame in frames:
            bg = Image.new("RGBA", frame.size, color) if color else checkerboard(frame.size)
            rendered.append(composite(frame, bg).resize((512, 512), Image.Resampling.LANCZOS).convert("RGB"))
        rendered[0].save(OUTPUT / f"cast_prayer_{name}_1200ms.gif", save_all=True,
                         append_images=rendered[1:], duration=duration, loop=0, disposal=2)
    print(f"frames={len(frames)} output={OUTPUT}")


if __name__ == "__main__":
    main()
