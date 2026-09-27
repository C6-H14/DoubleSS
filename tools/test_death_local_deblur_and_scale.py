from pathlib import Path
import cv2
import numpy as np

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC=ROOT/'death_splice_frames_review_v3_uniform_bg'
OUT=ROOT/'death_local_repair_tests'
OUT.mkdir(exist_ok=True)
BG=np.array([213,0,132],np.float32)


def fg_alpha(im):
    d=np.linalg.norm(im.astype(np.float32)-BG,axis=2)
    a=np.clip((d-14)/24,0,1)
    return cv2.GaussianBlur(a,(0,0),.55)[...,None]


def sharpen(im,sigma,amount,limit):
    lab=cv2.cvtColor(im,cv2.COLOR_BGR2LAB).astype(np.float32)
    l=lab[...,0]
    low=cv2.GaussianBlur(l,(0,0),sigma)
    delta=np.clip((l-low)*amount,-limit,limit)
    lab[...,0]=np.clip(l+delta,0,255)
    sharp=cv2.cvtColor(lab.astype(np.uint8),cv2.COLOR_LAB2BGR)
    a=fg_alpha(im)
    return np.clip(sharp*a+im*(1-a),0,255).astype(np.uint8)


def main():
    ids=[26,31,36,41,46]
    tw,th=360,267
    sheet=np.full((len(ids)*(th+30),3*tw,3),32,np.uint8)
    for row,i in enumerate(ids):
        im=cv2.imread(str(SRC/f'frame_{i:04d}.png'))
        variants=[im,sharpen(im,.85,1.05,14),sharpen(im,1.15,1.55,20)]
        names=['original','mild','strong']
        for col,(v,name) in enumerate(zip(variants,names)):
            cv2.imwrite(str(OUT/f'frame_{i:04d}_{name}.png'),v)
            thumb=cv2.resize(v,(tw,th),interpolation=cv2.INTER_AREA)
            y=row*(th+30); x=col*tw
            sheet[y:y+th,x:x+tw]=thumb
            cv2.putText(sheet,f'f{i} {name}',(x+5,y+th+20),cv2.FONT_HERSHEY_SIMPLEX,
                        .48,(240,240,240),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'deblur_comparison.png'),sheet)
    print(OUT/'deblur_comparison.png')

if __name__=='__main__': main()
