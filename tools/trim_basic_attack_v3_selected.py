from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_basic_attack" / "basic_attack_v3_opening_evens_removed" / "frames_50f_60fps"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_basic_attack" / "basic_attack_v4_selected_30f"
FRAMES = OUTPUT / "frames_30f_60fps"
GIF = OUTPUT / "basic_attack_v4_30f_60fps_500ms.gif"
MP4 = OUTPUT / "basic_attack_v4_30f_60fps_500ms.mp4"
FPS = 60.0
REMOVED = {2, 3, 7, 9, 11, 19, 21, 23, 26, 28, 30, 32, 34, 37, 39, 41, 43, 45, 47, 49}


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 50:
        raise RuntimeError(f"Expected 50 frames, found {len(paths)}")
    selected = [(number, path) for number, path in enumerate(paths, 1) if number not in REMOVED]
    if len(selected) != 30:
        raise RuntimeError(f"Expected 30 selected frames, found {len(selected)}")

    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    frames = []
    mapping = []
    for output_number, (source_number, source_path) in enumerate(selected, 1):
        image = Image.open(source_path).convert("RGB")
        image.save(FRAMES / f"frame_{output_number:03d}.png")
        frames.append(image)
        mapping.append(f"{output_number:03d} <- {source_number:03d}")

    frames[0].save(
        GIF, save_all=True, append_images=frames[1:],
        duration=round(1000 / FPS), loop=0, optimize=False, disposal=2,
    )
    width, height = frames[0].size
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for image in frames:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    (OUTPUT / "frame_mapping.txt").write_text("\n".join(mapping), encoding="utf-8")
    (OUTPUT / "removed_source_frames.txt").write_text(", ".join(map(str, sorted(REMOVED))), encoding="utf-8")
    print(f"source_frames={len(paths)} output_frames={len(frames)} fps={FPS} duration={len(frames)/FPS:.3f}s")
    print(f"removed={sorted(REMOVED)}")
    print(FRAMES)
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
