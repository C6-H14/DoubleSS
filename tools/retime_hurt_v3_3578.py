from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = next((ROOT / "art_preview" / "video").glob("jimeng-2026-09-26-3578-*.mp4"))
OUT_ROOT = ROOT / "art_preview" / "hurt_v3"
FRAMES = OUT_ROOT / "hurt_v3_51f_frames_60fps"
GIF = OUT_ROOT / "hurt_v3_51f_60fps.gif"
MP4 = OUT_ROOT / "hurt_v3_51f_60fps.mp4"
FPS = 60
ENTER_SOURCE_FIRST = 1
ENTER_SOURCE_LAST = 37
ENTER_FRAMES = 24
HOLD_FRAMES = 4


def main() -> None:
    cap = cv2.VideoCapture(str(SOURCE))
    source_fps = float(cap.get(cv2.CAP_PROP_FPS))
    decoded = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        decoded.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    cap.release()
    if len(decoded) < ENTER_SOURCE_LAST:
        raise RuntimeError(f"Only decoded {len(decoded)} frames")

    enter_indices = [round(value) for value in np.linspace(
        ENTER_SOURCE_FIRST - 1, ENTER_SOURCE_LAST - 1, ENTER_FRAMES
    )]
    sequence = enter_indices + [enter_indices[-1]] * HOLD_FRAMES + enter_indices[-2::-1]
    if len(sequence) != 51:
        raise RuntimeError(f"Expected 51 frames, got {len(sequence)}")

    if FRAMES.exists():
        shutil.rmtree(FRAMES)
    FRAMES.mkdir(parents=True)
    images = []
    for output_index, source_index in enumerate(sequence, 1):
        image = Image.fromarray(decoded[source_index], "RGB")
        image.save(FRAMES / f"frame_{output_index:03d}.png", optimize=True)
        images.append(image)

    images[0].save(
        GIF, save_all=True, append_images=images[1:], duration=round(1000 / FPS),
        loop=0, disposal=2, optimize=False,
    )
    width, height = images[0].size
    writer = cv2.VideoWriter(
        str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height)
    )
    if not writer.isOpened():
        raise RuntimeError("Failed to open MP4 writer")
    for image in images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    metadata = {
        "source": str(SOURCE), "source_fps": source_fps,
        "fps": FPS, "frames": len(sequence), "duration": len(sequence) / FPS,
        "enter": {"frames": ENTER_FRAMES, "duration": ENTER_FRAMES / FPS,
                  "source_frames_1based": [i + 1 for i in enter_indices]},
        "hold": {"frames": HOLD_FRAMES, "duration": HOLD_FRAMES / FPS},
        "recover": {"frames": ENTER_FRAMES - 1,
                    "duration": (ENTER_FRAMES - 1) / FPS,
                    "method": "reverse exact enter path without duplicate endpoint"},
    }
    (OUT_ROOT / "hurt_v3_51f_metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
