from pathlib import Path

import cv2


VIDEO = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\attack_sword_v2_trimmed.mp4")
OUT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\attack_sword_v2_22frame_v1\source_refs")
FRAME_MAP = {8: "transition_03_04_video.png", 65: "transition_13_14_video.png", 68: "transition_14_15_video.png"}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    capture = cv2.VideoCapture(str(VIDEO))
    if not capture.isOpened():
        raise RuntimeError(f"Cannot open video: {VIDEO}")

    for frame_index, filename in FRAME_MAP.items():
        capture.set(cv2.CAP_PROP_POS_FRAMES, frame_index)
        ok, frame = capture.read()
        if not ok:
            raise RuntimeError(f"Cannot decode frame {frame_index}")
        if not cv2.imwrite(str(OUT / filename), frame):
            raise RuntimeError(f"Cannot write {OUT / filename}")
    capture.release()


if __name__ == "__main__":
    main()
