from pathlib import Path
import json

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
VIDEO_DIR = ROOT / "art_preview" / "video"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "video_v2_review"


def main() -> None:
    matches = sorted(VIDEO_DIR.glob("jimeng-2026-09-26-7522-*.mp4"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one 7522 video, found {len(matches)}")
    source = matches[0]
    capture = cv2.VideoCapture(str(source))
    fps = float(capture.get(cv2.CAP_PROP_FPS))
    expected = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    frames = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        frames.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)))
    capture.release()
    if len(frames) != expected:
        raise RuntimeError(f"Decoded {len(frames)} of {expected} frames")

    OUTPUT.mkdir(parents=True, exist_ok=True)
    picks = [round(i * (len(frames) - 1) / 39) for i in range(40)]
    thumb_w = 320
    thumb_h = round(height * thumb_w / width)
    label_h = 20
    sheet = Image.new("RGB", (thumb_w * 5, (thumb_h + label_h) * 8), (25, 25, 25))
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(picks):
        thumb = frames[index].resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = slot % 5 * thumb_w
        y = slot // 5 * (thumb_h + label_h)
        sheet.paste(thumb, (x, y + label_h))
        draw.text((x + 4, y + 3), f"frame {index + 1:03d}", fill="white")
    contact = OUTPUT / "contact_40_samples.jpg"
    sheet.save(contact, quality=92)
    metadata = {
        "source": str(source), "frames": len(frames), "fps": fps,
        "duration": len(frames) / fps, "size": [width, height],
        "sampled_frames": [index + 1 for index in picks],
    }
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, ensure_ascii=False, indent=2))
    print(contact)


if __name__ == "__main__":
    main()
