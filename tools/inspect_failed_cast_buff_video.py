from pathlib import Path
import json

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO_DIR = ROOT / "art_candidates" / "video_source"
OUT = VIDEO_DIR / "cast_buff_v1_failed_analysis"


def main():
    video = sorted(VIDEO_DIR.glob("jimeng-*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)[0]
    capture = cv2.VideoCapture(str(video))
    if not capture.isOpened():
        raise RuntimeError(f"Cannot open {video}")
    count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = float(capture.get(cv2.CAP_PROP_FPS))
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    sample_count = min(24, count)
    indices = [round(i * (count - 1) / (sample_count - 1)) for i in range(sample_count)]
    OUT.mkdir(parents=True, exist_ok=True)
    thumbs = []
    for index in indices:
        capture.set(cv2.CAP_PROP_POS_FRAMES, index)
        ok, bgr = capture.read()
        if not ok:
            raise RuntimeError(f"Could not read frame {index + 1}")
        image = Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
        image.thumbnail((320, 240), Image.Resampling.LANCZOS)
        thumbs.append((index, image.copy()))
    capture.release()

    cols, cell_w, cell_h = 6, 320, 266
    rows = (len(thumbs) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h), "#181818")
    draw = ImageDraw.Draw(sheet)
    for i, (index, image) in enumerate(thumbs):
        x = (i % cols) * cell_w
        y = (i // cols) * cell_h
        sheet.paste(image, (x + (cell_w - image.width) // 2, y))
        draw.text((x + 8, y + 242), f"source frame {index + 1:03d}", fill="white")
    sheet.save(OUT / "failed_cast_buff_contact_sheet.jpg", quality=95)
    metadata = {
        "source": str(video),
        "width": width,
        "height": height,
        "fps": fps,
        "frame_count": count,
        "duration_seconds": count / fps if fps else 0,
        "sample_indices_one_based": [i + 1 for i in indices],
    }
    (OUT / "metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
