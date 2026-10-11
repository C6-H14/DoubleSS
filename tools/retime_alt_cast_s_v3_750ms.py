from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_cast_s" / "cast_s_v2_landmark_length" / "repaired_frames_97"
OUT = ROOT / "art_preview" / "alternate_fencer_cast_s" / "cast_s_v3_45f_750ms"
FRAMES_OUT = OUT / "frames_45f_60fps"
FPS = 60
FRAME_COUNT = 45


def main():
    paths = sorted(SOURCE.glob("*.png"))
    if len(paths) != 97:
        raise RuntimeError(f"Expected 97 source frames, found {len(paths)}")
    # Endpoint-preserving uniform temporal sampling.
    selected = np.linspace(0, len(paths) - 1, FRAME_COUNT).round().astype(int)
    frames = [Image.open(paths[i]).convert("RGB") for i in selected]
    FRAMES_OUT.mkdir(parents=True, exist_ok=True)
    for index, frame in enumerate(frames, 1):
        frame.save(FRAMES_OUT / f"cast_s_{index:03d}.png")

    width, height = frames[0].size
    writer = cv2.VideoWriter(str(OUT / "cast_s_v3_45f_60fps.mp4"),
                             cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for frame in frames:
        writer.write(cv2.cvtColor(np.array(frame), cv2.COLOR_RGB2BGR))
    writer.release()

    gif_frames = [frame.resize((640, 480), Image.Resampling.LANCZOS) for frame in frames]
    gif_frames[0].save(OUT / "cast_s_v3_45f_60fps.gif", save_all=True,
                       append_images=gif_frames[1:], duration=round(1000 / FPS),
                       loop=0, disposal=2, optimize=False)

    report = {
        "source_frames": len(paths), "output_frames": FRAME_COUNT, "fps": FPS,
        "duration_seconds": FRAME_COUNT / FPS,
        "selected_source_frames_1_based": (selected + 1).tolist(),
        "first_frame_preserved": int(selected[0]) == 0,
        "last_frame_preserved": int(selected[-1]) == len(paths) - 1,
    }
    (OUT / "retime_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
