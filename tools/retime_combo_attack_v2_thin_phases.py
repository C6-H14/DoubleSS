from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v2_phase_retimed_1s" / "frames_60f_60fps"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v3_thinned_phases"
FRAMES = OUTPUT / "frames_37f_60fps"
GIF = OUTPUT / "combo_attack_v3_37f_60fps.gif"
MP4 = OUTPUT / "combo_attack_v3_37f_60fps.mp4"
FPS = 60.0


PHASES = {
    "ready": list(range(1, 4)),
    "thrust_1_half": [4, 6, 8, 10, 12, 14],
    "recover_1": list(range(15, 20)),
    "low_line_2_half": [20, 22, 24, 26, 28, 30],
    "transition_3_minus_one": [32, 34, 35],
    "deep_thrust_3_half": [36, 38, 40, 42, 44, 46, 48],
    "recover_3_half": [49, 51, 53, 55, 57],
    "settle": [59, 60],
}


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 60:
        raise RuntimeError(f"Expected 60 source frames, found {len(paths)}")
    selected = [number for values in PHASES.values() for number in values]
    if len(selected) != 37 or selected != sorted(selected):
        raise RuntimeError(f"Invalid selection: {selected}")

    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    frames = []
    mapping = []
    for output_number, source_number in enumerate(selected, 1):
        image = Image.open(paths[source_number - 1]).convert("RGB")
        image.save(FRAMES / f"frame_{output_number:03d}.png")
        frames.append(image)
        mapping.append(f"{output_number:03d} <- v2 {source_number:03d}")

    frames[0].save(
        GIF, save_all=True, append_images=frames[1:], duration=round(1000 / FPS),
        loop=0, optimize=False, disposal=2,
    )
    width, height = frames[0].size
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height))
    for image in frames:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    lines = []
    cursor = 1
    for name, values in PHASES.items():
        lines.append(f"{name}: output {cursor:02d}-{cursor + len(values) - 1:02d}, v2 frames {values}")
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
