from pathlib import Path
import shutil

from PIL import Image, ImageSequence

from matte_cast_prayer_v8_border_connected import border_connected_matte


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO_DIR = ROOT / "art_candidates" / "video_source"
SOURCE = VIDEO_DIR / "Codex 图像 2026年9月20日 20_40_14.gif"
BASE = VIDEO_DIR / "command_ally"
FRAMES = BASE / "recovered_gif_v1" / "frames"
MAT = BASE / "matanyone_v1"
KEYED = MAT / "keyed_seed"


def main() -> None:
    if BASE.exists():
        shutil.rmtree(BASE)
    FRAMES.mkdir(parents=True)
    KEYED.mkdir(parents=True)
    gif = Image.open(SOURCE)
    frames = [frame.convert("RGB") for frame in ImageSequence.Iterator(gif)]
    if len(frames) != 16:
        raise RuntimeError(f"Expected 16 GIF frames, found {len(frames)}")
    for index, frame in enumerate(frames, 1):
        path = FRAMES / f"command_ally_{index:03d}.png"
        frame.save(path, optimize=True)
        border_connected_matte(path).save(KEYED / f"command_ally_{index:03d}.png", optimize=True)
    alpha = Image.open(KEYED / "command_ally_001.png").convert("RGBA").getchannel("A")
    alpha.point(lambda value: 255 if value >= 96 else 0).save(MAT / "first_mask.png")
    print(f"recovered={len(frames)} size={frames[0].size} output={BASE}")


if __name__ == "__main__":
    main()
