from pathlib import Path

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUT = SOURCE / "cast_special" / "recovery_extracted"


def main() -> None:
    video = max(SOURCE.glob("*.mp4"), key=lambda p: p.stat().st_mtime)
    cap = cv2.VideoCapture(str(video))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB), "RGB"))
    cap.release()

    OUT.mkdir(parents=True, exist_ok=True)

    # The useful motion ends at source frame 35. Sample it to 13 frames so the
    # replacement lasts 0.4 s at 30 fps while retaining the arm trajectory.
    picks = [0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 35]
    selected = [frames[i] for i in picks]
    for output_index, (source_index, image) in enumerate(zip(picks, selected), 1):
        image.save(OUT / f"frame_{output_index:02d}_source_{source_index:02d}.png", optimize=True)

    # GIF duration is 400 ms total (roughly 30 fps).
    selected[0].save(
        OUT / "cast_special_recovery_13f_30fps.gif",
        save_all=True,
        append_images=selected[1:],
        duration=33,
        loop=0,
        disposal=2,
    )

    cells = []
    for output_index, (source_index, image) in enumerate(zip(picks, selected), 1):
        thumb = image.resize((240, 240), Image.Resampling.LANCZOS)
        cell = Image.new("RGB", (240, 267), (18, 18, 18))
        cell.paste(thumb, (0, 27))
        ImageDraw.Draw(cell).text(
            (6, 7),
            f"{output_index:02d} <- src {source_index:02d} ({source_index / fps:.3f}s)",
            fill="white",
        )
        cells.append(cell)
    columns = 5
    rows = (len(cells) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * 240, rows * 267), (10, 10, 10))
    for index, cell in enumerate(cells):
        sheet.paste(cell, ((index % columns) * 240, (index // columns) * 267))
    sheet.save(OUT / "cast_special_recovery_13f_contact_sheet.png", optimize=True)

    (OUT / "frame_mapping.txt").write_text(
        "source=" + str(video) + "\n"
        + f"source_fps={fps:.6f}\n"
        + "selected_source_frames=" + ",".join(map(str, picks)) + "\n"
        + "preview=13 frames, 33 ms each, about 0.429 seconds\n",
        encoding="utf-8",
    )
    print(OUT)


if __name__ == "__main__":
    main()
