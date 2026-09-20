from pathlib import Path
import shutil

from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1")
SOURCE = ROOT / "cast_buff_transition_test_v1" / "pink_frames"
OUTPUT = ROOT / "cast_buff_transition_test_v2"
FRAMES = OUTPUT / "pink_frames"
GENERATED = [
    Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-a72a7571-b2bd-441f-8b5a-ed39622e4db6.png"),
    Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-b0198ecd-e478-451d-9c93-e683ea09d6a9.png"),
]


def main() -> None:
    if FRAMES.exists():
        shutil.rmtree(FRAMES)
    FRAMES.mkdir(parents=True, exist_ok=True)

    source_paths = sorted(SOURCE.glob("cast_buff_*.png"))
    for index, path in enumerate(source_paths, 1):
        out_index = index if index <= 57 else index + 2
        shutil.copy2(path, FRAMES / f"cast_buff_{out_index:03d}.png")

    for out_index, generated_path in zip((58, 59), GENERATED):
        image = Image.open(generated_path).convert("RGB")
        image = image.resize((1120, 832), Image.Resampling.LANCZOS)
        image.save(FRAMES / f"cast_buff_{out_index:03d}.png", compress_level=2)

    paths = sorted(FRAMES.glob("cast_buff_*.png"))
    images = [Image.open(path).convert("RGB") for path in paths]
    images[0].save(
        OUTPUT / "cast_buff_transition_test_v2.gif",
        save_all=True,
        append_images=images[1:],
        duration=20,
        loop=0,
        disposal=2,
    )

    review_indices = (56, 57, 58, 59, 60)
    tile_size = (448, 333)
    strip = Image.new("RGB", (tile_size[0] * 5, tile_size[1] + 24), "#171717")
    draw = ImageDraw.Draw(strip)
    for slot, index in enumerate(review_indices):
        image = Image.open(FRAMES / f"cast_buff_{index:03d}.png").convert("RGB")
        image = image.resize(tile_size, Image.Resampling.LANCZOS)
        strip.paste(image, (slot * tile_size[0], 0))
        draw.text((slot * tile_size[0] + 6, tile_size[1] + 4), f"frame {index}", fill="white")
    strip.save(OUTPUT / "cast_buff_056_to_060_sequence.jpg", quality=94)

    (OUTPUT / "selection.txt").write_text(
        "variant=cast_buff_transition_test_v2\n"
        "base=../cast_buff_transition_test_v1\n"
        "inserted_after=57\ninserted_frames=58,59\n"
        "old_frames_58_to_70_shifted_to=60_to_72\n"
        "frame_count=72\nfps=50\nduration=1.440s\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
