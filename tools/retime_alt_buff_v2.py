"""Retimed preview for regenerated buff source 4191, without false zoom correction."""

from pathlib import Path

import retime_alt_buff_v1 as preview


preview.SOURCE = next(Path(r"D:\StSmod\art_preview\video").glob("jimeng-2026-09-27-4191-*.mp4"))
preview.OUT = Path(r"D:\StSmod\art_preview\alternate_fencer_buff\buff_v2_60f_60fps")
preview.FRAMES = preview.OUT / "frames_60fps"
preview.NORMALIZE_SCALE = False
preview.BASENAME = "buff_v2_60f_60fps"
preview.SELECTED = (
    preview.phase(0, 29, 18)
    + preview.phase(30, 66, 24)
    + preview.phase(67, 96, 18)
)
assert len(preview.SELECTED) == 60 and len(set(preview.SELECTED)) == 60


if __name__ == "__main__":
    preview.main()
