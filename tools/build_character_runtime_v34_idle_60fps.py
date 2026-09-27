from pathlib import Path

from PIL import Image

import build_character_runtime_v33_alt_idle as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = Path(r"D:\StSmod\art_preview\video\idle_v2_matte_60fps\frames_60fps")
OUT = ROOT / "art_candidates" / "character_runtime_v34_idle_60fps"


def alpha_union(paths: list[Path]) -> tuple[int, int, int, int]:
    boxes = []
    for path in paths:
        box = Image.open(path).convert("RGBA").getchannel("A").getbbox()
        if box is None:
            raise RuntimeError(f"Empty alpha frame: {path}")
        boxes.append(box)
    return (
        min(box[0] for box in boxes),
        min(box[1] for box in boxes),
        max(box[2] for box in boxes),
        max(box[3] for box in boxes),
    )


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    if len(paths) != 241:
        raise RuntimeError(f"Expected 241 frames, found {len(paths)}")
    builder.SOURCE = SOURCE
    builder.SOURCE_PATTERN = "frame_*.png"
    builder.ATTACHMENT_PREFIX = "alt_idle"
    builder.OUT = OUT
    builder.IMAGES = OUT / "images"
    builder.EXPORT = OUT / "export"
    builder.FPS = 60.0
    builder.EXPECTED = len(paths)
    builder.UNION_BBOX = alpha_union(paths)
    builder.main()


if __name__ == "__main__":
    main()
