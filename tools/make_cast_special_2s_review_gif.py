from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\cast_special\combined_preview")
FRAMES = ROOT / "review_100f_50fps_frames"
OUTPUT = ROOT / "cast_special_combined_review_100f_50fps_2s_480px.gif"


def main() -> None:
    paths = sorted(FRAMES.glob("frame_*.png"))
    if len(paths) != 100:
        raise RuntimeError(f"Expected 100 frames, found {len(paths)}")
    images = [Image.open(path).convert("RGB").resize((480, 480), Image.Resampling.LANCZOS) for path in paths]
    images[0].save(
        OUTPUT,
        save_all=True,
        append_images=images[1:],
        duration=20,
        loop=0,
        disposal=2,
        optimize=False,
    )
    print(OUTPUT)


if __name__ == "__main__":
    main()
