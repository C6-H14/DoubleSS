from pathlib import Path
import shutil

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1")
SOURCE_FRAMES = ROOT / "cast_buff" / "pink_frames"
OUTPUT = ROOT / "cast_buff_transition_test_v1"
OUTPUT_FRAMES = OUTPUT / "pink_frames"
GENERATED = Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-49ac21e2-ce0d-4f37-bd1d-ae2002f8f677.png")


def main() -> None:
    if OUTPUT_FRAMES.exists():
        shutil.rmtree(OUTPUT_FRAMES)
    OUTPUT_FRAMES.mkdir(parents=True, exist_ok=True)

    source_paths = sorted(SOURCE_FRAMES.glob("cast_buff_*.png"))
    output_frames = []
    for index, path in enumerate(source_paths, 1):
        out_index = index if index <= 56 else index + 1
        output_path = OUTPUT_FRAMES / f"cast_buff_{out_index:03d}.png"
        shutil.copy2(path, output_path)
        output_frames.append((out_index, output_path))

    transition = Image.open(GENERATED).convert("RGB")
    transition = transition.resize((1120, 832), Image.Resampling.LANCZOS)
    transition_path = OUTPUT_FRAMES / "cast_buff_057.png"
    transition.save(transition_path, compress_level=2)
    output_frames.append((57, transition_path))
    output_frames.sort()

    images = [Image.open(path).convert("RGB") for _, path in output_frames]
    images[0].save(
        OUTPUT / "cast_buff_transition_test.gif",
        save_all=True,
        append_images=images[1:],
        duration=20,
        loop=0,
        disposal=2,
    )

    # Three-frame strip for judging only the repaired boundary.
    triplet = [Image.open(OUTPUT_FRAMES / f"cast_buff_{i:03d}.png").convert("RGB")
               for i in (56, 57, 58)]
    strip = Image.new("RGB", (560 * 3, 441), "black")
    for slot, image in enumerate(triplet):
        strip.paste(image.resize((560, 416), Image.Resampling.LANCZOS), (slot * 560, 0))
    strip.save(OUTPUT / "cast_buff_056_057_058_triplet.jpg", quality=94)

    (OUTPUT / "selection.txt").write_text(
        "variant=cast_buff_transition_test_v1\n"
        "base=../cast_buff\n"
        "inserted_at=57\n"
        "old_frames_57_to_69_shifted_to=58_to_70\n"
        "frame_count=70\nfps=50\nduration=1.400s\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
