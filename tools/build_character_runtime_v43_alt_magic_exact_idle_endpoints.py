from pathlib import Path

from PIL import Image

import build_character_runtime_v40_alt_magic_attack as config


ROOT = Path(r"D:\StSmod\DoubleSS")
builder = config.builder
builder.CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine_inner"
builder.OUT = ROOT / "art_candidates" / "character_runtime_v43_alt_magic_exact_idle_endpoints"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"
builder.TARGET_SUBJECT_HEIGHT = 322

IDLE_FRAMES = Path(r"D:\StSmod\art_preview\video\idle_v2_matte_60fps\frames_60fps")
OVERRIDE_DIR = ROOT / "art_candidates" / "alt_fencer_magic_attack_v1" / "idle_endpoint_overrides"


def make_idle_runtime_canvas(source: Path, destination: Path) -> None:
    image = Image.open(source).convert("RGBA")
    resized = image.resize((501, 501), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    canvas.paste(resized, (327, 319))
    destination.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(destination, optimize=True)


if __name__ == "__main__":
    start = OVERRIDE_DIR / "idle_start_runtime_raw.png"
    end = OVERRIDE_DIR / "idle_end_runtime_raw.png"
    make_idle_runtime_canvas(IDLE_FRAMES / "frame_0001.png", start)
    make_idle_runtime_canvas(IDLE_FRAMES / "frame_0241.png", end)
    builder.FRAME_OVERRIDES = {1: start, 57: end}
    builder.main()
