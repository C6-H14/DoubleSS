from pathlib import Path
import shutil

import cv2
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUTPUT = ROOT / "new_animation_candidates_v1"


def read_frame(cap: cv2.VideoCapture, index: int) -> Image.Image:
    cap.set(cv2.CAP_PROP_POS_FRAMES, index)
    ok, frame = cap.read()
    if not ok:
        raise RuntimeError(f"Unable to decode frame {index}")
    return Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))


def save_action(name: str, source: Path, indices: list[int], frame_ms: int | list[int], note: str) -> None:
    target = OUTPUT / name
    frames_dir = target / "pink_frames"
    if frames_dir.exists():
        shutil.rmtree(frames_dir)
    frames_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target / "source.mp4")

    cap = cv2.VideoCapture(str(source))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    frames = []
    for sequence, source_index in enumerate(indices, 1):
        image = read_frame(cap, source_index)
        image.save(frames_dir / f"{name}_{sequence:03d}.png", compress_level=2)
        frames.append(image)
    cap.release()

    frames[0].save(
        target / f"{name}_candidate.gif",
        save_all=True,
        append_images=frames[1:],
        duration=frame_ms,
        loop=0,
        disposal=2,
    )

    thumb_w, thumb_h = 278, 209
    cols = 6
    rows = (len(frames) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + 25)), "#181818")
    for i, (image, source_index) in enumerate(zip(frames, indices)):
        thumb = image.copy()
        thumb.thumbnail((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = (i % cols) * thumb_w
        y = (i // cols) * (thumb_h + 25)
        sheet.paste(thumb, (x + (thumb_w - thumb.width) // 2, y))
        ImageDraw.Draw(sheet).text((x + 5, y + 210), f"#{i+1:02d} <- src {source_index:03d}", fill="white")
    sheet.save(target / f"{name}_contact.jpg", quality=92)

    duration_ms = sum(frame_ms) if isinstance(frame_ms, list) else len(indices) * frame_ms
    nominal_fps = len(indices) * 1000 / duration_ms
    (target / "selection.txt").write_text(
        f"action={name}\nsource={source}\nsource_fps={fps:.6f}\n"
        f"selected_count={len(indices)}\npreview_timing={frame_ms}\n"
        f"preview_duration={duration_ms / 1000:.3f}s\nnominal_fps={nominal_fps:.3f}\n"
        f"source_indices={','.join(map(str, indices))}\nnotes={note}\n",
        encoding="utf-8",
    )


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    newest = sorted(ROOT.glob("*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)[:3]
    heavy, debuff, buff = newest

    # Remove most of the long overhead anticipation hold, while retaining one
    # frame at each side of the hold so the pose still reads.
    heavy_indices = list(range(0, 25, 2)) + [25, 51] + list(range(52, 97, 2))
    if heavy_indices[-1] != 96:
        heavy_indices.append(96)
    # User-requested deletions use one-based candidate frame numbers.
    heavy_indices = [source_index for position, source_index in enumerate(heavy_indices, 1)
                     if position not in {11, 13, 15}]

    # Egg reaches the future smoke center at f33. Frames 34-48 let it travel too
    # far/right and are intentionally omitted; f49 starts directly with smoke.
    debuff_indices = list(range(0, 33, 2)) + [33] + list(range(49, 97, 2))
    if debuff_indices[-1] != 96:
        debuff_indices.append(96)

    # Before candidate frame 27, restore the original midpoint between every
    # adjacent pair: current src 0,2,...,52 becomes every source frame 0..52.
    # From the old candidate frame 28 onward, retain the existing even sampling.
    buff_indices = list(range(0, 53)) + list(range(54, 101, 2))
    # Candidate frame 57 begins an unintended free-hand return/wave and frames
    # 58-64 complete that loop. Frame 56 and frame 65 are matching sword-down
    # recovery poses, so remove the whole 57-64 segment for a clean match cut.
    buff_indices = [source_index for position, source_index in enumerate(buff_indices, 1)
                    if not 57 <= position <= 64]
    buff_timing = 20

    save_action("attack_heavy", heavy, heavy_indices, 25,
                "Long overhead hold compressed; candidate frames 11, 13, and 15 removed as requested.")
    save_action("cast_debuff", debuff, debuff_indices, 22,
                "Match cut: source f33 egg at blast center -> source f49 established purple smoke; f34-f48 omitted.")
    save_action("cast_buff", buff, buff_indices, buff_timing,
                "Before old candidate frame 27, each missing midpoint restored. Old candidate frames 57-64 removed to eliminate unintended hand-return loop; old frame 56 match-cuts to old frame 65.")


if __name__ == "__main__":
    main()
