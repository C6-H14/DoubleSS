from pathlib import Path
import json

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUT = SOURCE / "hurt" / "preview_v1_24f_60fps"
PICKS = [
    0, 2, 4, 6, 8, 9,             # fast contraction
    15, 30, 44,                    # short pain hold
    47, 50, 53, 56, 59, 62, 65,   # recovery begins
    68, 71, 74, 77, 79,            # return to stance
    85, 90, 96,                    # stable finish
]


def main() -> None:
    video = max(SOURCE.glob("*.mp4"), key=lambda p: p.stat().st_mtime)
    capture = cv2.VideoCapture(str(video))
    fps = float(capture.get(cv2.CAP_PROP_FPS))
    decoded = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        decoded.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB), "RGB"))
    capture.release()
    if len(decoded) <= max(PICKS):
        raise RuntimeError(f"Video has only {len(decoded)} frames")

    OUT.mkdir(parents=True, exist_ok=True)
    frames_dir = OUT / "frames"
    frames_dir.mkdir(exist_ok=True)
    frames = []
    for number, source_index in enumerate(PICKS, 1):
        image = decoded[source_index]
        image.save(frames_dir / f"hurt_{number:03d}.png", optimize=True)
        frames.append(image)

    preview = [frame.resize((533, 400), Image.Resampling.LANCZOS) for frame in frames]
    preview[0].save(
        OUT / "hurt_preview_24f_60fps_400ms.gif",
        save_all=True,
        append_images=preview[1:],
        duration=17,
        loop=0,
        disposal=2,
        optimize=False,
    )

    columns, cell_w, cell_h = 6, 278, 234
    rows = 4
    sheet = Image.new("RGB", (columns * cell_w, rows * cell_h), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for index, (source_index, frame) in enumerate(zip(PICKS, frames), 1):
        thumb = frame.resize((278, 209), Image.Resampling.LANCZOS)
        x = ((index - 1) % columns) * cell_w
        y = ((index - 1) // columns) * cell_h
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 5, y + 5), f"{index:02d} <- src {source_index:02d} ({source_index / fps:.3f}s)", fill="white")
    sheet.save(OUT / "hurt_contact_sheet_24f.jpg", quality=96)

    metadata = {
        "source": str(video),
        "source_fps": fps,
        "selected_source_frames": PICKS,
        "output_frames": len(frames),
        "output_fps": 60,
        "duration": 0.4,
    }
    (OUT / "metadata.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
