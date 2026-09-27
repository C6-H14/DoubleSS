from pathlib import Path
import json

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_cast_s" / "video_v1_review"


def main() -> None:
    videos = sorted(VIDEO_DIR.glob("*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not videos:
        raise RuntimeError(f"No video in {VIDEO_DIR}")
    source = videos[0]
    cap = cv2.VideoCapture(str(source))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    expected = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)))
    cap.release()
    if not frames:
        raise RuntimeError(f"Cannot decode {source}")

    OUTPUT.mkdir(parents=True, exist_ok=True)
    picks = [round(i * (len(frames) - 1) / 47) for i in range(48)]
    thumb_w = 278
    thumb_h = round(height * thumb_w / width)
    label_h = 22
    sheet = Image.new("RGB", (thumb_w * 6, (thumb_h + label_h) * 8), (24, 24, 24))
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(picks):
        thumb = frames[index].resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = slot % 6 * thumb_w
        y = slot // 6 * (thumb_h + label_h)
        sheet.paste(thumb, (x, y + label_h))
        draw.text((x + 5, y + 4), f"{index + 1:03d}  {index / fps:.2f}s", fill="white")
        frames[index].save(OUTPUT / f"sample_{index + 1:03d}.png")
    contact = OUTPUT / "contact_48_samples.jpg"
    sheet.save(contact, quality=94)

    detail_indices = list(range(45, min(86, len(frames))))
    detail_w = 445
    detail_h = round(height * detail_w / width)
    detail_label_h = 24
    rows = (len(detail_indices) + 3) // 4
    detail = Image.new("RGB", (detail_w * 4, (detail_h + detail_label_h) * rows), (24, 24, 24))
    detail_draw = ImageDraw.Draw(detail)
    for slot, index in enumerate(detail_indices):
        thumb = frames[index].resize((detail_w, detail_h), Image.Resampling.LANCZOS)
        x = slot % 4 * detail_w
        y = slot // 4 * (detail_h + detail_label_h)
        detail.paste(thumb, (x, y + detail_label_h))
        detail_draw.text((x + 5, y + 4), f"{index + 1:03d}  {index / fps:.3f}s", fill="white")
    detail_path = OUTPUT / "contact_push_frames_046_086.jpg"
    detail.save(detail_path, quality=96)
    metadata = {
        "source": str(source), "decoded_frames": len(frames), "reported_frames": expected,
        "fps": fps, "duration": len(frames) / fps, "size": [width, height],
        "sampled_frames": [i + 1 for i in picks],
    }
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, ensure_ascii=False, indent=2))
    print(contact)
    print(detail_path)


if __name__ == "__main__":
    main()
