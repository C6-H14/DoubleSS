from pathlib import Path
import shutil

import cv2
from PIL import Image


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v1_full_60fps"
FRAMES = OUTPUT / "frames_241f_60fps"
GIF = OUTPUT / "combo_attack_v1_241f_60fps.gif"
MP4 = OUTPUT / "combo_attack_v1_241f_60fps.mp4"
FPS = 60.0


def main() -> None:
    matches = sorted(VIDEO_DIR.glob("jimeng-2026-09-26-3899-*.mp4"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one 3899 source video, found {len(matches)}")
    source = matches[0]
    capture = cv2.VideoCapture(str(source))
    source_fps = float(capture.get(cv2.CAP_PROP_FPS))
    source_count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    decoded = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        decoded.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    capture.release()
    if len(decoded) != source_count:
        raise RuntimeError(f"Decoded {len(decoded)} of {source_count} frames")

    duration = source_count / source_fps
    output_count = round(duration * FPS)
    indices = [min(source_count - 1, round(i * source_fps / FPS)) for i in range(output_count)]

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
        GIF, save_all=True, append_images=frames[1:],
        duration=round(1000 / FPS), loop=0, optimize=False, disposal=2,
    )
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for source_index in indices:
        writer.write(cv2.cvtColor(decoded[source_index], cv2.COLOR_RGB2BGR))
    writer.release()

    (OUTPUT / "frame_mapping.txt").write_text("\n".join(mapping), encoding="utf-8")
    print(f"source_frames={source_count} source_fps={source_fps:.6f} duration={duration:.6f}s")
    print(f"output_frames={output_count} fps={FPS} duration={output_count/FPS:.6f}s")
    print(FRAMES)
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
