"""Clean distant spark trails without altering the accepted body or sword."""
import json
import numpy as np
from PIL import Image
import repair_alt_buff_v4 as base

original_repair = base.repair

def clean_repair(rgba, index, donor):
    image, stats = original_repair(rgba, index, donor)
    pixels = np.array(image)
    yy, xx = np.mgrid[:pixels.shape[0], :pixels.shape[1]]
    # The extended left fingertips stay left of x=610. All content past
    # x=630 in this corridor is the unwanted distant spark outline.
    region = (xx >= 612) & (yy >= 250) & (yy <= 440)
    fade = np.clip((632 - xx) / 20., 0, 1)
    before = pixels[:, :, 3].copy()
    pixels[:, :, 3][region] = np.rint(before[region] * fade[region]).astype(np.uint8)
    pixels[pixels[:, :, 3] == 0] = 0
    stats['spark_alpha_removed'] = int((before.astype(int)-pixels[:, :, 3].astype(int)).sum())
    return Image.fromarray(pixels), stats

if __name__ == '__main__':
    base.OUT = base.ROOT / 'buff_v5_sparks_clean_60f_60fps'
    base.FRAMES = base.OUT / 'alpha_frames_60fps'
    base.repair = clean_repair
    base.main()
    for ext in ['gif', 'mp4']:
        (base.OUT / f'buff_v4_preview_60fps.{ext}').rename(base.OUT / f'buff_v5_preview_60fps.{ext}')
