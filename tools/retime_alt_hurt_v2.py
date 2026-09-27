from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "hurt_v2" / "hurt_complete_frames_60fps"
ORIGINAL_VIDEO = next((ROOT / "art_preview" / "video").glob("jimeng-2026-09-25-7397-*.mp4"))
OUT = ROOT / "art_preview" / "hurt_v2" / "hurt_tailfix_39f_frames_60fps"
GIF = ROOT / "art_preview" / "hurt_v2" / "hurt_tailfix_39f_60fps.gif"
MP4 = ROOT / "art_preview" / "hurt_v2" / "hurt_tailfix_39f_60fps.mp4"
FPS = 60
TRANSITION_FRAMES = 24  # 0.4 seconds
HOLD_FRAMES = 6        # 0.1 seconds


def sample_range(first: int, last: int, count: int) -> list[int]:
    return [round(value) for value in np.linspace(first, last, count)]


def optical_intermediate(a: np.ndarray, b: np.ndarray, t: float) -> Image.Image:
    """Bidirectionally warp two RGB frames before blending them."""
    gray_a = cv2.cvtColor(a, cv2.COLOR_RGB2GRAY)
    gray_b = cv2.cvtColor(b, cv2.COLOR_RGB2GRAY)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    flow_ab = dis.calc(gray_a, gray_b, None)
    flow_ba = dis.calc(gray_b, gray_a, None)
    height, width = gray_a.shape
    grid_x, grid_y = np.meshgrid(np.arange(width), np.arange(height))
    map_ax = (grid_x - flow_ab[..., 0] * t).astype(np.float32)
    map_ay = (grid_y - flow_ab[..., 1] * t).astype(np.float32)
    map_bx = (grid_x - flow_ba[..., 0] * (1.0 - t)).astype(np.float32)
    map_by = (grid_y - flow_ba[..., 1] * (1.0 - t)).astype(np.float32)
    warp_a = cv2.remap(a, map_ax, map_ay, cv2.INTER_CUBIC,
                       borderMode=cv2.BORDER_REFLECT)
    warp_b = cv2.remap(b, map_bx, map_by, cv2.INTER_CUBIC,
                       borderMode=cv2.BORDER_REFLECT)
    mixed = cv2.addWeighted(warp_a, 1.0 - t, warp_b, t, 0.0)
    return Image.fromarray(mixed, "RGB")


def main() -> None:
    # Preserve the whole generated motion, but redistribute it into two even
    # 0.7 s transitions with only a short closed/guarding hold between them.
    enter = sample_range(1, 220, TRANSITION_FRAMES)
    hold = [220] * HOLD_FRAMES
    recover = sample_range(221, 369, TRANSITION_FRAMES)
    original_picks = enter + hold + recover
    removed_output_frames = set(range(19, 42, 2))
    first_cut = [source for output_index, source in enumerate(original_picks, 1)
                 if output_index not in removed_output_frames]
    removed_second_pass = set(range(19, 30, 2))
    picks = [source for output_index, source in enumerate(first_cut, 1)
             if output_index not in removed_second_pass]

    # Frames 364..369 came from 24.15 -> 60 fps conversion, which duplicated
    # source frames and retained a blurry source frame. Keep the sequence only
    # through output frame 34, then append exact decoded frames from the
    # original 7397 MP4 in reverse recovery order. Original frame 2 is the
    # motion-blurred exposure, so omit it.
    picks = picks[:-2]
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    frames: list[Image.Image] = []
    for index, source_index in enumerate(picks, 1):
        image = Image.open(SOURCE / f"frame_{source_index:03d}.png").convert("RGB")
        image.save(OUT / f"frame_{index:03d}.png", optimize=True)
        frames.append(image)
    capture = cv2.VideoCapture(str(ORIGINAL_VIDEO))
    original = []
    for _ in range(6):
        ok, bgr = capture.read()
        if not ok:
            raise RuntimeError("Failed to decode the first six original hurt frames")
        original.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB), "RGB"))
    capture.release()
    for source_index in (6, 5, 4, 3, 1):
        frames.append(original[source_index - 1])
    for index, image in enumerate(frames, 1):
        image.save(OUT / f"frame_{index:03d}.png", optimize=True)

    frames[0].save(
        GIF, save_all=True, append_images=frames[1:], duration=round(1000 / FPS),
        loop=0, disposal=2, optimize=False,
    )
    width, height = frames[0].size
    writer = cv2.VideoWriter(
        str(MP4), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (width, height)
    )
    if not writer.isOpened():
        raise RuntimeError("Failed to open MP4 writer")
    for frame in frames:
        writer.write(cv2.cvtColor(np.asarray(frame), cv2.COLOR_RGB2BGR))
    writer.release()

    metadata = {
        "fps": FPS,
        "frames": len(frames),
        "duration": len(frames) / FPS,
        "enter": {"frames": TRANSITION_FRAMES, "duration": TRANSITION_FRAMES / FPS,
                  "source_range": [1, 220]},
        "hold": {"frames": HOLD_FRAMES, "duration": HOLD_FRAMES / FPS,
                 "source_frame": 220},
        "recover": {"frames": TRANSITION_FRAMES, "duration": TRANSITION_FRAMES / FPS,
                    "source_range": [221, 369]},
        "selected_source_frames": picks + ["original:6", "original:5", "original:4",
                                             "original:3", "original:1"],
    }
    metadata["removed_original_output_frames"] = sorted(removed_output_frames)
    metadata["removed_42f_output_frames"] = sorted(removed_second_pass)
    (OUT.parent / "hurt_tailfix_39f_metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
