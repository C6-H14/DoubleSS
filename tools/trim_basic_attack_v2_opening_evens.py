from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_basic_attack" / "basic_attack_v2_1s_every4" / "frames_60f_60fps"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_basic_attack" / "basic_attack_v3_opening_evens_removed"
FRAMES = OUTPUT / "frames_50f_60fps"
GIF = OUTPUT / "basic_attack_v3_50f_60fps.gif"
MP4 = OUTPUT / "basic_attack_v3_50f_60fps.mp4"
FPS = 60.0


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 60:
        raise RuntimeError(f"Expected 60 frames, found {len(paths)}")

    removed = set(range(2, 22, 2))
    selected = [(number, path) for number, path in enumerate(paths, 1) if number not in removed]
    if len(selected) != 50:
        raise RuntimeError(f"Expected 50 selected frames, found {len(selected)}")

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
    (OUTPUT / "removed_source_frames.txt").write_text(", ".join(map(str, sorted(removed))), encoding="utf-8")
    print(f"source_frames={len(paths)} output_frames={len(frames)} fps={FPS} duration={len(frames)/FPS:.3f}s")
    print(f"removed={sorted(removed)}")
    print(FRAMES)
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
