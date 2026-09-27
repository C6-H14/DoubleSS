from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUT = SOURCE / "idle" / "review_v1"


def main():
    video = max(SOURCE.glob("jimeng-2026-09-21-4865-*.mp4"), key=lambda p: p.stat().st_mtime)
    cap = cv2.VideoCapture(str(video))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB), "RGB"))
    cap.release()
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)

    picks = np.linspace(0, len(frames) - 1, 32).round().astype(int).tolist()
    cols, cw, ch = 8, 240, 205
    sheet = Image.new("RGB", (cols * cw, 4 * ch), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for n, source_index in enumerate(picks):
        frame = frames[source_index].resize((240, 180), Image.Resampling.LANCZOS)
        x, y = (n % cols) * cw, (n // cols) * ch
        sheet.paste(frame, (x, y + 25))
        draw.text((x + 4, y + 4), f"src {source_index:03d}  {source_index/fps:.2f}s", fill="white")
    sheet.save(OUT / "idle_contact_32.jpg", quality=96)

    preview = [f.resize((533, 400), Image.Resampling.LANCZOS) for f in frames]
    preview[0].save(OUT / "idle_source.gif", save_all=True, append_images=preview[1:],
                    duration=round(1000 / fps), loop=0, disposal=2, optimize=False)
    meta = {"source": str(video), "fps": fps, "frames": len(frames),
            "duration": len(frames) / fps, "size": list(frames[0].size), "contact_picks": picks}
    (OUT / "metadata.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(meta, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
