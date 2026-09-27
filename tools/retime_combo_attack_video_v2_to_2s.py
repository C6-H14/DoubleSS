from pathlib import Path
import shutil

import cv2
from PIL import Image


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v4_new_video_2s"
FRAMES = OUTPUT / "frames_120f_60fps"
GIF = OUTPUT / "combo_attack_v4_120f_60fps_2s.gif"
MP4 = OUTPUT / "combo_attack_v4_120f_60fps_2s.mp4"
FPS = 60.0
OUTPUT_COUNT = 120


def main() -> None:
    matches = sorted(VIDEO_DIR.glob("jimeng-2026-09-26-7522-*.mp4"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one 7522 source video, found {len(matches)}")
    capture = cv2.VideoCapture(str(matches[0]))
    source_fps = float(capture.get(cv2.CAP_PROP_FPS))
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    decoded = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        decoded.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    capture.release()
    if len(decoded) != 97:
        raise RuntimeError(f"Expected 97 decoded frames, found {len(decoded)}")

    indices = [round(i * (len(decoded) - 1) / (OUTPUT_COUNT - 1)) for i in range(OUTPUT_COUNT)]
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    frames = []
    mapping = []
    for output_number, source_index in enumerate(indices, 1):
        image = Image.fromarray(decoded[source_index])
        image.save(FRAMES / f"frame_{output_number:03d}.png")
        frames.append(image)
        mapping.append(f"{output_number:03d} <- source {source_index + 1:03d}")

    frames[0].save(
        GIF, save_all=True, append_images=frames[1:], duration=round(1000 / FPS),
        loop=0, optimize=False, disposal=2,
    )
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for source_index in indices:
        writer.write(cv2.cvtColor(decoded[source_index], cv2.COLOR_RGB2BGR))
    writer.release()
    (OUTPUT / "frame_mapping.txt").write_text("\n".join(mapping), encoding="utf-8")

    print(f"source_frames={len(decoded)} source_fps={source_fps:.6f}")
    print(f"output_frames={OUTPUT_COUNT} fps={FPS} duration={OUTPUT_COUNT/FPS:.3f}s")
    print(FRAMES)
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
