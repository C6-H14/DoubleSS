from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image
from matanyone import InferenceCore


ROOT = Path(r"D:\StSmod\DoubleSS")
CANDIDATES = ROOT / "art_candidates" / "video_source" / "new_animation_candidates_v1"
WORK = ROOT / "art_candidates" / "matting_tests" / "matanyone_v14_three"
BASE_MASK = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_pink_v1" / "input" / "first_mask.png"
SOURCES = {
    "attack_heavy": (CANDIDATES / "attack_heavy" / "pink_frames", "attack_heavy_*.png", 40.0),
    "cast_debuff": (CANDIDATES / "cast_debuff" / "pink_frames", "cast_debuff_*.png", 1000.0 / 22.0),
    "cast_buff": (CANDIDATES / "cast_buff_transition_test_v6" / "pink_frames", "cast_buff_*.png", 50.0),
}


def make_video_and_mask(name, directory, pattern, fps):
    paths = sorted(directory.glob(pattern))
    if not paths:
        raise RuntimeError(f"No frames: {directory} / {pattern}")
    first = cv2.imread(str(paths[0]), cv2.IMREAD_COLOR)
    h, w = first.shape[:2]
    input_dir = WORK / name / "input"
    input_dir.mkdir(parents=True, exist_ok=True)
    video = input_dir / f"{name}.mp4"
    writer = cv2.VideoWriter(str(video), cv2.VideoWriter_fourcc(*"mp4v"), fps, (w, h))
    for path in paths:
        frame = cv2.imread(str(path), cv2.IMREAD_COLOR)
        if frame.shape[:2] != (h, w):
            raise RuntimeError(f"Mixed dimensions: {path}")
        writer.write(frame)
    writer.release()
    mask = np.asarray(Image.open(BASE_MASK).convert("L").resize((w, h), Image.Resampling.LANCZOS))
    # Keep the initialization decisive; gray resampling pixels otherwise teach
    # the tracker that the pink fringe is part of the foreground.
    mask = np.where(mask >= 128, 255, 0).astype(np.uint8)
    mask_path = input_dir / "first_mask.png"
    cv2.imwrite(str(mask_path), mask)
    return paths, video, mask_path


def main():
    WORK.mkdir(parents=True, exist_ok=True)
    prepared = {}
    for name, spec in SOURCES.items():
        prepared[name] = make_video_and_mask(name, *spec)

    for name, (paths, video, mask) in prepared.items():
        output = WORK / name / "output"
        pha_existing = list(output.glob("*/pha/*.png")) if output.exists() else []
        if len(pha_existing) == len(paths):
            print(f"SKIP {name}: existing {len(pha_existing)} alpha frames")
            continue
        if output.exists():
            shutil.rmtree(output)
        output.mkdir(parents=True)
        print(f"PROCESS {name}: {len(paths)} frames")
        # InferenceCore retains temporal memory whose tensor dimensions depend
        # on the video canvas, so each differently-sized animation needs an
        # isolated instance.
        processor = InferenceCore("PeiqingYang/MatAnyone")
        result = processor.process_video(
            input_path=str(video),
            mask_path=str(mask),
            output_path=str(output),
            n_warmup=10,
            r_erode=5,
            r_dilate=5,
            suffix="semantic768",
            save_image=True,
            max_size=768,
        )
        print(result)


if __name__ == "__main__":
    main()
