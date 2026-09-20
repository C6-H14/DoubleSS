from pathlib import Path

import build_character_runtime_v16_cast_prayer as builder


ROOT = Path(r"D:\StSmod\DoubleSS")

# Extend v16 while replacing cast_prayer with the denser, correctly anchored
# 41-frame sequence. Other animations and attachments remain byte-identical.
builder.BASE = ROOT / "art_candidates" / "character_runtime_v16_cast_prayer"
builder.BASE_IMPORT_NAME = "DoubleSS_character_v16_import_3.8.json"
builder.SOURCE = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v6" / "alpha_frames"
builder.OUTPUT = ROOT / "art_candidates" / "character_runtime_v17_cast_prayer_dense"
builder.OUTPUT_IMPORT_NAME = "DoubleSS_character_v17_import_3.8.json"
builder.IMAGES = builder.OUTPUT / "images"
builder.EXPORT = builder.OUTPUT / "export"
builder.EXPECTED_FRAMES = 41

# Calibrated against attack_sword_01 (the idle attachment): match its 327 px
# visible height, horizontal center 361.5 and ground line y=560.
builder.SCALE = 0.566
builder.PASTE = (92, 94)

# The generated final standing frame drifts 15 px right and 26 px above the
# calibrated idle anchor. Correct that drift gradually during the recovery so
# returning to idle has no visible snap.
builder.FRAME_OFFSETS = {}
for index in range(30, 42):
    progress = (index - 30) / 11.0
    builder.FRAME_OFFSETS[index] = (round(-15 * progress), round(26 * progress))


if __name__ == "__main__":
    builder.main()
