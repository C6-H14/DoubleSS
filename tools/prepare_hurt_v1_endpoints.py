from pathlib import Path

import cv2
from PIL import Image

from matte_alt_idle_v1 import matte


ROOT = Path(r"D:\StSmod")
START_SOURCE = ROOT / "art_preview" / "video" / "idle_endpoint_palette_v2" / "idle_start.png"
RAW_END = ROOT / "art_preview" / "hurt_v1" / "hurt_end_raw_v4.png"
OUT = ROOT / "art_preview" / "hurt_v1"
CANVAS = (960, 960)
BACKGROUND = (58, 0, 112, 255)


def keyed(path: Path) -> Image.Image:
    bgr = cv2.imread(str(path), cv2.IMREAD_COLOR)
    if bgr is None:
        raise RuntimeError(f"Could not read {path}")
    if (bgr.shape[1], bgr.shape[0]) != CANVAS:
        bgr = cv2.resize(bgr, CANVAS, interpolation=cv2.INTER_LANCZOS4)
    return matte(bgr)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    start = keyed(START_SOURCE)
    end = keyed(RAW_END)
    start_box = start.getchannel("A").getbbox()
    end_box = end.getchannel("A").getbbox()
    if start_box is None or end_box is None:
        raise RuntimeError("Empty keyed endpoint")

    target_h = start_box[3] - start_box[1]
    end_h = end_box[3] - end_box[1]
    scale = target_h / end_h
    resized = end.resize(
        (round(end.width * scale), round(end.height * scale)),
        Image.Resampling.LANCZOS,
    )
    resized_box = resized.getchannel("A").getbbox()
    assert resized_box is not None
    target_center_x = (start_box[0] + start_box[2]) / 2
    resized_center_x = (resized_box[0] + resized_box[2]) / 2
    paste_x = round(target_center_x - resized_center_x)
    paste_y = round(start_box[3] - resized_box[3])
    aligned = Image.new("RGBA", CANVAS, (0, 0, 0, 0))
    aligned.alpha_composite(resized, (paste_x, paste_y))

    background = Image.new("RGBA", CANVAS, BACKGROUND)
    start_frame = Image.alpha_composite(background, start)
    end_frame = Image.alpha_composite(background, aligned)
    start_frame.convert("RGB").save(OUT / "hurt_start.png")
    end_frame.convert("RGB").save(OUT / "hurt_end.png")
    aligned.save(OUT / "hurt_end_transparent.png")
    print({
        "start_bbox": start_box,
        "end_bbox_before": end_box,
        "scale": scale,
        "paste": (paste_x, paste_y),
    })


if __name__ == "__main__":
    main()
