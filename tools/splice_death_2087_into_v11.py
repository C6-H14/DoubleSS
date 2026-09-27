from pathlib import Path
import csv,cv2,numpy as np
from PIL import Image
from analyze_death_video_splice import read_video

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source')
OUT=ROOT/'death_splice_analysis'
OLD=OUT/'death_splice_frames_review_v10_support_20f'
DST=OUT/'death_splice_frames_review_v11_2087_start'
DST.mkdir(exist_ok=True)
BG=np.array([213,0,132],np.uint8) # BGR

def normalize_bg(im):
    hsv=cv2.cvtColor(im,cv2.COLOR_BGR2HSV);h,s,v=cv2.split(hsv)
    cand=((h>=118)&(h<=165)&(s>=60)&(v>=35)).astype(np.uint8)
    n,l,st,_=cv2.connectedComponentsWithStats(cand,8)
    border=set(np.unique(np.concatenate((l[0],l[-1],l[:,0],l[:,-1]))));bg=np.zeros(cand.shape,bool)
    for k in border:
        if k:bg|=l==k
    out=im.copy();out[bg]=BG;return out

def box(im):
    d=np.linalg.norm(im.astype(np.float32)-BG.astype(np.float32),axis=2);y,x=np.where(d>35)
    return np.array([*np.percentile(x,[2,98]),*np.percentile(y,[1,99])],np.float32)

def fit(src,tgt):
    sb=box(src);tb=box(tgt);sw,sh=sb[1]-sb[0],sb[3]-sb[2];tw,th=tb[1]-tb[0],tb[3]-tb[2]
    scale=float(np.sqrt((tw/sw)*(th/sh)));sc=np.array([(sb[0]+sb[1])/2,(sb[2]+sb[3])/2]);tc=np.array([(tb[0]+tb[1])/2,(tb[2]+tb[3])/2])
    return scale,tc-scale*sc

def main():
    old=[cv2.imread(str(p)) for p in sorted(OLD.glob('frame_*.png'))]
    video=list(ROOT.glob('*2087*.mp4'))[0];fps,new=read_video(video);h,w=old[0].shape[:2]
    new=[normalize_bg(cv2.resize(f,(w,h),interpolation=cv2.INTER_LANCZOS4)) for f in new]
    s0,t0=fit(new[0],old[0]);s1,t1=fit(new[72],old[56])
    ids=np.rint(np.linspace(0,72,56)).astype(int).tolist();part=[]
    for idx in ids:
        u=idx/72;scale=(1-u)*s0+u*s1;trans=(1-u)*t0+u*t1
        M=np.array([[scale,0,trans[0]],[0,scale,trans[1]]],np.float32)
        f=cv2.warpAffine(new[idx],M,(w,h),flags=cv2.INTER_LANCZOS4,borderMode=cv2.BORDER_CONSTANT,borderValue=tuple(map(int,BG)))
        part.append(f)
    frames=part+old[56:]
    for p in DST.glob('frame_*.png'):p.unlink()
    for i,f in enumerate(frames):cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
    with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp);wr.writerow(['output_frame','source','source_frame'])
        for i,j in enumerate(ids):wr.writerow([i,'2087',j])
        for i in range(56,len(frames)):wr.writerow([i,'previous',i])
    thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in frames]
    gif=OUT/'death_splice_v12_2087_start_preview.gif';thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=42,loop=0,disposal=2,optimize=False)
    mp4=OUT/'death_splice_v12_2087_start_24fps.mp4';vw=cv2.VideoWriter(str(mp4),cv2.VideoWriter_fourcc(*'mp4v'),24,(w,h))
    for f in frames:vw.write(f)
    vw.release()
    review_ids=list(range(48,64));tw,th=350,260;sheet=np.full((2*(th+28),8*tw,3),32,np.uint8)
    for n,i in enumerate(review_ids):
        im=cv2.resize(frames[i],(tw,th),interpolation=cv2.INTER_AREA);y=(n//8)*(th+28);x=(n%8)*tw;sheet[y:y+th,x:x+tw]=im
        cv2.putText(sheet,f'f{i}',(x+4,y+th+19),cv2.FONT_HERSHEY_SIMPLEX,.45,(245,245,245),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'death_v12_2087_seam.png'),sheet)
    print('frames',len(frames),'ids',ids);print('align',s0,t0.tolist(),s1,t1.tolist());print(gif);print(mp4);print(DST)
if __name__=='__main__':main()
