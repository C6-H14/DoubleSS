from pathlib import Path

import finish_cast_attack_1_preview_v2 as builder


# Start from the approved v2 mapping: current frames 1..41 correspond to
# source-video frames 7,9,...,87. Remove odd current frame numbers 13..27,
# then densify the recovery segment between original current frames 36..40
# with source-video midpoint frames 78,80,82,84.
base = list(range(6, 87, 2))  # zero-based source indices
remove_current = set(range(13, 28, 2))
selected = []
for current_number, source_index in enumerate(base, start=1):
    if current_number not in remove_current:
        selected.append(source_index)
    if 36 <= current_number < 40:
        next_source_index = base[current_number]
        selected.append((source_index + next_source_index) // 2)

builder.SOURCE_INDICES = selected
builder.OUT = (
    builder.ROOT
    / "art_candidates"
    / "video_source"
    / "cast_v1"
    / "preview_v3_37f_50fps"
)
builder.FRAMES = builder.OUT / "frames"
builder.VERSION = "v3"


if __name__ == "__main__":
    builder.main()
