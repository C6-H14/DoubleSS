from pathlib import Path
import json
import shutil

from PIL import Image, ImageDraw

from matte_cast_prayer_v8_border_connected import border_connected_matte, checkerboard


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_special" / "combined_preview" / "trimmed_v2_75f_50fps" / "frames"
OUTPUT = ROOT / "art_candidates" / "video_source" / "cast_special" / "final_v1_alpha"
FRAMES = OUTPUT / "alpha_frames"
EXPECTED = 75


def composite(frame: Image.Image, color: tuple[int, int, int, int]) -> Image.Image:
    return Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} source frames, found {len(paths)}")

    frames = []
    for index, path in enumerate(paths, 1):
        frame = border_connected_matte(path)
        frame.save(FRAMES / f"cast_special_{index:03d}.png", optimize=True)
        frames.append(frame)

    # Full-speed QA previews on backgrounds that expose both dark edge noise
    # and accidental transparent holes.
    for name, color in (("black", (0, 0, 0, 255)), ("white", (255, 255, 255, 255))):
        rendered = [composite(frame, color).resize((480, 480), Image.Resampling.LANCZOS).convert("RGB")
                    for frame in frames]
        rendered[0].save(
            OUTPUT / f"cast_special_{name}_75f_50fps.gif",
            save_all=True,
            append_images=rendered[1:],
            duration=20,
            loop=0,
            disposal=2,
            optimize=False,
        )

    columns = 10
    thumb = 160
    rows = (len(frames) + columns - 1) // columns
    for name, background in (
        ("black", Image.new("RGBA", frames[0].size, (0, 0, 0, 255))),
        ("white", Image.new("RGBA", frames[0].size, (255, 255, 255, 255))),
        ("checker", checkerboard(frames[0].size, 32)),
    ):
        sheet = Image.new("RGB", (columns * thumb, rows * (thumb + 20)), (24, 24, 24))
        draw = ImageDraw.Draw(sheet)
        for i, frame in enumerate(frames):
            view = Image.alpha_composite(background.copy(), frame)
            view = view.resize((thumb, thumb), Image.Resampling.LANCZOS).convert("RGB")
            x = (i % columns) * thumb
            y = (i // columns) * (thumb + 20)
            sheet.paste(view, (x, y))
            draw.text((x + 4, y + thumb + 2), f"{i + 1:03d}", fill="white")
        sheet.save(OUTPUT / f"cast_special_{name}_contact_sheet.jpg", quality=96)

    (OUTPUT / "metadata.json").write_text(
        json.dumps({"frames": EXPECTED, "fps": 50, "duration": 1.5,
                    "matte": "border-connected purple with interior edge RGB decontamination"}, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"frames={len(frames)} output={OUTPUT}")


if __name__ == "__main__":
    main()
