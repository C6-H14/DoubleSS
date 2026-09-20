from pathlib import Path

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUTPUT = SOURCE / "cast_prayer" / "source_review"


def read_frame(cap: cv2.VideoCapture, index: int) -> Image.Image:
    cap.set(cv2.CAP_PROP_POS_FRAMES, index)
    ok, frame = cap.read()
    if not ok:
        raise RuntimeError(f"Cannot read frame {index}")
    return Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))


def contact(video: Path, output: Path) -> None:
    cap = cv2.VideoCapture(str(video))
    fps = cap.get(cv2.CAP_PROP_FPS)
    count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration = count / fps
    times = [min(i * 0.25, duration - 1 / fps) for i in range(int(duration / 0.25) + 1)]
    frames = [(t, read_frame(cap, min(count - 1, round(t * fps)))) for t in times]
    cap.release()
    thumb = (256, 256)
    cols = 5
    rows = (len(frames) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb[0], rows * (thumb[1] + 26)), "#222")
    draw = ImageDraw.Draw(sheet)
    for i, (t, image) in enumerate(frames):
        x = i % cols * thumb[0]
        y = i // cols * (thumb[1] + 26)
        sheet.paste(image.resize(thumb, Image.Resampling.LANCZOS), (x, y))
        draw.text((x + 6, y + thumb[1] + 6), f"{t:.2f}s", fill="white")
    sheet.save(output, quality=95)
    print(f"{video.name}: fps={fps:.3f}, frames={count}, duration={duration:.3f}s -> {output}")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    videos = sorted(SOURCE.glob("jimeng-2026-09-*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)[:2]
    if len(videos) != 2:
        raise RuntimeError("Expected two latest prayer videos")
    # Newest is the return-to-idle segment; the second newest is kneel/prayer.
    contact(videos[1], OUTPUT / "segment_1_contact_025s.jpg")
    contact(videos[0], OUTPUT / "segment_2_contact_025s.jpg")


if __name__ == "__main__":
    main()
