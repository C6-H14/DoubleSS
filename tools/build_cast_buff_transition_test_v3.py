from pathlib import Path
import shutil

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1")
SOURCE_DIR = ROOT / "cast_buff_transition_test_v2" / "pink_frames"
OUTPUT = ROOT / "cast_buff_transition_test_v3"
FRAMES = OUTPUT / "pink_frames"
SELECTION = ROOT / "cast_buff" / "selection.txt"
GENERATED = [
    Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-7742026a-9365-4123-884e-b0cfd9dedaa5.png"),
    Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-cb0f6b32-3117-438e-a8fa-c098761136f8.png"),
]
DELETE_OLD = {62, 64, 68, 70}


def source_video() -> Path:
    for line in SELECTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("source="):
            return Path(line.removeprefix("source="))
    raise RuntimeError("source video missing from selection.txt")


def extract_video_frame(video: Path, index: int) -> Image.Image:
    cap = cv2.VideoCapture(str(video))
    cap.set(cv2.CAP_PROP_POS_FRAMES, index)
    ok, frame = cap.read()
    cap.release()
    if not ok:
        raise RuntimeError(f"Unable to decode source frame {index}")
    return Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))


def main() -> None:
    if FRAMES.exists():
        shutil.rmtree(FRAMES)
    FRAMES.mkdir(parents=True, exist_ok=True)

    video = source_video()
    source_midpoints = {
        54: extract_video_frame(video, 55),
        55: extract_video_frame(video, 57),
    }
    generated = [Image.open(path).convert("RGB").resize((1120, 832), Image.Resampling.LANCZOS)
                 for path in GENERATED]

    sequence: list[tuple[str, Image.Image]] = []
    for old_index, path in enumerate(sorted(SOURCE_DIR.glob("cast_buff_*.png")), 1):
        if old_index in DELETE_OLD:
            continue
        sequence.append((f"old_{old_index}", Image.open(path).convert("RGB")))
        if old_index in source_midpoints:
            sequence.append((f"source_{55 if old_index == 54 else 57}", source_midpoints[old_index]))
        if old_index == 56:
            sequence.append(("generated_56_to_57_one_third", generated[0]))
            sequence.append(("generated_56_to_57_two_thirds", generated[1]))

    if len(sequence) != 72:
        raise RuntimeError(f"Expected 72 frames, got {len(sequence)}")

    images = []
    manifest = []
    for new_index, (origin, image) in enumerate(sequence, 1):
        path = FRAMES / f"cast_buff_{new_index:03d}.png"
        image.save(path, compress_level=2)
        images.append(image)
        manifest.append(f"{new_index:03d}={origin}")

    images[0].save(
        OUTPUT / "cast_buff_transition_test_v3.gif",
        save_all=True,
        append_images=images[1:],
        duration=20,
        loop=0,
        disposal=1,
        optimize=False,
    )
    writer = cv2.VideoWriter(
        str(OUTPUT / "cast_buff_transition_test_v3.mp4"),
        cv2.VideoWriter_fourcc(*"mp4v"), 50.0, (1120, 832),
    )
    if not writer.isOpened():
        raise RuntimeError("Unable to create MP4 preview")
    for image in images:
        writer.write(cv2.cvtColor(__import__("numpy").asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    review_indices = range(52, 66)
    tile_w, tile_h = 336, 250
    cols = 5
    rows = 3
    sheet = Image.new("RGB", (tile_w * cols, (tile_h + 24) * rows), "#171717")
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(review_indices):
        image = images[index - 1].resize((tile_w, tile_h), Image.Resampling.LANCZOS)
        x = (slot % cols) * tile_w
        y = (slot // cols) * (tile_h + 24)
        sheet.paste(image, (x, y))
        draw.text((x + 5, y + tile_h + 4), f"frame {index}: {manifest[index-1].split('=',1)[1]}", fill="white")
    sheet.save(OUTPUT / "cast_buff_v3_frames_052_to_065.jpg", quality=94)

    (OUTPUT / "manifest.txt").write_text(
        "variant=cast_buff_transition_test_v3\nframe_count=72\nfps=50\nduration=1.440s\n"
        "deleted_v2_frames=62,64,68,70\n"
        "insertions=after v2 f54 source video f55; after v2 f55 source video f57; after v2 f56 two generated thirds\n\n"
        + "\n".join(manifest) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
