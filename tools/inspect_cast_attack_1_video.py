from pathlib import Path
import json

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO_DIR = ROOT / "art_candidates" / "video_source"
OUT = VIDEO_DIR / "cast_v1" / "inspection"


def main():
    videos = sorted(VIDEO_DIR.glob("*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not videos:
        raise RuntimeError("No MP4 files found")
    video = videos[0]
    capture = cv2.VideoCapture(str(video))
    if not capture.isOpened():
        raise RuntimeError(f"Cannot open {video}")

    frame_count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = float(capture.get(cv2.CAP_PROP_FPS))
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    duration = frame_count / fps if fps else 0.0
    sample_count = min(20, frame_count)
    indices = [round(i * (frame_count - 1) / (sample_count - 1)) for i in range(sample_count)]

    OUT.mkdir(parents=True, exist_ok=True)
    thumbs = []
    for index in indices:
        capture.set(cv2.CAP_PROP_POS_FRAMES, index)
        ok, bgr = capture.read()
        if not ok:
            raise RuntimeError(f"Failed to read frame {index}")
        rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
        image = Image.fromarray(rgb)
        image.thumbnail((320, 240), Image.Resampling.LANCZOS)
        thumbs.append((index, image.copy()))
    capture.release()

    cols = 5
    cell_w, cell_h = 320, 266
    rows = (len(thumbs) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h), "#181818")
    draw = ImageDraw.Draw(sheet)
    for i, (index, image) in enumerate(thumbs):
        x = (i % cols) * cell_w
        y = (i // cols) * cell_h
        sheet.paste(image, (x + (cell_w - image.width) // 2, y))
        draw.text((x + 8, y + 242), f"source frame {index + 1:03d}", fill="white")
    sheet.save(OUT / "cast_attack_1_contact_sheet.jpg", quality=94)

    metadata = {
        "source": str(video),
        "width": width,
        "height": height,
        "fps": fps,
        "frame_count": frame_count,
        "duration_seconds": duration,
        "sample_indices_zero_based": indices,
    }
    (OUT / "metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, ensure_ascii=False, indent=2))
    print(OUT / "cast_attack_1_contact_sheet.jpg")


if __name__ == "__main__":
    main()
