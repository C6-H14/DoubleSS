from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1")
SOURCE = ROOT / "cast_buff_transition_test_v4" / "pink_frames"
OUTPUT = ROOT / "cast_buff_transition_test_v5"
FRAMES = OUTPUT / "pink_frames"


def optical_interpolate(left: Image.Image, right: Image.Image, t: float) -> Image.Image:
    a = np.asarray(left.convert("RGB"), dtype=np.float32) / 255.0
    b = np.asarray(right.convert("RGB"), dtype=np.float32) / 255.0
    a_gray = cv2.cvtColor(np.uint8(np.clip(a * 255, 0, 255)), cv2.COLOR_RGB2GRAY)
    b_gray = cv2.cvtColor(np.uint8(np.clip(b * 255, 0, 255)), cv2.COLOR_RGB2GRAY)

    flow_ab = cv2.calcOpticalFlowFarneback(a_gray, b_gray, None, 0.5, 5, 35, 5, 7, 1.5, 0)
    flow_ba = cv2.calcOpticalFlowFarneback(b_gray, a_gray, None, 0.5, 5, 35, 5, 7, 1.5, 0)
    yy, xx = np.mgrid[0:a.shape[0], 0:a.shape[1]].astype(np.float32)

    warped_a = cv2.remap(
        a, xx + t * flow_ba[..., 0], yy + t * flow_ba[..., 1],
        cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE,
    )
    warped_b = cv2.remap(
        b, xx + (1.0 - t) * flow_ab[..., 0], yy + (1.0 - t) * flow_ab[..., 1],
        cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE,
    )
    mixed = np.clip((1.0 - t) * warped_a + t * warped_b, 0.0, 1.0)
    return Image.fromarray(np.uint8(np.round(mixed * 255)), "RGB")


def main() -> None:
    if FRAMES.exists():
        shutil.rmtree(FRAMES)
    FRAMES.mkdir(parents=True, exist_ok=True)

    frames = [Image.open(path).convert("RGB") for path in sorted(SOURCE.glob("cast_buff_*.png"))]
    if len(frames) != 67:
        raise RuntimeError(f"Expected 67 V4 frames, got {len(frames)}")

    # One-based 46->51: replace 47..50 with equally spaced optical-flow frames.
    left, right = frames[45], frames[50]
    for one_based, t in zip(range(47, 51), (0.2, 0.4, 0.6, 0.8)):
        frames[one_based - 1] = optical_interpolate(left, right, t)

    # One-based 56->60: replace 57..59 at quarter steps.
    left, right = frames[55], frames[59]
    for one_based, t in zip(range(57, 60), (0.25, 0.5, 0.75)):
        frames[one_based - 1] = optical_interpolate(left, right, t)

    for index, image in enumerate(frames, 1):
        image.save(FRAMES / f"cast_buff_{index:03d}.png", compress_level=2)

    frames[0].save(
        OUTPUT / "cast_buff_transition_test_v5.gif",
        save_all=True, append_images=frames[1:], duration=20, loop=0,
        disposal=1, optimize=False,
    )
    writer = cv2.VideoWriter(
        str(OUTPUT / "cast_buff_transition_test_v5.mp4"),
        cv2.VideoWriter_fourcc(*"mp4v"), 50.0, (1120, 832),
    )
    if not writer.isOpened():
        raise RuntimeError("Unable to create MP4 preview")
    for image in frames:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    chosen = list(range(45, 53)) + list(range(55, 62))
    tile_w, tile_h, cols = 336, 250, 5
    rows = (len(chosen) + cols - 1) // cols
    sheet = Image.new("RGB", (tile_w * cols, (tile_h + 24) * rows), "#171717")
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(chosen):
        image = frames[index - 1].resize((tile_w, tile_h), Image.Resampling.LANCZOS)
        x = (slot % cols) * tile_w
        y = (slot // cols) * (tile_h + 24)
        sheet.paste(image, (x, y))
        label = "flow" if index in {47, 48, 49, 50, 57, 58, 59} else "endpoint/original"
        draw.text((x + 5, y + tile_h + 4), f"frame {index}: {label}", fill="white")
    sheet.save(OUTPUT / "cast_buff_v5_flow_sequences.jpg", quality=94)

    (OUTPUT / "manifest.txt").write_text(
        "variant=cast_buff_transition_test_v5\nframe_count=67\nfps=50\nduration=1.340s\n"
        "replaced_47_to_50=bidirectional optical flow between frames 46 and 51 at 20/40/60/80 percent\n"
        "replaced_57_to_59=bidirectional optical flow between frames 56 and 60 at 25/50/75 percent\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
