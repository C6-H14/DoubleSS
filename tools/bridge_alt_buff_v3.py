"""Bridge transparent buff frames to the actual idle pose at both ends.

Preview-only: 60 frames / 60 fps, no JAR changes.
"""

from __future__ import annotations

import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_buff")
SOURCE = ROOT / "buff_v2_alpha_60f_60fps" / "alpha_frames_60fps"
IDLE = Path(r"D:\StSmod\DoubleSS\art_candidates\character_runtime_v34_idle_60fps\images\alt_idle_001.png")
OUT = ROOT / "buff_v3_alpha_aligned_60f_60fps"
FRAMES = OUT / "alpha_frames_60fps"
BRIDGE = 5
FPS = 60
SIZE = (1112, 834)


def to_premultiplied(image: Image.Image) -> np.ndarray:
    rgba = np.array(image.convert("RGBA"), dtype=np.float32)
    rgba[:, :, :3] *= rgba[:, :, 3:4] / 255.0
    return rgba


def to_straight(premul: np.ndarray) -> Image.Image:
    result = premul.copy()
    alpha = result[:, :, 3:4] / 255.0
    result[:, :, :3] /= np.maximum(alpha, 1.0 / 255.0)
    result[:, :, :3][alpha[:, :, 0] == 0] = 0
    return Image.fromarray(np.clip(np.rint(result), 0, 255).astype(np.uint8), "RGBA")


def mapped_idle() -> np.ndarray:
    # The runtime atlas pixels are already premultiplied. Align the idle
    # silhouette's top-left and shoe baseline with source buff frame 001.
    premul = np.asarray(Image.open(IDLE).convert("RGBA"), dtype=np.float32)
    scale = 568.0 / 341.0
    matrix = np.float32([[scale, 0, 142 - 379 * scale], [0, scale, 122 - 390 * scale]])
    return cv2.warpAffine(premul, matrix, SIZE, flags=cv2.INTER_LINEAR,
                          borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))


def flow(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    def gray(image: np.ndarray) -> np.ndarray:
        alpha = image[:, :, 3:4] / 255.0
        rgb = np.clip(image[:, :, :3] + 32 * (1 - alpha), 0, 255).astype(np.uint8)
        return cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY)

    ys, xs = slice(105, 710), slice(100, 720)
    first = np.ascontiguousarray(gray(a)[ys, xs])
    second = np.ascontiguousarray(gray(b)[ys, xs])
    estimator = cv2.DISOpticalFlow.create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    estimator.setFinestScale(1)
    partial = estimator.calc(first, second, None)
    full = np.zeros((SIZE[1], SIZE[0], 2), dtype=np.float32)
    full[ys, xs] = partial
    return full


def morph(a: np.ndarray, b: np.ndarray, forward: np.ndarray,
          backward: np.ndarray, t: float) -> np.ndarray:
    yy, xx = np.mgrid[:SIZE[1], :SIZE[0]].astype(np.float32)
    left = cv2.remap(a, xx - t * forward[:, :, 0], yy - t * forward[:, :, 1],
                     cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))
    right = cv2.remap(b, xx - (1 - t) * backward[:, :, 0], yy - (1 - t) * backward[:, :, 1],
                      cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))
    return left * (1 - t) + right * t


def contact(frames: list[Image.Image], path: Path, background: tuple[int, int, int, int]) -> None:
    indices = np.linspace(0, len(frames) - 1, 20).round().astype(int)
    sheet = Image.new("RGB", (5 * 300, 4 * 245), (28, 28, 28))
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(indices):
        view = Image.alpha_composite(Image.new("RGBA", frames[index].size, background), frames[index])
        thumb = view.resize((300, 225), Image.Resampling.LANCZOS).convert("RGB")
        x, y = slot % 5 * 300, slot // 5 * 245
        sheet.paste(thumb, (x, y))
        draw.text((x + 4, y + 226), f"{index + 1:02d}", fill=(255, 255, 255))
    sheet.save(path)


def main() -> None:
    paths = sorted(SOURCE.glob("buff_alpha_*.png"))
    if len(paths) != 60:
        raise RuntimeError(f"Expected 60 alpha frames, got {len(paths)}")
    FRAMES.mkdir(parents=True, exist_ok=True)
    images = [Image.open(path).convert("RGBA") for path in paths]
    frames = [to_premultiplied(image) for image in images]
    idle = mapped_idle()
    # Replace the existing first/last five frames; total duration stays 1 s.
    first, last = frames[BRIDGE - 1], frames[-BRIDGE]
    forward_in, backward_in = flow(idle, first), flow(first, idle)
    forward_out, backward_out = flow(last, idle), flow(idle, last)
    for index in range(BRIDGE):
        t = index / (BRIDGE - 1)
        frames[index] = morph(idle, first, forward_in, backward_in, t)
        frames[-BRIDGE + index] = morph(last, idle, forward_out, backward_out, t)
    output = [to_straight(frame) for frame in frames]
    for index, image in enumerate(output, 1):
        image.save(FRAMES / f"buff_alpha_{index:03d}.png", optimize=True)
    for name, color in {"dark": (32, 32, 32, 255), "white": (255, 255, 255, 255),
                        "green": (0, 170, 60, 255)}.items():
        contact(output, OUT / f"contact_{name}.png", color)
    # Opaque preview gives stable RGB edges; the PNG sequence is the actual
    # transparent deliverable (GIF palettes would reintroduce colour fringes).
    dark = [Image.alpha_composite(Image.new("RGBA", image.size, (32, 32, 32, 255)), image)
            .convert("RGB") for image in output]
    writer = cv2.VideoWriter(str(OUT / "buff_v3_alpha_preview_60fps.mp4"),
                             cv2.VideoWriter_fourcc(*"mp4v"), FPS, SIZE)
    if not writer.isOpened():
        raise RuntimeError("Could not open MP4 writer")
    for image in dark:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()
    gif = [image.resize((556, 417), Image.Resampling.LANCZOS) for image in dark]
    palette = gif[0].quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    quantized = [image.quantize(palette=palette, dither=Image.Dither.NONE) for image in gif]
    quantized[0].save(OUT / "buff_v3_alpha_preview_60fps.gif", save_all=True,
                      append_images=quantized[1:], duration=[20, 20, 10] * 20,
                      loop=0, disposal=2, optimize=False)
    (OUT / "report.json").write_text(json.dumps({
        "source": str(SOURCE), "frames": 60, "fps": 60,
        "idle_reference": str(IDLE), "bridge_frames_per_end": BRIDGE,
        "transparent_frames": str(FRAMES), "jar_exported": False,
    }, indent=2) + "\n", encoding="utf-8")
    print(f"Exported {len(output)} transparent frames and dark previews to {OUT}")


if __name__ == "__main__":
    main()
