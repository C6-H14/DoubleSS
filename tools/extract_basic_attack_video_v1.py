from pathlib import Path
import shutil

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_basic_attack" / "basic_attack_v1_extracted"
RAW = OUTPUT / "raw_97f_sourcefps"
FRAMES_60 = OUTPUT / "frames_241f_60fps"
GIF = OUTPUT / "basic_attack_v1_241f_60fps.gif"
MP4 = OUTPUT / "basic_attack_v1_241f_60fps.mp4"
CONTACT = OUTPUT / "contact_sheet.jpg"
TARGET_FPS = 60.0


def find_source() -> Path:
    matches = sorted(VIDEO_DIR.glob("jimeng-2026-09-26-4132-*.mp4"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one 4132 video, found {len(matches)}")
    return matches[0]


def main() -> None:
    source = find_source()
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
    output_count = round(duration * TARGET_FPS)
    indices = [min(source_count - 1, round(i * source_fps / TARGET_FPS)) for i in range(output_count)]

    # This directory contains only reproducible extraction products for this source.
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    RAW.mkdir(parents=True)
    FRAMES_60.mkdir(parents=True)

    for i, rgb in enumerate(decoded, 1):
        Image.fromarray(rgb).save(RAW / f"frame_{i:03d}.png")

    frames_60 = []
    mapping = []
    for output_number, source_index in enumerate(indices, 1):
        image = Image.fromarray(decoded[source_index])
        image.save(FRAMES_60 / f"frame_{output_number:03d}.png")
        frames_60.append(image)
        mapping.append(f"{output_number:03d} <- {source_index + 1:03d}")

    frames_60[0].save(
        GIF,
        save_all=True,
        append_images=frames_60[1:],
        duration=round(1000 / TARGET_FPS),
        loop=0,
        optimize=False,
        disposal=2,
    )
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), TARGET_FPS, (width, height))
    for source_index in indices:
        writer.write(cv2.cvtColor(decoded[source_index], cv2.COLOR_RGB2BGR))
    writer.release()

    picks = [round(i * (source_count - 1) / 19) for i in range(20)]
    thumb_w = 400
    thumb_h = round(height * thumb_w / width)
    sheet = Image.new("RGB", (thumb_w * 5, (thumb_h + 22) * 4), (25, 25, 25))
    draw = ImageDraw.Draw(sheet)
    for slot, source_index in enumerate(picks):
        image = Image.fromarray(decoded[source_index]).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = slot % 5 * thumb_w
        y = slot // 5 * (thumb_h + 22)
        sheet.paste(image, (x, y + 22))
        draw.text((x + 5, y + 4), f"source {source_index + 1:03d}", fill="white")
    sheet.save(CONTACT, quality=92)

    (OUTPUT / "frame_mapping_60fps.txt").write_text("\n".join(mapping), encoding="utf-8")
    print(f"source={source}")
    print(f"source_frames={source_count} source_fps={source_fps:.6f} duration={duration:.6f}")
    print(f"output_frames={output_count} target_fps={TARGET_FPS}")
    print(RAW)
    print(FRAMES_60)
    print(GIF)
    print(MP4)
    print(CONTACT)


if __name__ == "__main__":
    main()
