from pathlib import Path

import build_character_runtime_v40_alt_magic_attack as config


ROOT = Path(r"D:\StSmod\DoubleSS")
builder = config.builder
builder.CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine_inner"
builder.OUT = ROOT / "art_candidates" / "character_runtime_v41_alt_magic_idle_scale"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"

# Match the measured inner idle runtime height exactly.
builder.TARGET_SUBJECT_HEIGHT = 342


if __name__ == "__main__":
    builder.main()
