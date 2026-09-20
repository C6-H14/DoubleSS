from collections import deque
from pathlib import Path
import sys

import numpy as np
from PIL import Image


def components(mask: np.ndarray):
    h, w = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    for y0, x0 in zip(*np.where(mask & ~seen)):
        if seen[y0, x0]:
            continue
        q = deque([(int(y0), int(x0))])
        seen[y0, x0] = True
        xs, ys = [], []
        while q:
            y, x = q.popleft()
            xs.append(x); ys.append(y)
            for ny, nx in ((y-1,x),(y+1,x),(y,x-1),(y,x+1)):
                if 0 <= ny < h and 0 <= nx < w and mask[ny,nx] and not seen[ny,nx]:
                    seen[ny,nx] = True; q.append((ny,nx))
        yield len(xs), (min(xs),min(ys),max(xs),max(ys))


p = Path(sys.argv[1])
a = np.asarray(Image.open(p).convert('RGBA'))
rgb, alpha = a[...,:3], a[...,3]
for threshold in (230, 238, 245, 250):
    chroma = rgb.max(2)-rgb.min(2)
    mask = (alpha > 128) & (rgb.min(2) >= threshold) & (chroma <= 12)
    cs = sorted(components(mask), reverse=True)[:30]
    print('threshold', threshold, 'pixels', int(mask.sum()))
    for size,bbox in cs:
        if size >= 4: print(size,bbox)
