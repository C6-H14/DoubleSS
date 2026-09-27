from pathlib import Path
import json
import shutil

from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\hurt")
SOURCE = ROOT / "preview_v2_16f_60fps" / "frames"
OUT = ROOT / "preview_v3_11f_60fps"
DELETE = {6, 8, 10, 12, 14}


def main() -> None:
    paths = sorted(SOURCE.glob("hurt_*.png"))
    if len(paths) != 16:
        raise RuntimeError(f"Expected 16 frames, found {len(paths)}")
    kept = [(index, path) for index, path in enumerate(paths, 1) if index not in DELETE]
    if len(kept) != 11:
        raise RuntimeError(f"Expected 11 kept frames, found {len(kept)}")

    if OUT.exists():
        shutil.rmtree(OUT)
    frames_dir = OUT / "frames"
    frames_dir.mkdir(parents=True)
    frames = []
    mapping = []
    for new_index, (old_index, path) in enumerate(kept, 1):
        destination = frames_dir / f"hurt_{new_index:03d}.png"
        shutil.copy2(path, destination)
        frames.append(Image.open(destination).convert("RGB"))
        mapping.append({"new": new_index, "old_v2": old_index})

    preview = [frame.resize((533, 400), Image.Resampling.LANCZOS) for frame in frames]
    preview[0].save(
        OUT / "hurt_preview_v3_11f_60fps_183ms.gif",
        save_all=True,
        append_images=preview[1:],
        duration=17,
        loop=0,
        disposal=2,
        optimize=False,
    )

    columns, cell_w, cell_h = 4, 278, 234
    rows = 3
    sheet = Image.new("RGB", (columns * cell_w, rows * cell_h), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for new_index, ((old_index, _), frame) in enumerate(zip(kept, frames), 1):
        thumb = frame.resize((278, 209), Image.Resampling.LANCZOS)
        x = ((new_index - 1) % columns) * cell_w
        y = ((new_index - 1) // columns) * cell_h
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 5, y + 5), f"{new_index:02d} <- v2 {old_index:02d}", fill="white")
    sheet.save(OUT / "hurt_contact_sheet_v3_11f.jpg", quality=96)

    metadata = {
        "deleted_v2_frames": sorted(DELETE),
        "mapping": mapping,
        "frames": 11,
        "fps": 60,
        "duration_seconds": 11 / 60,
    }
    (OUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
