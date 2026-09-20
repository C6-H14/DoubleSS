from pathlib import Path
import shutil

from matanyone import InferenceCore


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO = ROOT / "art_candidates" / "video_source" / "cast_v1" / "cast_attack_1_source.mp4"
TEST = ROOT / "art_candidates" / "matting_tests" / "matanyone_cast_attack_1_v1"


def main():
    ascii_video = TEST / "input" / "cast_attack_1_source.mp4"
    if not ascii_video.exists() or ascii_video.stat().st_size != VIDEO.stat().st_size:
        shutil.copy2(VIDEO, ascii_video)
    output = TEST / "output_768"
    output.mkdir(parents=True, exist_ok=True)
    processor = InferenceCore("PeiqingYang/MatAnyone")
    result = processor.process_video(
        input_path=str(ascii_video),
        mask_path=str(TEST / "input" / "first_mask.png"),
        output_path=str(output),
        n_warmup=10,
        r_erode=5,
        r_dilate=5,
        suffix="cast768",
        save_image=True,
        max_size=768,
    )
    print(result)


if __name__ == "__main__":
    main()
