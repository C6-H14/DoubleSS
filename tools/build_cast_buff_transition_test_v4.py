from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1")
SOURCE_DIR = ROOT / "cast_buff_transition_test_v3" / "pink_frames"
OUTPUT = ROOT / "cast_buff_transition_test_v4"
FRAMES = OUTPUT / "pink_frames"
SOURCE_F53 = ROOT / "cast_buff_transition_test_v4_source_f53.png"
GENERATED = {
    "53_54_q25": Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-f132edc4-782d-4a90-97b1-bfce6d469aea.png"),
    "53_54_q75": Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-5a58ca19-b055-425c-86ad-cbb3fe430b0c.png"),
    "63_64_q33": Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-7eecfeb0-2d50-490f-a19b-422d9212eb2c.png"),
    "63_64_q67": Path(r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860\exec-94365e77-8ea0-4452-af33-a363a01965cb.png"),
}
DELETE_OLD = {2, 4, 6, 8, 10, 18, 35, 37, 39, 41}


def fitted(path: Path) -> Image.Image:
    return Image.open(path).convert("RGB").resize((1120, 832), Image.Resampling.LANCZOS)


def main() -> None:
    if FRAMES.exists():
        shutil.rmtree(FRAMES)
    FRAMES.mkdir(parents=True, exist_ok=True)

    additions = {name: fitted(path) for name, path in GENERATED.items()}
    source_f53 = Image.open(SOURCE_F53).convert("RGB")
    sequence: list[tuple[str, Image.Image]] = []
    for old_index, path in enumerate(sorted(SOURCE_DIR.glob("cast_buff_*.png")), 1):
        if old_index in DELETE_OLD:
            continue
        sequence.append((f"v3_{old_index}", Image.open(path).convert("RGB")))
        if old_index == 53:
            sequence.extend([
                ("insert_53_54_q25", additions["53_54_q25"]),
                ("insert_source_video_f53_q50", source_f53),
                ("insert_53_54_q75", additions["53_54_q75"]),
            ])
        if old_index == 63:
            sequence.extend([
                ("insert_63_64_q33", additions["63_64_q33"]),
                ("insert_63_64_q67", additions["63_64_q67"]),
            ])

    if len(sequence) != 67:
        # 72 - 10 current-round deletions + 5 insertions = 67.
        raise RuntimeError(f"Expected 67 frames, got {len(sequence)}")

    images = []
    manifest = []
    for new_index, (origin, image) in enumerate(sequence, 1):
        image.save(FRAMES / f"cast_buff_{new_index:03d}.png", compress_level=2)
        images.append(image)
        manifest.append(f"{new_index:03d}={origin}")

    images[0].save(
        OUTPUT / "cast_buff_transition_test_v4.gif",
        save_all=True, append_images=images[1:], duration=20, loop=0,
        disposal=1, optimize=False,
    )
    writer = cv2.VideoWriter(
        str(OUTPUT / "cast_buff_transition_test_v4.mp4"),
        cv2.VideoWriter_fourcc(*"mp4v"), 50.0, (1120, 832),
    )
    if not writer.isOpened():
        raise RuntimeError("Unable to create MP4 preview")
    for image in images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    # Review all modified source neighborhoods after renumbering.
    modified = [(i + 1, origin, image) for i, (origin, image) in enumerate(sequence)
                if ("insert" in origin or origin in {"v3_52", "v3_53", "v3_54", "v3_61", "v3_63", "v3_65", "v3_66"})]
    tile_w, tile_h, cols = 336, 250, 5
    rows = (len(modified) + cols - 1) // cols
    sheet = Image.new("RGB", (tile_w * cols, (tile_h + 24) * rows), "#171717")
    draw = ImageDraw.Draw(sheet)
    for slot, (new_index, origin, image) in enumerate(modified):
        thumb = image.resize((tile_w, tile_h), Image.Resampling.LANCZOS)
        x = (slot % cols) * tile_w
        y = (slot // cols) * (tile_h + 24)
        sheet.paste(thumb, (x, y))
        draw.text((x + 5, y + tile_h + 4), f"new {new_index}: {origin}", fill="white")
    sheet.save(OUTPUT / "cast_buff_v4_modified_sequences.jpg", quality=94)

    (OUTPUT / "manifest.txt").write_text(
        "variant=cast_buff_transition_test_v4\n"
        "frame_count=67\nfps=50\nduration=1.340s\n"
        "deleted_v3_frames=02,04,06,08,10,18,35,37,39,41\n"
        "inserted_after_v3_53=three frames (q25, source f53 q50, q75)\n"
        "inserted_between_v3_63_and_64=two frames (q33, q67)\n\n"
        + "\n".join(manifest) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
