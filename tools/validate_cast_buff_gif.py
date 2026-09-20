from pathlib import Path
import hashlib

import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageSequence


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1\cast_buff_transition_test_v2")
GIF = ROOT / "cast_buff_transition_test_v2.gif"
PNG_DIR = ROOT / "pink_frames"
OUT = ROOT / "decoded_gif_052_to_064.jpg"


def digest(image: Image.Image) -> str:
    return hashlib.sha256(image.convert("RGB").tobytes()).hexdigest()[:12]


def main() -> None:
    gif = Image.open(GIF)
    decoded = [frame.convert("RGB") for frame in ImageSequence.Iterator(gif)]
    durations = [frame.info.get("duration") for frame in ImageSequence.Iterator(Image.open(GIF))]
    pngs = sorted(PNG_DIR.glob("cast_buff_*.png"))
    if len(decoded) != len(pngs):
        raise RuntimeError(f"frame count mismatch: gif={len(decoded)} png={len(pngs)}")

    rows = []
    for index, (gif_frame, png_path) in enumerate(zip(decoded, pngs), 1):
        png = Image.open(png_path).convert("RGB")
        mae = float(np.mean(np.abs(np.asarray(gif_frame, dtype=np.float32) - np.asarray(png, dtype=np.float32))))
        rows.append(f"{index:03d} duration={durations[index-1]} gif={digest(gif_frame)} png={digest(png)} mae={mae:.4f}")
    (ROOT / "gif_validation.txt").write_text("\n".join(rows), encoding="utf-8")

    chosen = list(range(52, 65))
    tile_w, tile_h = 336, 250
    sheet = Image.new("RGB", (tile_w * 5, (tile_h + 24) * 3), "#151515")
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(chosen):
        image = decoded[index - 1].resize((tile_w, tile_h), Image.Resampling.LANCZOS)
        x = (slot % 5) * tile_w
        y = (slot // 5) * (tile_h + 24)
        sheet.paste(image, (x, y))
        draw.text((x + 5, y + tile_h + 4), f"decoded GIF frame {index}", fill="white")
    sheet.save(OUT, quality=94)

    # Fresh filenames avoid Codex/Windows image-preview cache. The MP4 is an
    # independent playback check that does not use GIF disposal/palette logic.
    verified_gif = ROOT / "cast_buff_transition_v2_verified_72f.gif"
    png_images = [Image.open(path).convert("RGB") for path in pngs]
    png_images[0].save(
        verified_gif,
        save_all=True,
        append_images=png_images[1:],
        duration=20,
        loop=0,
        disposal=1,
        optimize=False,
    )
    verified_mp4 = ROOT / "cast_buff_transition_v2_verified_72f.mp4"
    first = cv2.cvtColor(np.asarray(png_images[0]), cv2.COLOR_RGB2BGR)
    writer = cv2.VideoWriter(
        str(verified_mp4), cv2.VideoWriter_fourcc(*"mp4v"), 50.0,
        (first.shape[1], first.shape[0]),
    )
    if not writer.isOpened():
        raise RuntimeError("OpenCV could not create MP4 verification preview")
    for image in png_images:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()
    print(f"gif_frames={len(decoded)}")
    print(f"durations={sorted(set(durations))}")
    print(f"review={OUT}")
    print(f"verified_gif={verified_gif}")
    print(f"verified_mp4={verified_mp4}")


if __name__ == "__main__":
    main()
