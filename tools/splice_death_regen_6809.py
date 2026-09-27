from pathlib import Path
import csv
import cv2
import numpy as np
from PIL import Image

from analyze_death_video_splice import read_video

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT=ROOT/'death_splice_analysis'
SRC=OUT/'death_splice_frames_review_v5_hand_transition'
DST=OUT/'death_splice_frames_review_v6_regen_26_59'
DST.mkdir(exist_ok=True)
BG=np.array([213,0,132],np.float32)


def clean_bg(im):
    hsv=cv2.cvtColor(im,cv2.COLOR_BGR2HSV)
    h,s,v=cv2.split(hsv)
    mask=((h>=118)&(h<=165)&(s>=70)&(v>=35))
    out=im.copy(); out[mask]=BG.astype(np.uint8)
    return out


def robust_box(im):
    d=np.linalg.norm(im.astype(np.float32)-BG,axis=2)
    y,x=np.where(d>34)
    x0,x1=np.percentile(x,[2,98]); y0,y1=np.percentile(y,[1,99])
    return np.array([x0,y0,x1,y1],np.float32)


def fit(src,target):
    sb=robust_box(src); tb=robust_box(target)
    sw,sh=sb[2]-sb[0],sb[3]-sb[1]
    tw,th=tb[2]-tb[0],tb[3]-tb[1]
    scale=float(np.sqrt((tw/sw)*(th/sh)))
    sc=np.array([(sb[0]+sb[2])/2,(sb[1]+sb[3])/2])
    tc=np.array([(tb[0]+tb[2])/2,(tb[1]+tb[3])/2])
    trans=tc-scale*sc
    return scale,trans


def main():
    old=[cv2.imread(str(SRC/f'frame_{i:04d}.png')) for i in range(129)]
    video=list(ROOT.glob('*6809*.mp4'))[0]
    _,new=read_video(video)
    h,w=old[0].shape[:2]
    new=[clean_bg(cv2.resize(f,(w,h),interpolation=cv2.INTER_LANCZOS4)) for f in new]
    s0,t0=fit(new[0],old[25]); s1,t1=fit(new[95],old[60])
    ids=np.linspace(1,94,34).round().astype(int).tolist()
    replacement=[]
    for idx in ids:
        u=idx/95
        scale=(1-u)*s0+u*s1
        trans=(1-u)*t0+u*t1
        M=np.array([[scale,0,trans[0]],[0,scale,trans[1]]],np.float32)
        frame=cv2.warpAffine(new[idx],M,(w,h),flags=cv2.INTER_LANCZOS4,
                             borderMode=cv2.BORDER_CONSTANT,
                             borderValue=tuple(int(x) for x in BG))
        replacement.append(frame)
    frames=old[:26]+replacement+old[60:]

    for i,f in enumerate(frames): cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
    with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp); wr.writerow(['output_frame','source_video','source_frame'])
        for i in range(26): wr.writerow([i,'previous',i])
        for n,idx in enumerate(ids,26): wr.writerow([n,'6809',idx])
        for i in range(60,len(frames)): wr.writerow([i,'previous',i])

    thumbs=[]
    for f in frames:
        small=cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA)
        thumbs.append(Image.fromarray(cv2.cvtColor(small,cv2.COLOR_BGR2RGB)))
    gif=OUT/'death_splice_v7_regen_26_59_preview.gif'
    thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=42,
                   loop=0,disposal=2,optimize=False)
    mp4=OUT/'death_splice_v7_regen_26_59_24fps.mp4'
    vw=cv2.VideoWriter(str(mp4),cv2.VideoWriter_fourcc(*'mp4v'),24.0,(w,h))
    for f in frames: vw.write(f)
    vw.release()

    # Both splice neighborhoods.
    review_ids=list(range(22,30))+list(range(56,64))
    tw,th,cols=280,208,8
    sheet=np.full((2*(th+28),cols*tw,3),32,np.uint8)
    for n,i in enumerate(review_ids):
        im=cv2.resize(frames[i],(tw,th),interpolation=cv2.INTER_AREA)
        y=(n//cols)*(th+28); x=(n%cols)*tw
        sheet[y:y+th,x:x+tw]=im
        cv2.putText(sheet,f'f{i}',(x+5,y+th+19),cv2.FONT_HERSHEY_SIMPLEX,
                    .48,(240,240,240),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'death_regen_26_59_seams.png'),sheet)
    print(DST); print(gif); print(mp4); print('ids',ids)
    print('align',s0,t0.tolist(),s1,t1.tolist())

if __name__=='__main__': main()
