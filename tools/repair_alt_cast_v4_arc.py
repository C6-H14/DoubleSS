"""Restore smooth fading with a rounded, rather than vertical, boundary."""
import numpy as np
from PIL import Image
import repair_alt_cast_v2 as base

def arc_mask(xx,yy):
    # Circle in final compressed coordinates. The right cap bulges outward
    # at flame-axis height, while the top/bottom fade earlier.
    outx=650+(xx-650)*.42
    distance=np.hypot(outx-650,yy-240)
    t=np.clip((distance-130)/60.,0,1)
    mask=1-t*t*(3-2*t)
    mask[xx<650]=1
    return mask

if __name__=='__main__':
    base.OUT=base.ROOT/'cast_v4_arc_fade_60f_60fps'
    base.FRAMES=base.OUT/'alpha_frames_60fps'
    base.edge_mask=arc_mask
    base.main()
    yy,xx=np.mgrid[:834,:1112]
    Image.fromarray(np.rint(arc_mask(xx,yy)*255).astype(np.uint8)).save(base.OUT/'arc_mask.png')
    for ext in ['gif','mp4']:
        (base.OUT/f'cast_v2_preview_60fps.{ext}').rename(base.OUT/f'cast_v4_preview_60fps.{ext}')
