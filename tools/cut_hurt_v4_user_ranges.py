from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
SOURCE_DIR = ROOT / "art_preview" / "hurt_v4_bridge5030" / "hurt_v4_51f_frames_60fps"
OUT = ROOT / "art_preview" / "hurt_v5_direct_cuts"
FRAMES = OUT / "hurt_v5_44f_frames_60fps"
GIF = OUT / "hurt_v5_44f_60fps.gif"
MP4 = OUT / "hurt_v5_44f_60fps.mp4"
FPS = 60
REMOVED = set(range(16, 19)) | set(range(33, 37))


def main() -> None:
    source_paths = sorted(SOURCE_DIR.glob("frame_*.png"))
    if len(source_paths) != 51:
        raise RuntimeError(f"Expected 51 source frames, got {len(source_paths)}")
    kept = [(i, path) for i, path in enumerate(source_paths, 1) if i not in REMOVED]
    if len(kept) != 44:
        raise RuntimeError(f"Expected 44 kept frames, got {len(kept)}")

    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    images = []
    mapping = []
    for new_index, (old_index, path) in enumerate(kept, 1):
        image = Image.open(path).convert("RGB")
        image.save(FRAMES / f"frame_{new_index:03d}.png", optimize=True)
        images.append(image)
        mapping.append({"output": new_index, "source_v4": old_index})

    images[0].save(
        GIF, save_all=True, append_images=images[1:],
        duration=round(1000 / FPS), loop=0, disposal=2, optimize=False,
    )
    width, height = images[0].size
    writer = cv2.VideoWriter(
        str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height)
    )
    if not writer.isOpened():
        raise RuntimeError("Failed to open MP4 writer")
    for image in images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    # Two rows centered on the requested direct cuts.
    old_picks = [13, 14, 15, 19, 20, 21, 30, 31, 32, 37, 38, 39]
    thumbs = []
    for old_index in old_picks:
        image = Image.open(SOURCE_DIR / f"frame_{old_index:03d}.png").convert("RGB")
        image = image.resize((278, 209), Image.Resampling.LANCZOS)
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, 100, 24), fill=(20, 20, 20))
        draw.text((6, 5), f"old {old_index}", fill="white")
        thumbs.append(image)
    sheet = Image.new("RGB", (6 * 278, 2 * 209), (20, 20, 20))
    for i, image in enumerate(thumbs):
        sheet.paste(image, ((i % 6) * 278, (i // 6) * 209))
    sheet.save(OUT / "direct_cut_contact.jpg", quality=96)

    metadata = {
        "fps": FPS,
        "frames": len(images),
        "duration": len(images) / FPS,
        "removed_original_frames": sorted(REMOVED),
        "direct_connections": [[15, 19], [32, 37]],
        "mapping": mapping,
    }
    (OUT / "hurt_v5_metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({k: v for k, v in metadata.items() if k != "mapping"},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
