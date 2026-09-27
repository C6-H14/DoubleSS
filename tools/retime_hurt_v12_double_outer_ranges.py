from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "hurt_v11_abdomen_hand_even_reveal" / "hurt_v11_46f_frames_60fps"
OUT = ROOT / "art_preview" / "hurt_v13_outer_ranges_doubled_singlewarp"
FRAMES = OUT / "hurt_v13_83f_frames_60fps"
GIF = OUT / "hurt_v13_83f_60fps.gif"
MP4 = OUT / "hurt_v13_83f_60fps.mp4"
FPS = 60
REMOVED = {22, 24}


def midpoint_flow(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    ga = cv2.cvtColor(a, cv2.COLOR_RGB2GRAY)
    gb = cv2.cvtColor(b, cv2.COLOR_RGB2GRAY)
    flow_ab = cv2.calcOpticalFlowFarneback(ga, gb, None, 0.5, 5, 25, 5, 7, 1.5, 0)
    h, w = ga.shape
    x, y = np.meshgrid(np.arange(w), np.arange(h))
    ax = (x - 0.5 * flow_ab[..., 0]).astype(np.float32)
    ay = (y - 0.5 * flow_ab[..., 1]).astype(np.float32)
    wa = cv2.remap(a, ax, ay, cv2.INTER_LANCZOS4, borderMode=cv2.BORDER_REFLECT_101)
    # A two-sided blend creates two swords/hands when source topology differs.
    # The one-sided halfway warp preserves a single coherent silhouette.
    return wa


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 46:
        raise RuntimeError(f"Expected 46 frames, got {len(paths)}")
    originals = {i: np.asarray(Image.open(path).convert("RGB"))
                 for i, path in enumerate(paths, 1)}

    sequence: list[tuple[np.ndarray, str]] = []
    # 1..20: midpoint in every internal interval.
    for index in range(1, 21):
        sequence.append((originals[index], f"source {index}"))
        if index < 20:
            sequence.append((midpoint_flow(originals[index], originals[index + 1]),
                             f"mid {index}-{index + 1}"))
    # 21..25: remove two frames, with no inserted midpoint in this hold block.
    for index in range(21, 26):
        if index not in REMOVED:
            sequence.append((originals[index], f"source {index}"))
    # 26..46: midpoint in every internal interval.
    for index in range(26, 47):
        sequence.append((originals[index], f"source {index}"))
        if index < 46:
            sequence.append((midpoint_flow(originals[index], originals[index + 1]),
                             f"mid {index}-{index + 1}"))
    if len(sequence) != 83:
        raise RuntimeError(f"Expected 83 frames, got {len(sequence)}")

    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    images = []
    mapping = []
    for output_index, (rgb, source) in enumerate(sequence, 1):
        image = Image.fromarray(rgb, "RGB")
        image.save(FRAMES / f"frame_{output_index:03d}.png", optimize=True)
        images.append(image)
        mapping.append({"output": output_index, "source": source})

    images[0].save(GIF, save_all=True, append_images=images[1:],
                   duration=round(1000 / FPS), loop=0, disposal=2,
                   optimize=False)
    width, height = images[0].size
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"),
                             FPS, (width, height))
    for image in images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    # Sample midpoints on the sword/hand-heavy regions for visual QA.
    inserted = [(i, rgb, source) for i, (rgb, source) in enumerate(sequence, 1)
                if source.startswith("mid")]
    picks = np.linspace(0, len(inserted) - 1, 20).round().astype(int)
    sheet = Image.new("RGB", (5 * 278, 4 * 234), (24, 24, 24))
    draw = ImageDraw.Draw(sheet)
    for n, pick in enumerate(picks):
        out_index, rgb, label = inserted[pick]
        thumb = Image.fromarray(rgb, "RGB").resize((278, 209), Image.Resampling.LANCZOS)
        x, y = (n % 5) * 278, (n // 5) * 234
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 5, y + 5), f"out {out_index}: {label}", fill="white")
    sheet.save(OUT / "inserted_midpoint_contact.jpg", quality=96)

    metadata = {
        "fps": FPS, "frames": len(sequence), "duration": len(sequence) / FPS,
        "inserted_midpoints": 39, "removed_source_frames": sorted(REMOVED),
        "mapping": mapping,
    }
    (OUT / "hurt_v12_metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({k: v for k, v in metadata.items() if k != "mapping"},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
