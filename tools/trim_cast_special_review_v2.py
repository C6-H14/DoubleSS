from pathlib import Path
import shutil

from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\cast_special\combined_preview")
SOURCE = ROOT / "review_100f_50fps_frames"
OUT = ROOT / "trimmed_v2_75f_50fps"


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 100:
        raise RuntimeError(f"Expected 100 source frames, found {len(paths)}")

    delete = {
        number
        for number in range(1, 101)
        if (32 <= number <= 50 and number % 2 == 1)
        or (67 <= number <= 98 and number % 2 == 0)
    }
    kept = [(number, path) for number, path in enumerate(paths, 1) if number not in delete]
    if len(kept) != 75:
        raise RuntimeError(f"Expected 75 kept frames, got {len(kept)}")

    OUT.mkdir(parents=True, exist_ok=True)
    frames_dir = OUT / "frames"
    frames_dir.mkdir(exist_ok=True)
    images = []
    mapping = []
    for new_number, (old_number, source) in enumerate(kept, 1):
        destination = frames_dir / f"frame_{new_number:03d}.png"
        shutil.copy2(source, destination)
        images.append(Image.open(destination).convert("RGB"))
        mapping.append(f"{new_number:03d} <- {old_number:03d}")

    preview = [image.resize((480, 480), Image.Resampling.LANCZOS) for image in images]
    preview[0].save(
        OUT / "cast_special_trimmed_v2_75f_50fps_1p5s.gif",
        save_all=True,
        append_images=preview[1:],
        duration=20,
        loop=0,
        disposal=2,
        optimize=False,
    )

    cells = []
    for new_number, ((old_number, _), image) in enumerate(zip(kept, images), 1):
        thumb = image.resize((144, 144), Image.Resampling.LANCZOS)
        cell = Image.new("RGB", (144, 164), (18, 18, 18))
        cell.paste(thumb, (0, 20))
        ImageDraw.Draw(cell).text((4, 3), f"{new_number:02d} <- {old_number:03d}", fill="white")
        cells.append(cell)
    columns = 10
    rows = (len(cells) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * 144, rows * 164), (10, 10, 10))
    for index, cell in enumerate(cells):
        sheet.paste(cell, ((index % columns) * 144, (index // columns) * 164))
    sheet.save(OUT / "cast_special_trimmed_v2_contact_sheet.png", optimize=True)

    (OUT / "frame_mapping.txt").write_text(
        "Deleted original odd frames in 32-50: "
        + ",".join(str(n) for n in sorted(delete) if 32 <= n <= 50)
        + "\nDeleted original even frames in 67-98: "
        + ",".join(str(n) for n in sorted(delete) if 67 <= n <= 98)
        + "\nKept frames: 75\nPlayback: 50 fps\nDuration: 1.500 seconds\n\n"
        + "\n".join(mapping)
        + "\n",
        encoding="utf-8",
    )
    print(OUT)


if __name__ == "__main__":
    main()
