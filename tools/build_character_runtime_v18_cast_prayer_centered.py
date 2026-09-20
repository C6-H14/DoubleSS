from pathlib import Path

import build_character_runtime_v17_cast_prayer_dense as config


ROOT = Path(r"D:\StSmod\DoubleSS")
builder = config.builder

builder.BASE = ROOT / "art_candidates" / "character_runtime_v17_cast_prayer_dense"
builder.BASE_IMPORT_NAME = "DoubleSS_character_v17_import_3.8.json"
builder.OUTPUT = ROOT / "art_candidates" / "character_runtime_v18_cast_prayer_centered"
builder.OUTPUT_IMPORT_NAME = "DoubleSS_character_v18_import_3.8.json"
builder.IMAGES = builder.OUTPUT / "images"
builder.EXPORT = builder.OUTPUT / "export"

# Existing idle attachments use a 700x700 canvas; prayer uses 1024x1024.
# Spine centers each attachment independently, so equivalent artwork positions
# differ by (1024-700)/2 = 162 pixels on both axes. Apply that center offset to
# the previously calibrated paste point. This preserves identical local/world
# coordinates without changing the creature hitbox.
builder.PASTE = (254, 256)


if __name__ == "__main__":
    builder.main()
