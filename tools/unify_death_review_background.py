from pathlib import Path
import shutil
import cv2
import numpy as np
from PIL import Image

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC=ROOT/'death_splice_frames_review_v2'
DST=ROOT/'death_splice_frames_review_v3_uniform_bg'
DST.mkdir(exist_ok=True)

# Reference background from the first source segment, BGR / RGB #8400D5.
TARGET=np.array([213,0,132],np.float32)


def replace_background(im):
    hsv=cv2.cvtColor(im,cv2.COLOR_BGR2HSV).astype(np.float32)
    h,s,v=cv2.split(hsv)
    # Soft confidence for the purple family. This catches all source gradients
    # while leaving blonde hair, skin, green clothing, gold and steel untouched.
    dh=np.abs(h-139)
    hue=np.clip(1-dh/27,0,1)
    sat=np.clip((s-45)/95,0,1)
    val=np.clip((v-25)/70,0,1)
    alpha=(hue*sat*val)
    # Protect very dark ink outlines even if compression shifts their hue.
    alpha[v<48]=0
    alpha=cv2.GaussianBlur(alpha,(0,0),.65)[...,None]
    out=im.astype(np.float32)*(1-alpha)+TARGET*alpha
    return np.clip(out,0,255).astype(np.uint8),alpha[...,0]


def main():
    previews=[]
    changed=[]
    for i in range(123):
        im=cv2.imread(str(SRC/f'frame_{i:04d}.png'))
        out,a=replace_background(im)
        cv2.imwrite(str(DST/f'frame_{i:04d}.png'),out)
        changed.append(float((a>.5).mean()))
        small=cv2.resize(out,(560,416),interpolation=cv2.INTER_AREA)
        previews.append(Image.fromarray(cv2.cvtColor(small,cv2.COLOR_BGR2RGB)))
    manifest=SRC/'frame_manifest.csv'
    if manifest.exists(): shutil.copy2(manifest,DST/'frame_manifest.csv')
    gif=ROOT/'death_splice_v4_uniform_background_preview.gif'
    previews[0].save(gif,save_all=True,append_images=previews[1:],duration=42,
                     loop=0,disposal=2,optimize=False)
    print(DST)
    print(gif)
    print(f'background_fraction range={min(changed):.4f}..{max(changed):.4f}')


if __name__=='__main__': main()
