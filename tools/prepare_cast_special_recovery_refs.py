from pathlib import Path

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUT = SOURCE / "cast_special" / "recovery_regen_refs"
FIRST_INDEX = 58
LAST_INDEX = 76


def main() -> None:
    video = max(SOURCE.glob("*.mp4"), key=lambda p: p.stat().st_mtime)
    capture = cv2.VideoCapture(str(video))
    frames: dict[int, Image.Image] = {}
    for index in range(LAST_INDEX + 1):
        ok, bgr = capture.read()
        if not ok:
            raise RuntimeError(f"Video ended before frame {LAST_INDEX}: {video}")
        if index in (FIRST_INDEX, LAST_INDEX):
            rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
            frames[index] = Image.fromarray(rgb, "RGB")
    capture.release()

    OUT.mkdir(parents=True, exist_ok=True)
    first_path = OUT / "cast_special_recovery_first_frame58.png"
    last_path = OUT / "cast_special_recovery_last_frame76.png"
    frames[FIRST_INDEX].save(first_path, optimize=True)
    frames[LAST_INDEX].save(last_path, optimize=True)

    preview = Image.new("RGB", (1920, 1010), (20, 20, 20))
    preview.paste(frames[FIRST_INDEX], (0, 50))
    preview.paste(frames[LAST_INDEX], (960, 50))
    draw = ImageDraw.Draw(preview)
    draw.text((20, 15), "FIRST frame 58 - last valid landed pose", fill="white")
    draw.text((980, 15), "LAST frame 76 - correct idle pose", fill="white")
    preview.save(OUT / "cast_special_recovery_refs_preview.png", optimize=True)

    print(first_path)
    print(last_path)
    print(OUT / "cast_special_recovery_refs_preview.png")


if __name__ == "__main__":
    main()
