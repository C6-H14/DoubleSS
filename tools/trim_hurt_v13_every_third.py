from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "hurt_v13_outer_ranges_doubled_singlewarp" / "hurt_v13_83f_frames_60fps"
OUT = ROOT / "art_preview" / "hurt_v14_every_third_removed"
FRAMES = OUT / "hurt_v14_57f_frames_60fps"
GIF = OUT / "hurt_v14_57f_60fps.gif"
MP4 = OUT / "hurt_v14_57f_60fps.mp4"
FPS = 60.0


def remove_frame(number: int) -> bool:
    if 1 <= number <= 39:
        return (number - 1) % 3 == 2
    if 43 <= number <= 83:
        return (number - 43) % 3 == 2
    return False


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 83:
        raise RuntimeError(f"Expected 83 source frames, found {len(paths)}")

    kept = [(i, path) for i, path in enumerate(paths, 1) if not remove_frame(i)]
    removed = [i for i in range(1, 84) if remove_frame(i)]
    if len(kept) != 57:
        raise RuntimeError(f"Expected 57 output frames, got {len(kept)}")

    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)

    pil_frames = []
    mapping = []
    for out_number, (source_number, source_path) in enumerate(kept, 1):
        image = Image.open(source_path).convert("RGB")
        image.save(FRAMES / f"frame_{out_number:03d}.png")
        pil_frames.append(image)
        mapping.append(f"{out_number:03d} <- {source_number:03d}")

    pil_frames[0].save(
        GIF,
        save_all=True,
        append_images=pil_frames[1:],
        duration=round(1000 / FPS),
        loop=0,
        optimize=False,
        disposal=2,
    )

    width, height = pil_frames[0].size
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for image in pil_frames:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    (OUT / "frame_mapping.txt").write_text("\n".join(mapping), encoding="utf-8")
    (OUT / "removed_source_frames.txt").write_text(", ".join(map(str, removed)), encoding="utf-8")
    print(f"source={len(paths)} output={len(kept)} duration={len(kept)/FPS:.3f}s")
    print(f"removed={removed}")
    print(FRAMES)
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
