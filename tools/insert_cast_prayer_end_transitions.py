from pathlib import Path
import json
import shutil

import cv2
from PIL import Image, ImageDraw

from refine_cast_prayer_v4 import foreground_anchor, shift_to_anchor


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "cast_prayer"
SOURCE_V3 = BASE / "preview_v3" / "frames"
SOURCE_V4 = BASE / "preview_v4" / "frames"
OUTPUT = BASE / "preview_v5"
FRAMES = OUTPUT / "frames"


def grab(video: Path, time_s: float):
    cap = cv2.VideoCapture(str(video))
    fps = cap.get(cv2.CAP_PROP_FPS)
    cap.set(cv2.CAP_PROP_POS_FRAMES, round(time_s * fps))
    ok, frame = cap.read()
    cap.release()
    if not ok:
        raise RuntimeError(f"Cannot read {video.name} at {time_s:.3f}s")
    return frame


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    reference = cv2.imread(str(SOURCE_V3 / "cast_prayer_027.png"))
    target_x, target_y, _ = foreground_anchor(reference)

    # Current v4 frame 27=old source frame 42, frame 28=old source 44,
    # frame 29=old source 45. Restore old 43, then sample 0.07 s between
    # old 44 (0.14 s) and old 45 (0.00 s) directly from the source video.
    restored_43 = cv2.imread(str(SOURCE_V3 / "cast_prayer_043.png"))
    restored_43, dx43, dy43 = shift_to_anchor(restored_43, target_x, target_y)

    video_dir = ROOT / "art_candidates" / "video_source"
    videos = list(video_dir.glob("jimeng-2026-09-19-6487-*.mp4"))
    if len(videos) != 1:
        raise RuntimeError(f"Expected one kneeling source video, found {len(videos)}")
    midpoint = grab(videos[0], 0.07)
    midpoint, dxmid, dymid = shift_to_anchor(midpoint, target_x, target_y)

    sequence = []
    provenance = []
    for i in range(1, 30):
        frame = cv2.imread(str(SOURCE_V4 / f"cast_prayer_{i:03d}.png"))
        sequence.append(frame)
        provenance.append(f"preview_v4 frame {i}")
        if i == 27:
            sequence.append(restored_43)
            provenance.append(f"preview_v3 source frame 43 aligned ({dx43},{dy43})")
        if i == 28:
            sequence.append(midpoint)
            provenance.append(f"source video 0.07s aligned ({dxmid},{dymid})")

    pil_frames = []
    for i, frame in enumerate(sequence, 1):
        cv2.imwrite(str(FRAMES / f"cast_prayer_{i:03d}.png"), frame)
        pil_frames.append(Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)))

    pil_frames[0].save(
        OUTPUT / "cast_prayer_preview_60fps.gif",
        save_all=True,
        append_images=pil_frames[1:],
        duration=17,
        loop=0,
        optimize=False,
    )

    thumb, cols = 250, 6
    rows = (len(pil_frames) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb, rows * (thumb + 30)), "#222")
    draw = ImageDraw.Draw(sheet)
    for i, image in enumerate(pil_frames):
        x, y = i % cols * thumb, i // cols * (thumb + 30)
        sheet.paste(image.resize((thumb, thumb), Image.Resampling.LANCZOS), (x, y))
        label = f"{i+1:02d}"
        if i + 1 in (28, 30):
            label += " inserted"
        draw.text((x + 5, y + thumb + 6), label, fill="white")
    sheet.save(OUTPUT / "cast_prayer_frames_contact.jpg", quality=95)

    metadata = {"fps": 60, "frames": len(sequence), "duration_seconds": len(sequence) / 60, "provenance": provenance}
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in metadata.items() if k != "provenance"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
