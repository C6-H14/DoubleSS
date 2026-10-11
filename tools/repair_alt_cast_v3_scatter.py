"""Seeded scan-line scatter mask, increasing transparency to the right."""
import cv2
import numpy as np
from PIL import Image
import repair_alt_cast_v2 as base

def scattered_mask(xx,yy):
    h,w=xx.shape
    rng=np.random.default_rng(1625)
    dots=np.zeros((h,w),np.float32)
    # Scan from left to right; each column gains more randomly placed dots.
    # The fixed seed yields the same mask throughout the clip (no flicker).
    for x in range(850,min(w,1100)):
        t=(x-850)/250.
        count=int(1+110*t*t)
        for y in rng.integers(0,h,count):
            cv2.circle(dots,(x,int(y)),int(rng.integers(2,6)),1.,-1)
    dots=cv2.GaussianBlur(dots,(5,5),.8)
    progress=np.clip((xx-850)/250.,0,1)
    # Broad random shapes break up the fade front, while the envelope forces
    # the clipped source edge fully transparent rather than leaving a strip.
    coarse=rng.random((max(2,h//18),max(2,w//26))).astype(np.float32)
    noise=cv2.resize(coarse,(w,h),interpolation=cv2.INTER_CUBIC)
    irregular=np.clip(progress+(noise-.5)*.6*np.sin(np.pi*progress),0,1)
    envelope=1-irregular*irregular*(3-2*irregular)
    mask=envelope*(1-dots*.95)
    mask[xx<850]=1
    mask[xx>=1100]=0
    return mask

if __name__=='__main__':
    base.OUT=base.ROOT/'cast_v3_scatter_fade_60f_60fps'
    base.FRAMES=base.OUT/'alpha_frames_60fps'
    base.edge_mask=scattered_mask
    base.main()
    yy,xx=np.mgrid[:834,:1112]
    Image.fromarray(np.rint(scattered_mask(xx,yy)*255).astype(np.uint8)).save(base.OUT/'scatter_mask.png')
    for ext in ['gif','mp4']:
        (base.OUT/f'cast_v2_preview_60fps.{ext}').rename(base.OUT/f'cast_v3_preview_60fps.{ext}')
