from pathlib import Path

import cv2
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT = ROOT / "new_animation_review"


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    videos = sorted(ROOT.glob("*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)[:3]
    lines = []
    for rank, video in enumerate(videos, 1):
        cap = cv2.VideoCapture(str(video))
        total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        fps = float(cap.get(cv2.CAP_PROP_FPS))
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        sample_count = 20
        indices = [round(i * (total - 1) / (sample_count - 1)) for i in range(sample_count)]
        thumbs = []
        for index in indices:
            cap.set(cv2.CAP_PROP_POS_FRAMES, index)
            ok, frame = cap.read()
            if not ok:
                continue
            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            image = Image.fromarray(frame)
            image.thumbnail((240, 180), Image.Resampling.LANCZOS)
            tile = Image.new("RGB", (250, 210), "#202020")
            tile.paste(image, ((250 - image.width) // 2, 5))
            ImageDraw.Draw(tile).text((8, 187), f"f{index:03d}  {index / fps:.2f}s", fill="white")
            thumbs.append(tile)
        sheet = Image.new("RGB", (250 * 5, 210 * 4), "#111111")
        for i, thumb in enumerate(thumbs):
            sheet.paste(thumb, ((i % 5) * 250, (i // 5) * 210))
        sheet_path = OUT / f"video_{rank}_contact.jpg"
        sheet.save(sheet_path, quality=92)
        lines.append(
            f"video_{rank}\npath={video}\nsize={width}x{height}\nfps={fps:.6f}\n"
            f"frames={total}\nduration={total / fps:.6f}\ncontact={sheet_path}\n"
        )
        cap.release()
    (OUT / "video_info.txt").write_text("\n".join(lines), encoding="utf-8")

    # Detailed inspection of the second/newest-1 clip (cast_debuff): one tile per
    # source frame around the throw-to-smoke transition.
    video = videos[1]
    cap = cv2.VideoCapture(str(video))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    indices = list(range(28, 59))
    tiles = []
    for index in indices:
        cap.set(cv2.CAP_PROP_POS_FRAMES, index)
        ok, frame = cap.read()
        if not ok:
            continue
        frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        image = Image.fromarray(frame)
        image.thumbnail((278, 209), Image.Resampling.LANCZOS)
        tile = Image.new("RGB", (286, 235), "#202020")
        tile.paste(image, ((286 - image.width) // 2, 3))
        ImageDraw.Draw(tile).text((6, 213), f"f{index:03d}  {index / fps:.3f}s", fill="white")
        tiles.append(tile)
    cols = 5
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (286 * cols, 235 * rows), "#111111")
    for i, tile in enumerate(tiles):
        sheet.paste(tile, ((i % cols) * 286, (i // cols) * 235))
    sheet.save(OUT / "cast_debuff_throw_detail.jpg", quality=94)
    cap.release()

    # Detailed review of the selected cast_buff recovery segment. Candidate
    # numbering is one-based and mirrors filenames in new_animation_candidates_v1.
    candidate_dir = ROOT / "new_animation_candidates_v1" / "cast_buff" / "pink_frames"
    candidate_paths = sorted(candidate_dir.glob("cast_buff_*.png"))[51:68]
    tiles = []
    for path in candidate_paths:
        image = Image.open(path).convert("RGB")
        image.thumbnail((278, 209), Image.Resampling.LANCZOS)
        tile = Image.new("RGB", (286, 235), "#202020")
        tile.paste(image, ((286 - image.width) // 2, 3))
        ImageDraw.Draw(tile).text((6, 213), path.stem, fill="white")
        tiles.append(tile)
    cols = 5
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (286 * cols, 235 * rows), "#111111")
    for i, tile in enumerate(tiles):
        sheet.paste(tile, ((i % cols) * 286, (i // cols) * 235))
    sheet.save(OUT / "cast_buff_candidate_52_68_detail.jpg", quality=94)


if __name__ == "__main__":
    main()
