from pathlib import Path

import cv2
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1")
SELECTION = ROOT / "cast_buff" / "selection.txt"
OUTPUT = ROOT / "cast_buff_transition_test_v4_source_f53.png"


def main() -> None:
    source = None
    for line in SELECTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("source="):
            source = Path(line.removeprefix("source="))
            break
    if source is None:
        raise RuntimeError("source path missing")
    cap = cv2.VideoCapture(str(source))
    cap.set(cv2.CAP_PROP_POS_FRAMES, 53)
    ok, frame = cap.read()
    cap.release()
    if not ok:
        raise RuntimeError("unable to decode source f53")
    Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)).save(OUTPUT, compress_level=2)


if __name__ == "__main__":
    main()
