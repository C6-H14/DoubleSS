from pathlib import Path

import build_character_runtime_v40_alt_magic_attack as config


ROOT = Path(r"D:\StSmod\DoubleSS")
builder = config.builder
builder.CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine_inner"
builder.OUT = ROOT / "art_candidates" / "character_runtime_v42_alt_magic_head_scale"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"

# Match the endpoint head/hair scale to idle (magic was about 6% wider).
builder.TARGET_SUBJECT_HEIGHT = 322


if __name__ == "__main__":
    builder.main()
