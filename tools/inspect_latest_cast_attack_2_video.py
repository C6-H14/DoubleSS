from pathlib import Path

import cv2
from PIL import Image, ImageDraw, ImageFont


SOURCE_DIR = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT_DIR = SOURCE_DIR / "cast_attack_2_latest_motion_audit"


def main() -> None:
    video = max(SOURCE_DIR.glob("*.mp4"), key=lambda p: p.stat().st_mtime)
    cap = cv2.VideoCapture(str(video))
    fps = cap.get(cv2.CAP_PROP_FPS)
    frames = []
    for frame_no in range(37, 60):
        cap.set(cv2.CAP_PROP_POS_FRAMES, frame_no)
        ok, bgr = cap.read()
        if not ok:
            continue
        rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
        im = Image.fromarray(rgb)
        im.thumbnail((280, 208), Image.Resampling.LANCZOS)
        frames.append((frame_no, im.copy()))
    cap.release()

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    cols = 6
    rows = (len(frames) + cols - 1) // cols
    tile_w, tile_h, label_h, pad = 280, 208, 28, 12
    sheet = Image.new("RGB", (pad + cols * (tile_w + pad), 42 + rows * (tile_h + label_h + pad)), (28, 28, 28))
    draw = ImageDraw.Draw(sheet)
    draw.text((pad, 10), f"{video.name} | fps={fps:.3f} | frames 37-59", fill="white", font=ImageFont.load_default())
    for i, (frame_no, im) in enumerate(frames):
        row, col = divmod(i, cols)
        x = pad + col * (tile_w + pad)
        y = 42 + row * (tile_h + label_h + pad)
        sheet.paste(im, (x, y))
        seconds = frame_no / fps
        draw.text((x, y + tile_h + 4), f"frame {frame_no:02d} | {seconds:.3f}s", fill="white", font=ImageFont.load_default())
    sheet.save(OUT_DIR / "frames_37_59_contact.png")
    (OUT_DIR / "source.txt").write_text(str(video), encoding="utf-8")


if __name__ == "__main__":
    main()
