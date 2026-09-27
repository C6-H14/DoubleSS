from pathlib import Path
import shutil

import cv2
from PIL import Image


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v2_phase_retimed_1s"
FRAMES = OUTPUT / "frames_60f_60fps"
GIF = OUTPUT / "combo_attack_v2_60f_60fps_1s.gif"
MP4 = OUTPUT / "combo_attack_v2_60f_60fps_1s.mp4"
FPS = 60.0


# Source-frame selections by motion phase. Long holds/recovery stretches are
# thinned more aggressively; thrust peaks and sword-line changes stay denser.
PHASES = {
    "ready": [1, 3, 4],
    "thrust_1": [i for i in range(5, 19) if i not in {7, 10, 13}],
    "recover_1": [i for i in range(19, 25) if i != 22],
    "low_line_2": [i for i in range(25, 42) if i not in {28, 31, 34, 37, 40}],
    "transition_3": [42, 43, 46, 47],
    "deep_thrust_3": [i for i in range(48, 68) if i not in {50, 53, 56, 59, 62, 64, 66}],
    "recover_3": [i for i in range(68, 85) if i not in {70, 72, 74, 76, 78, 80, 82}],
    "settle": [85, 97],
}


def main() -> None:
    matches = sorted(VIDEO_DIR.glob("jimeng-2026-09-26-3899-*.mp4"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one 3899 source video, found {len(matches)}")
    capture = cv2.VideoCapture(str(matches[0]))
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

    selected = [number for values in PHASES.values() for number in values]
    if len(selected) != 60 or selected != sorted(selected) or len(set(selected)) != 60:
        raise RuntimeError(f"Invalid 60-frame selection: {selected}")

    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    frames = []
    mapping = []
    for output_number, source_number in enumerate(selected, 1):
        image = Image.fromarray(decoded[source_number - 1])
        image.save(FRAMES / f"frame_{output_number:03d}.png")
        frames.append(image)
        mapping.append(f"{output_number:03d} <- source {source_number:03d}")

    frames[0].save(
        GIF, save_all=True, append_images=frames[1:], duration=round(1000 / FPS),
        loop=0, optimize=False, disposal=2,
    )
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for source_number in selected:
        writer.write(cv2.cvtColor(decoded[source_number - 1], cv2.COLOR_RGB2BGR))
    writer.release()

    lines = []
    cursor = 1
    for name, values in PHASES.items():
        lines.append(f"{name}: output {cursor:02d}-{cursor + len(values) - 1:02d}, source {values}")
        cursor += len(values)
    lines.append("")
    lines.extend(mapping)
    (OUTPUT / "phase_and_frame_mapping.txt").write_text("\n".join(lines), encoding="utf-8")
    print(f"output_frames={len(frames)} fps={FPS} duration={len(frames)/FPS:.3f}s")
    for name, values in PHASES.items():
        print(f"{name}: {len(values)} frames")
    print(FRAMES)
    print(GIF)
    print(MP4)


if __name__ == "__main__":
    main()
