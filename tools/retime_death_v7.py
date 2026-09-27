from pathlib import Path
import csv
import cv2
import numpy as np
from PIL import Image

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC = ROOT / "death_splice_frames_review_v6_regen_26_59"
DST = ROOT / "death_splice_frames_review_v8_retimed_phase_adjusted"
DST.mkdir(exist_ok=True)
BG = np.array([213, 0, 132], np.uint8)


def mask(im):
    d = np.linalg.norm(im.astype(np.float32) - BG.astype(np.float32), axis=2)
    return d > 34


def motion(a, b):
    aa = cv2.resize(a, (280,208), interpolation=cv2.INTER_AREA)
    bb = cv2.resize(b, (280,208), interpolation=cv2.INTER_AREA)
    ma = cv2.resize(mask(a).astype(np.uint8), (280,208), interpolation=cv2.INTER_NEAREST)
    mb = cv2.resize(mask(b).astype(np.uint8), (280,208), interpolation=cv2.INTER_NEAREST)
    mm = cv2.dilate((ma|mb), np.ones((7,7),np.uint8)) > 0
    ga = cv2.cvtColor(aa,cv2.COLOR_BGR2GRAY); gb=cv2.cvtColor(bb,cv2.COLOR_BGR2GRAY)
    flow=cv2.calcOpticalFlowFarneback(ga,gb,None,.5,3,15,3,5,1.2,0)
    vals=np.linalg.norm(flow,axis=2)[mm]
    return float(np.percentile(vals,75)) if vals.size else 0.0


def tween(a,b,t):
    if t <= .001: return a.copy()
    if t >= .999: return b.copy()
    # Dense bidirectional flow at half resolution is stable enough for anime contours,
    # while forcing clean purple back to the exact key color.
    h,w=a.shape[:2]; size=(w//2,h//2)
    x=cv2.resize(a,size,interpolation=cv2.INTER_AREA)
    y=cv2.resize(b,size,interpolation=cv2.INTER_AREA)
    gx=cv2.cvtColor(x,cv2.COLOR_BGR2GRAY); gy=cv2.cvtColor(y,cv2.COLOR_BGR2GRAY)
    f=cv2.calcOpticalFlowFarneback(gx,gy,None,.5,4,19,4,7,1.3,0)
    r=cv2.calcOpticalFlowFarneback(gy,gx,None,.5,4,19,4,7,1.3,0)
    xx,yy=np.meshgrid(np.arange(size[0],dtype=np.float32),np.arange(size[1],dtype=np.float32))
    wa=cv2.remap(x,xx-f[...,0]*t,yy-f[...,1]*t,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=tuple(BG.tolist()))
    wb=cv2.remap(y,xx-r[...,0]*(1-t),yy-r[...,1]*(1-t),cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=tuple(BG.tolist()))
    out=cv2.addWeighted(wa,1-t,wb,t,0)
    out=cv2.resize(out,(w,h),interpolation=cv2.INTER_LANCZOS4)
    hsv=cv2.cvtColor(out,cv2.COLOR_BGR2HSV); H,S,V=cv2.split(hsv)
    purple=(H>=118)&(H<=165)&(S>=70)&(V>=35)
    out[purple]=BG
    return out


def resample_segment(frames, scores, lo, hi, intervals):
    # Arc-length retiming: preserve a small time floor for subtle motion, cap spikes,
    # and devote extra frames to genuine large pose changes.
    raw=np.array(scores[lo+1:hi+1],np.float32)
    cap=max(float(np.percentile(raw,80)),.35)
    weights=.22 + np.minimum(raw,cap)
    cum=np.r_[0,np.cumsum(weights)]
    targets=np.linspace(0,cum[-1],intervals+1)
    positions=[]; out=[]
    for q in targets:
        k=min(int(np.searchsorted(cum,q,side='right')-1),len(weights)-1)
        k=max(k,0)
        den=cum[k+1]-cum[k]
        t=0 if den<=1e-8 else float((q-cum[k])/den)
        pos=lo+k+t
        positions.append(pos)
        out.append(tween(frames[lo+k],frames[lo+k+1],t))
    out[-1]=frames[hi].copy(); positions[-1]=float(hi)
    return out,positions


def main():
    frames=[cv2.imread(str(p)) for p in sorted(SRC.glob('frame_*.png'))]
    scores=[0.0]+[motion(a,b) for a,b in zip(frames,frames[1:])]
    # interval allocation by dramatic phase:
    # recoil / kneel / settle / brace / lower / loss of strength / fall / final settle
    phases=[(0,25,22),(25,39,12),(39,59,10),(59,75,10),
            (75,96,21),(96,110,18),(110,123,16),(123,128,5)]
    output=[]; mapping=[]
    for n,(lo,hi,count) in enumerate(phases):
        part,pos=resample_segment(frames,scores,lo,hi,count)
        if n: part=part[1:]; pos=pos[1:]
        output.extend(part); mapping.extend(pos)
    for i,f in enumerate(output): cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
    with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp); wr.writerow(['output_frame','source_position']); wr.writerows(enumerate(mapping))

    h,w=output[0].shape[:2]
    gif_frames=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in output]
    gif=ROOT/'death_splice_v9_phase_adjusted_preview.gif'
    gif_frames[0].save(gif,save_all=True,append_images=gif_frames[1:],duration=42,loop=0,disposal=2,optimize=False)
    mp4=ROOT/'death_splice_v9_phase_adjusted_24fps.mp4'
    vw=cv2.VideoWriter(str(mp4),cv2.VideoWriter_fourcc(*'mp4v'),24,(w,h))
    for f in output: vw.write(f)
    vw.release()

    # phase contact sheet
    ids=list(range(0,len(output),4)); tw,th,cols=280,208,8
    rows=(len(ids)+cols-1)//cols; sheet=np.full((rows*(th+28),cols*tw,3),32,np.uint8)
    for n,i in enumerate(ids):
        im=cv2.resize(output[i],(tw,th),interpolation=cv2.INTER_AREA)
        y=(n//cols)*(th+28); x=(n%cols)*tw; sheet[y:y+th,x:x+tw]=im
        cv2.putText(sheet,f'f{i} src={mapping[i]:.1f}',(x+4,y+th+19),cv2.FONT_HERSHEY_SIMPLEX,.43,(245,245,245),1,cv2.LINE_AA)
    cv2.imwrite(str(ROOT/'death_v9_phase_adjusted_contact.png'),sheet)
    print('frames',len(output),'seconds',len(output)/24)
    print(gif); print(mp4); print(DST)

if __name__=='__main__': main()
