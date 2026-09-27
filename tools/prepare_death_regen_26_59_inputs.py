from pathlib import Path
import cv2
import numpy as np

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC=ROOT/'death_splice_frames_review_v5_hand_transition'
OUT=ROOT/'death_regen_26_59_inputs'
OUT.mkdir(exist_ok=True)
TARGET=np.array([213,0,132],np.uint8)


def clean_bg(im):
    hsv=cv2.cvtColor(im,cv2.COLOR_BGR2HSV)
    h,s,v=cv2.split(hsv)
    # Every purple-family pixel belongs to the flat source background.
    mask=((h>=118)&(h<=165)&(s>=70)&(v>=35)).astype(np.uint8)*255
    # Close compression pinholes, but do not expand into the subject.
    mask=cv2.morphologyEx(mask,cv2.MORPH_CLOSE,np.ones((3,3),np.uint8))
    out=im.copy(); out[mask>0]=TARGET
    return out


for src_idx,name in [(25,'death_26_59_start_frame25.png'),(60,'death_26_59_end_frame60.png')]:
    im=cv2.imread(str(SRC/f'frame_{src_idx:04d}.png'))
    cv2.imwrite(str(OUT/name),clean_bg(im))
print(OUT)
