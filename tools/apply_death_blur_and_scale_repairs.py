from pathlib import Path
import shutil
import cv2
import numpy as np
from PIL import Image

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC=ROOT/'death_splice_frames_review_v3_uniform_bg'
DST=ROOT/'death_splice_frames_review_v4_repaired'
DST.mkdir(exist_ok=True)
BG=np.array([213,0,132],np.float32)


def foreground_alpha(im):
    d=np.linalg.norm(im.astype(np.float32)-BG,axis=2)
    a=np.clip((d-14)/24,0,1)
    return cv2.GaussianBlur(a,(0,0),.55)[...,None]


def mild_sharpen(im):
    lab=cv2.cvtColor(im,cv2.COLOR_BGR2LAB).astype(np.float32)
    l=lab[...,0]
    low=cv2.GaussianBlur(l,(0,0),.85)
    delta=np.clip((l-low)*1.05,-14,14)
    lab[...,0]=np.clip(l+delta,0,255)
    sharp=cv2.cvtColor(lab.astype(np.uint8),cv2.COLOR_LAB2BGR)
    a=foreground_alpha(im)
    return np.clip(sharp*a+im*(1-a),0,255).astype(np.uint8)


def subject_center(im):
    d=np.linalg.norm(im.astype(np.float32)-BG,axis=2)
    y,x=np.where(d>32)
    x0,x1=np.percentile(x,[2,98]); y0,y1=np.percentile(y,[1,99])
    return (float((x0+x1)/2),float((y0+y1)/2))


def scale_subject_frame(im,scale=.968):
    h,w=im.shape[:2]
    cx,cy=subject_center(im)
    M=np.array([[scale,0,(1-scale)*cx],[0,scale,(1-scale)*cy]],np.float32)
    return cv2.warpAffine(im,M,(w,h),flags=cv2.INTER_LANCZOS4,
                          borderMode=cv2.BORDER_CONSTANT,
                          borderValue=tuple(int(x) for x in BG))


def main():
    preview=[]
    for i in range(123):
        im=cv2.imread(str(SRC/f'frame_{i:04d}.png'))
        if 26 <= i <= 47:
            im=mild_sharpen(im)
        if i >= 53:
            im=scale_subject_frame(im)
        cv2.imwrite(str(DST/f'frame_{i:04d}.png'),im)
        small=cv2.resize(im,(560,416),interpolation=cv2.INTER_AREA)
        preview.append(Image.fromarray(cv2.cvtColor(small,cv2.COLOR_BGR2RGB)))
    if (SRC/'frame_manifest.csv').exists():
        shutil.copy2(SRC/'frame_manifest.csv',DST/'frame_manifest.csv')
    gif=ROOT/'death_splice_v5_deblur_scale_repaired_preview.gif'
    preview[0].save(gif,save_all=True,append_images=preview[1:],duration=42,
                    loop=0,disposal=2,optimize=False)

    ids=[51,52,53,54,55]
    tw,th=336,250
    sheet=np.full((th+30,len(ids)*tw,3),32,np.uint8)
    for col,i in enumerate(ids):
        im=cv2.imread(str(DST/f'frame_{i:04d}.png'))
        im=cv2.resize(im,(tw,th),interpolation=cv2.INTER_AREA)
        sheet[:th,col*tw:(col+1)*tw]=im
        cv2.putText(sheet,f'f{i}',(col*tw+5,th+20),cv2.FONT_HERSHEY_SIMPLEX,
                    .5,(240,240,240),1,cv2.LINE_AA)
    cv2.imwrite(str(ROOT/'death_scale_51_55_review.png'),sheet)
    print(DST)
    print(gif)

if __name__=='__main__': main()
