from pathlib import Path
import cv2
import numpy as np

from analyze_death_video_splice import read_video

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT=ROOT/'death_splice_analysis'


def load(token):
    p=list(ROOT.glob(f'*{token}*.mp4'))
    return read_video(p[0])[1]


def main():
    a=load('9507'); b=load('1520')
    # Normalize the bridge to the 9507 canvas before comparing.
    h,w=a[0].shape[:2]
    seq=[('9507',i,a[i]) for i in range(40,47)]
    seq += [('1520',i,cv2.resize(b[i],(w,h),interpolation=cv2.INTER_LANCZOS4)) for i in range(13)]
    # Fixed crop encloses abdomen, both hands, sword hilt and front knee.
    x0,y0,x1,y1=300,285,850,715
    tw,th=440,344; cols=5
    rows=(len(seq)+cols-1)//cols
    sheet=np.full((rows*(th+30),cols*tw,3),32,np.uint8)
    for n,(label,idx,f) in enumerate(seq):
        crop=f[y0:y1,x0:x1]
        im=cv2.resize(crop,(tw,th),interpolation=cv2.INTER_AREA)
        y=(n//cols)*(th+30); x=(n%cols)*tw
        sheet[y:y+th,x:x+tw]=im
        cv2.putText(sheet,f'{label} f{idx}',(x+5,y+th+20),cv2.FONT_HERSHEY_SIMPLEX,
                    .5,(240,240,240),1,cv2.LINE_AA)
    path=OUT/'death_bridge_hand_dense.png'
    cv2.imwrite(str(path),sheet)
    cv2.imwrite(str(OUT/'death_hand_microbridge_start_1520_f005.png'), b[5])
    cv2.imwrite(str(OUT/'death_hand_microbridge_end_1520_f008.png'), b[8])
    print(path)

if __name__=='__main__': main()
