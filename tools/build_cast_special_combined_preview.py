from pathlib import Path

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUT = SOURCE / "cast_special" / "combined_preview"


def find_video(prefix: str) -> Path:
    matches = list(SOURCE.glob(prefix + "*.mp4"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one match for {prefix!r}, got {matches}")
    return matches[0]


def decode(path: Path) -> list[Image.Image]:
    capture = cv2.VideoCapture(str(path))
    result = []
    while True:
        ok, bgr = capture.read()
        if not ok:
            break
        result.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB), "RGB"))
    capture.release()
    if not result:
        raise RuntimeError(f"Cannot decode {path}")
    return result


def save_gif(frames: list[Image.Image], path: Path, duration_ms: int) -> None:
    frames[0].save(
        path,
        save_all=True,
        append_images=frames[1:],
        duration=duration_ms,
        loop=0,
        disposal=2,
    )


def main() -> None:
    main_video = find_video("jimeng-2026-09-21-5132-")
    recovery_video = find_video("jimeng-2026-09-21-2600-")
    main_frames = decode(main_video)
    recovery_frames = decode(recovery_video)

    # Keep the valid main action through its final safe landed frame 58.
    # Sample both sources at the same one-in-three cadence. Recovery frame 0
    # duplicates main frame 58, so start the replacement at source frame 3.
    main_picks = list(range(0, 58, 3)) + [58]
    recovery_picks = list(range(3, 35, 3)) + [35]
    combined = [main_frames[i] for i in main_picks] + [recovery_frames[i] for i in recovery_picks]

    OUT.mkdir(parents=True, exist_ok=True)
    for output_index, image in enumerate(combined, 1):
        phase = "main" if output_index <= len(main_picks) else "recovery"
        image.save(OUT / f"frame_{output_index:02d}_{phase}.png", optimize=True)

    save_gif(combined, OUT / "cast_special_combined_33f_30fps.gif", 33)
    save_gif(combined, OUT / "cast_special_combined_33f_24fps.gif", 42)

    # Long review version: retain every original source frame so timing can be
    # judged and trimmed visually. 59 main + 35 recovery + 6 final holds =
    # exactly 100 frames, or 2.00 seconds at 50 fps.
    review_main_picks = list(range(0, 59))
    review_recovery_picks = list(range(1, 36))
    review = [main_frames[i] for i in review_main_picks]
    review += [recovery_frames[i] for i in review_recovery_picks]
    review += [review[-1].copy() for _ in range(6)]
    review_dir = OUT / "review_100f_50fps_frames"
    review_dir.mkdir(exist_ok=True)
    for output_index, image in enumerate(review, 1):
        image.save(review_dir / f"frame_{output_index:03d}.png", optimize=True)
    save_gif(review, OUT / "cast_special_combined_review_100f_50fps_2s.gif", 20)

    cells = []
    for output_index, image in enumerate(combined, 1):
        thumb = image.resize((192, 192), Image.Resampling.LANCZOS)
        cell = Image.new("RGB", (192, 216), (18, 18, 18))
        cell.paste(thumb, (0, 24))
        phase = "M" if output_index <= len(main_picks) else "R"
        ImageDraw.Draw(cell).text((6, 5), f"{output_index:02d} {phase}", fill="white")
        cells.append(cell)
    columns = 6
    rows = (len(cells) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * 192, rows * 216), (10, 10, 10))
    for index, cell in enumerate(cells):
        sheet.paste(cell, ((index % columns) * 192, (index // columns) * 216))
    sheet.save(OUT / "cast_special_combined_33f_contact_sheet.png", optimize=True)

    (OUT / "frame_mapping.txt").write_text(
        f"main_video={main_video}\n"
        f"recovery_video={recovery_video}\n"
        f"main_source_frames={','.join(map(str, main_picks))}\n"
        f"recovery_source_frames={','.join(map(str, recovery_picks))}\n"
        f"combined_frames={len(combined)}\n"
        "30fps_preview_duration=1.089s\n"
        "24fps_preview_duration=1.386s\n"
        "long_review_main_frames=0-58\n"
        "long_review_recovery_frames=1-35\n"
        "long_review_final_hold_frames=6\n"
        "long_review=100 frames, 50 fps, 2.000 seconds\n",
        encoding="utf-8",
    )
    print(OUT)


if __name__ == "__main__":
    main()
