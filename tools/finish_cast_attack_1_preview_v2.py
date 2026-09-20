from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO = ROOT / "art_candidates" / "video_source" / "cast_v1" / "cast_attack_1_source.mp4"
MATTE = (
    ROOT
    / "art_candidates"
    / "matting_tests"
    / "matanyone_cast_attack_1_v1"
    / "output_768"
    / "cast_attack_1_source_cast768"
)
MASTER = (
    ROOT
    / "art_candidates"
    / "matting_tests"
    / "matanyone_attack_pink_v1"
    / "selected47_50fps"
    / "transparent_original_endpoint_aligned.png"
)
OUT = ROOT / "art_candidates" / "video_source" / "cast_v1" / "preview_v2_41f_50fps"
FRAMES = OUT / "frames"
SOURCE_INDICES = list(range(6, 87, 2))
SIZE = (1024, 768)
FPS = 50
VERSION = "v2"


def smoothstep(edge0, edge1, value):
    t = np.clip((value - edge0) / (edge1 - edge0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def gold_layer(rgb):
    data = rgb.astype(np.float32)
    red, green, blue = data[..., 0], data[..., 1], data[..., 2]
    goldness = np.minimum(red - blue, green - blue)
    brightness = np.maximum(red, green)
    alpha = smoothstep(5.0, 62.0, goldness) * smoothstep(105.0, 220.0, brightness)
    x = np.arange(rgb.shape[1], dtype=np.float32)[None, :]
    y = np.arange(rgb.shape[0], dtype=np.float32)[:, None]
    alpha *= smoothstep(rgb.shape[1] * 0.57, rgb.shape[1] * 0.65, x)
    alpha *= 1.0 - smoothstep(rgb.shape[0] * 0.68, rgb.shape[0] * 0.82, y)
    alpha = cv2.GaussianBlur(alpha, (0, 0), 0.55)
    alpha[alpha < 0.025] = 0.0
    alpha[alpha > 0.94] = 1.0
    alpha_u8 = np.uint8(np.round(alpha * 255.0))

    # Remove tiny codec flecks while keeping the rune, ring and star-shaped fade.
    count, labels, stats, _ = cv2.connectedComponentsWithStats((alpha_u8 > 5).astype(np.uint8), 8)
    keep = np.zeros_like(alpha_u8)
    for component in range(1, count):
        if stats[component, cv2.CC_STAT_AREA] >= 8:
            keep[labels == component] = alpha_u8[labels == component]

    solid = keep >= 175
    clean_rgb = rgb.copy()
    if solid.any():
        _, nearest_index = distance_transform_edt(~solid, return_indices=True)
        nearest = rgb[nearest_index[0], nearest_index[1]]
        edge = (keep > 0) & (keep < 175)
        clean_rgb[edge] = nearest[edge]
    clean_rgb[keep == 0] = 0
    return clean_rgb, keep


def fit_endpoint(master, target):
    target_bbox = target.getchannel("A").getbbox()
    master_bbox = master.getchannel("A").getbbox()
    if target_bbox is None or master_bbox is None:
        raise RuntimeError("Empty endpoint")
    crop = master.crop(master_bbox)
    x0, y0, x1, y1 = target_bbox
    crop = crop.resize((x1 - x0, y1 - y0), Image.Resampling.LANCZOS)
    result = Image.new("RGBA", target.size, (0, 0, 0, 0))
    result.alpha_composite(crop, (x0, y0))
    return result


def on_background(image, color):
    bg = Image.new("RGBA", image.size, (*color, 255))
    return Image.alpha_composite(bg, image).convert("RGB")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    FRAMES.mkdir(parents=True, exist_ok=True)
    for old in FRAMES.glob("cast_attack_1_*.png"):
        old.unlink()

    capture = cv2.VideoCapture(str(VIDEO))
    if not capture.isOpened():
        raise RuntimeError(f"Cannot open {VIDEO}")
    frames = []
    for source_index in SOURCE_INDICES:
        capture.set(cv2.CAP_PROP_POS_FRAMES, source_index)
        ok, bgr = capture.read()
        if not ok:
            raise RuntimeError(f"Cannot read source frame {source_index + 1}")
        bgr = cv2.resize(bgr, SIZE, interpolation=cv2.INTER_AREA)
        source_rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)

        fgr = np.asarray(Image.open(MATTE / "fgr" / f"{source_index:05d}.png").convert("RGB"))
        person_alpha = np.array(
            Image.open(MATTE / "pha" / f"{source_index:05d}.png").convert("L"), copy=True
        )
        person_alpha[person_alpha < 8] = 0
        gold_rgb, gold_alpha = gold_layer(source_rgb)

        pa = person_alpha.astype(np.float32) / 255.0
        ga = gold_alpha.astype(np.float32) / 255.0
        out_alpha = ga + pa * (1.0 - ga)
        premul = gold_rgb.astype(np.float32) * ga[..., None]
        premul += fgr.astype(np.float32) * pa[..., None] * (1.0 - ga[..., None])
        out_rgb = np.divide(
            premul,
            out_alpha[..., None],
            out=np.zeros_like(premul),
            where=out_alpha[..., None] > 1e-6,
        )
        rgba = np.dstack((np.uint8(np.clip(np.round(out_rgb), 0, 255)), np.uint8(np.round(out_alpha * 255))))
        frames.append(Image.fromarray(rgba, "RGBA"))
    capture.release()

    endpoint = fit_endpoint(Image.open(MASTER).convert("RGBA"), frames[0])
    frames[0] = endpoint
    frames[-1] = endpoint.copy()
    for index, frame in enumerate(frames, 1):
        frame.save(FRAMES / f"cast_attack_1_{index:02d}.png", optimize=True)

    black = [on_background(frame, (0, 0, 0)) for frame in frames]
    black[0].save(
        OUT / f"cast_attack_1_{VERSION}_{len(frames)}f_{FPS}fps_black.gif",
        save_all=True,
        append_images=black[1:],
        duration=20,
        loop=0,
        disposal=2,
    )

    thumb_w, thumb_h, cols = 256, 192, 7
    rows = (len(frames) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + 22)), "black")
    draw = ImageDraw.Draw(sheet)
    for i, frame in enumerate(frames):
        thumb = on_background(frame, (0, 0, 0)).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = (i % cols) * thumb_w
        y = (i // cols) * (thumb_h + 22)
        sheet.paste(thumb, (x, y))
        draw.text((x + 5, y + thumb_h + 3), f"{i + 1:02d} <- src {SOURCE_INDICES[i] + 1:03d}", fill="white")
    sheet.save(OUT / "black_contact_sheet.png", optimize=True)

    (OUT / "metadata.json").write_text(
        json.dumps(
            {
                "source": str(VIDEO),
                "source_frames_one_based": [i + 1 for i in SOURCE_INDICES],
                "output_frames": len(frames),
                "fps": FPS,
                "duration_seconds": len(frames) / FPS,
                "person_matte": "MatAnyone",
                "spell_effect": "gold color extraction in right spell area",
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"frames={len(frames)} duration={len(frames) / FPS:.2f}s")
    print(OUT)


if __name__ == "__main__":
    main()
