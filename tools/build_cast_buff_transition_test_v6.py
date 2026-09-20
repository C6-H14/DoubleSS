from pathlib import Path
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\new_animation_candidates_v1")
SOURCE = ROOT / "cast_buff_transition_test_v4" / "pink_frames"
OUTPUT = ROOT / "cast_buff_transition_test_v6"
FRAMES = OUTPUT / "pink_frames"
GENERATED = Path(
    r"C:\Users\C6H14\.codex\generated_images\01a0a9f4-80d7-7af2-a6bb-c01d6d7b4860"
)

# Image-generation calls completed in prompt order.  The first four are the
# 20/40/60/80% poses between V4 frames 46 and 51; the final three are the
# 25/50/75% poses between V4 frames 56 and 60.
REPLACEMENTS = {
    47: "exec-95c0fb9c-0dde-4703-8896-5e24cb88eaa5.png",
    48: "exec-271f6780-d5ce-4088-bc1d-7c043198537f.png",
    49: "exec-f7d2aece-38de-4435-9569-5f6e862143b0.png",
    50: "exec-113bdd23-412e-46c0-8c5f-05bda26619b7.png",
    57: "exec-898951b5-5099-4be3-ab07-7bd3b79d6cbc.png",
    58: "exec-27ab9de1-3f1c-476a-9a13-df93a3d2c5b9.png",
    59: "exec-304cddca-31ae-4c8c-87b6-07d7e7611106.png",
}


def fit_to_canvas(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    """The generated frames have the same aspect ratio; resample exactly once."""
    return image.convert("RGB").resize(size, Image.Resampling.LANCZOS)


def pose_metrics(image: Image.Image) -> tuple[float, float, float]:
    """Return character top, shoe bottom and shoe horizontal center."""
    a = np.asarray(image.convert("RGB"))
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    character = ((r < 180) & (g < 160) & (b < 150)) | (
        (r > 120) & (g > 70) & (g < 210) & (b < 120)
    )
    ys, _ = np.where(character)
    shoe = (np.indices(r.shape)[0] > 580) & (r < 150) & (g < 110) & (b < 110)
    shoe_y, shoe_x = np.where(shoe)
    return float(ys.min()), float(shoe_y.max()), float(shoe_x.mean())


def normalize_pose(
    image: Image.Image,
    left_metrics: tuple[float, float, float],
    right_metrics: tuple[float, float, float],
    t: float,
) -> Image.Image:
    top, bottom, shoe_x = pose_metrics(image)
    target_top = (1.0 - t) * left_metrics[0] + t * right_metrics[0]
    target_bottom = (1.0 - t) * left_metrics[1] + t * right_metrics[1]
    target_shoe_x = (1.0 - t) * left_metrics[2] + t * right_metrics[2]
    scale = (target_bottom - target_top) / (bottom - top)
    # Uniform scale around the current shoe center/bottom, then translate that
    # anchor onto its linearly interpolated position.
    tx = target_shoe_x - scale * shoe_x
    ty = target_bottom - scale * bottom
    matrix = np.array([[scale, 0.0, tx], [0.0, scale, ty]], dtype=np.float32)
    src = np.asarray(image.convert("RGB"))
    corner = tuple(int(v) for v in np.median(src[:32, :32], axis=(0, 1)))
    warped = cv2.warpAffine(
        src,
        matrix,
        image.size,
        flags=cv2.INTER_LANCZOS4,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=corner,
    )
    return Image.fromarray(warped, "RGB")


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True, exist_ok=True)

    frames = [Image.open(path).convert("RGB") for path in sorted(SOURCE.glob("cast_buff_*.png"))]
    if len(frames) != 67:
        raise RuntimeError(f"Expected 67 V4 frames, got {len(frames)}")
    canvas = frames[0].size

    group_a = {47: 0.2, 48: 0.4, 49: 0.6, 50: 0.8}
    group_b = {57: 0.25, 58: 0.5, 59: 0.75}
    metrics_a = (pose_metrics(frames[45]), pose_metrics(frames[50]))
    metrics_b = (pose_metrics(frames[55]), pose_metrics(frames[59]))
    for one_based, filename in REPLACEMENTS.items():
        image = fit_to_canvas(Image.open(GENERATED / filename), canvas)
        if one_based in group_a:
            image = normalize_pose(image, metrics_a[0], metrics_a[1], group_a[one_based])
        else:
            image = normalize_pose(image, metrics_b[0], metrics_b[1], group_b[one_based])
        frames[one_based - 1] = image

    for index, image in enumerate(frames, 1):
        image.save(FRAMES / f"cast_buff_{index:03d}.png", compress_level=2)

    frames[0].save(
        OUTPUT / "cast_buff_transition_test_v6.gif",
        save_all=True,
        append_images=frames[1:],
        duration=20,
        loop=0,
        disposal=1,
        optimize=False,
    )

    writer = cv2.VideoWriter(
        str(OUTPUT / "cast_buff_transition_test_v6.mp4"),
        cv2.VideoWriter_fourcc(*"mp4v"),
        50.0,
        canvas,
    )
    if not writer.isOpened():
        raise RuntimeError("Unable to create MP4 preview")
    for image in frames:
        writer.write(cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
    writer.release()

    chosen = list(range(45, 53)) + list(range(55, 62))
    tile_w, tile_h, cols = 336, 250, 5
    rows = (len(chosen) + cols - 1) // cols
    sheet = Image.new("RGB", (tile_w * cols, (tile_h + 26) * rows), "#171717")
    draw = ImageDraw.Draw(sheet)
    for slot, index in enumerate(chosen):
        image = frames[index - 1].resize((tile_w, tile_h), Image.Resampling.LANCZOS)
        x = (slot % cols) * tile_w
        y = (slot // cols) * (tile_h + 26)
        sheet.paste(image, (x, y))
        label = "AI in-between" if index in REPLACEMENTS else "endpoint/original"
        draw.text((x + 5, y + tile_h + 4), f"frame {index}: {label}", fill="white")
    sheet.save(OUTPUT / "cast_buff_v6_sequences.jpg", quality=94)

    (OUTPUT / "manifest.txt").write_text(
        "variant=cast_buff_transition_test_v6\n"
        "source=cast_buff_transition_test_v4\n"
        "frame_count=67\n"
        "fps=50\n"
        "duration=1.340s\n"
        "replaced_47_to_50=clean generated in-betweens at 20/40/60/80 percent between 46 and 51\n"
        "replaced_57_to_59=clean generated in-betweens at 25/50/75 percent between 56 and 60\n"
        "normalization=uniform affine alignment to interpolated character height, shoe baseline, and shoe center\n"
        "rejected=V5 optical-flow frames due to double-sword ghosting\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
