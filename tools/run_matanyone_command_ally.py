from pathlib import Path

import cv2
from matanyone import InferenceCore


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "command_ally"
SOURCE = BASE / "recovered_gif_v1" / "frames"
WORK = BASE / "matanyone_v1"


def main() -> None:
    paths = sorted(SOURCE.glob("command_ally_*.png"))
    first = cv2.imread(str(paths[0]))
    height, width = first.shape[:2]
    video = WORK / "command_ally_16.mp4"
    writer = cv2.VideoWriter(str(video), cv2.VideoWriter_fourcc(*"mp4v"), 24.0, (width, height))
    for path in paths:
        writer.write(cv2.imread(str(path)))
    writer.release()
    processor = InferenceCore("PeiqingYang/MatAnyone")
    result = processor.process_video(
        input_path=str(video), mask_path=str(WORK / "first_mask.png"),
        output_path=str(WORK / "output"), n_warmup=10,
        r_erode=3, r_dilate=3, suffix="command1024",
        save_image=True, max_size=1024,
    )
    print(result)


if __name__ == "__main__":
    main()
