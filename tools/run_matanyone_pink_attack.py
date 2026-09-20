from pathlib import Path
import json
import shutil

from matanyone import InferenceCore


ROOT = Path(r"D:\StSmod\DoubleSS")
TEST = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_pink_v1"


def main():
    original_video = Path(json.loads((TEST / "source.json").read_text(encoding="utf-8"))["video"])
    video = TEST / "input" / "attack_pink_source.mp4"
    if not video.exists() or video.stat().st_size != original_video.stat().st_size:
        shutil.copy2(original_video, video)
    output = TEST / "output_768_ascii"
    output.mkdir(parents=True, exist_ok=True)
    processor = InferenceCore("PeiqingYang/MatAnyone")
    result = processor.process_video(
        input_path=str(video),
        mask_path=str(TEST / "input" / "first_mask.png"),
        output_path=str(output),
        n_warmup=10,
        r_erode=5,
        r_dilate=5,
        suffix="pink768",
        save_image=True,
        max_size=768,
    )
    print(result)


if __name__ == "__main__":
    main()
