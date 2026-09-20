from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO_DIR = ROOT / "art_candidates" / "video_source"
OUTPUT = VIDEO_DIR / "cast_prayer" / "preview_v3"
FRAMES = OUTPUT / "frames"
FPS = 60.0


def find_one(pattern: str) -> Path:
    matches = list(VIDEO_DIR.glob(pattern))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one match for {pattern}, found {len(matches)}")
    return matches[0]


def grab(video: Path, time_s: float) -> Image.Image:
    cap = cv2.VideoCapture(str(video))
    fps = cap.get(cv2.CAP_PROP_FPS)
    count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    index = max(0, min(count - 1, round(time_s * fps)))
    cap.set(cv2.CAP_PROP_POS_FRAMES, index)
    ok, frame = cap.read()
    cap.release()
    if not ok:
        raise RuntimeError(f"Cannot read {video.name} at {time_s:.3f}s")
    return Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))


def sample(video: Path, start: float, end: float, count: int) -> list[tuple[float, Image.Image]]:
    return [(float(t), grab(video, float(t))) for t in np.linspace(start, end, count)]


def main() -> None:
    first = find_one("jimeng-2026-09-19-6487-*.mp4")
    second = find_one("jimeng-2026-09-20-5239-*.mp4")
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    # 45 frames total: 0.15 s kneel + 0.4 s prayer + 0.2 s rise.
    # The generated return clip raises the sword overhead at roughly 2.75-3.50 s.
    # Avoid that unwanted motion completely: use its quiet light-fade section, then
    # reverse the clean kneeling motion from segment 1 for a natural recovery.
    rise_items = list(reversed(sample(first, 0.0, 1.50, 12)))
    phases = [
        ("kneel", first, sample(first, 0.0, 1.50, 9)),
        ("prayer", first, sample(first, 1.50, 3.98, 13)),
        ("prayer", second, sample(second, 0.45, 2.00, 11)),
        ("rise", first, rise_items),
    ]

    built = []
    manifest = []
    for phase, video, items in phases:
        for source_time, image in items:
            built.append(image)
            index = len(built)
            path = FRAMES / f"cast_prayer_{index:03d}.png"
            image.save(path, optimize=True)
            manifest.append({"frame": index, "phase": phase, "source": video.name, "source_time": round(source_time, 4)})

    # 60 fps preview. GIF timing is integer milliseconds, so 17 ms is the closest
    # broadly supported delay (about 1.02 seconds for 60 frames).
    gif = OUTPUT / "cast_prayer_preview_60fps.gif"
    built[0].save(gif, save_all=True, append_images=built[1:], duration=17, loop=0, optimize=False)

    # Complete final-frame contact sheet, numbered and phase-labelled.
    thumb = 250
    cols = 6
    rows = (len(built) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb, rows * (thumb + 28)), "#222")
    draw = ImageDraw.Draw(sheet)
    for i, (image, item) in enumerate(zip(built, manifest)):
        x = i % cols * thumb
        y = i // cols * (thumb + 28)
        sheet.paste(image.resize((thumb, thumb), Image.Resampling.LANCZOS), (x, y))
        draw.text((x + 5, y + thumb + 6), f"{i + 1:02d} {item['phase']}", fill="white")
    sheet.save(OUTPUT / "cast_prayer_frames_contact.jpg", quality=95)

    summary = {
        "fps": FPS,
        "frames": len(built),
        "preview_duration_seconds": len(built) / FPS,
        "target_phases_seconds": {"kneel": 0.15, "prayer": 0.4, "rise": 0.2},
        "frame_allocation": {"kneel": 9, "prayer": 24, "rise": 12},
        "manifest": manifest,
    }
    (OUTPUT / "metadata.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in summary.items() if k != "manifest"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
