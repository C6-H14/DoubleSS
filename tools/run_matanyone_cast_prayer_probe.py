from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image
from matanyone import InferenceCore


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "cast_prayer"
PINK = BASE / "preview_v6" / "frames"
INITIAL_ALPHA = BASE / "preview_v8_border_connected" / "alpha_frames" / "cast_prayer_001.png"
WORK = ROOT / "art_candidates" / "matting_tests" / "matanyone_cast_prayer_probe"
INPUT = WORK / "input"
OUTPUT = WORK / "output"


def main() -> None:
    if WORK.exists():
        shutil.rmtree(WORK)
    INPUT.mkdir(parents=True)
    OUTPUT.mkdir(parents=True)

    paths = sorted(PINK.glob("cast_prayer_*.png"))
    if len(paths) != 41:
        raise RuntimeError(f"Expected 41 frames, found {len(paths)}")
    first = cv2.imread(str(paths[0]))
    height, width = first.shape[:2]
    video = INPUT / "cast_prayer_41.mp4"
    writer = cv2.VideoWriter(str(video), cv2.VideoWriter_fourcc(*"mp4v"), 30.0, (width, height))
    for path in paths:
        writer.write(cv2.imread(str(path)))
    writer.release()

    alpha = np.asarray(Image.open(INITIAL_ALPHA).convert("RGBA").getchannel("A"))
    # A solid semantic seed: fill only tiny internal defects, retaining the
    # genuine spaces between limbs, sword and hair.
    mask = np.uint8(alpha >= 96) * 255
    cv2.imwrite(str(INPUT / "first_mask.png"), mask)
    Image.fromarray(mask, "L").save(INPUT / "first_mask_preview.png")

    processor = InferenceCore("PeiqingYang/MatAnyone")
    result = processor.process_video(
        input_path=str(video),
        mask_path=str(INPUT / "first_mask.png"),
        output_path=str(OUTPUT),
        n_warmup=10,
        r_erode=3,
        r_dilate=3,
        suffix="prayer1024",
        save_image=True,
        max_size=1024,
    )
    (WORK / "result.json").write_text(json.dumps({"result": str(result)}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(result)


if __name__ == "__main__":
    main()
