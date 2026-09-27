from pathlib import Path

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUT = ROOT / "art_candidates" / "video_source" / "cast_special" / "inspection"


def main() -> None:
    video = max(SOURCE.glob("*.mp4"), key=lambda p: p.stat().st_mtime)
    cap = cv2.VideoCapture(str(video))
    fps = cap.get(cv2.CAP_PROP_FPS)
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    cap.release()

    OUT.mkdir(parents=True, exist_ok=True)
    indices = list(range(48, len(frames), 2))
    cells = []
    for index in indices:
        image = Image.fromarray(frames[index]).resize((300, 300), Image.Resampling.LANCZOS)
        cell = Image.new("RGB", (300, 326), (20, 20, 20))
        cell.paste(image, (0, 26))
        ImageDraw.Draw(cell).text((8, 6), f"{index:02d}  {index / fps:.3f}s", fill="white")
        cells.append(cell)

    columns = 5
    rows = (len(cells) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * 300, rows * 326), (12, 12, 12))
    for i, cell in enumerate(cells):
        sheet.paste(cell, ((i % columns) * 300, (i // columns) * 326))
    sheet.save(OUT / "end_dense_contact_sheet.png", optimize=True)
    print(OUT / "end_dense_contact_sheet.png")


if __name__ == "__main__":
    main()
