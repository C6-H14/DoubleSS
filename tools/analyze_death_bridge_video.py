from pathlib import Path
import json
import cv2
import numpy as np

from analyze_death_video_splice import read_video, contact_sheet, descriptor, pair_cost

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT = ROOT / "death_splice_analysis"
OUT.mkdir(exist_ok=True)


def one(token):
    p = list(ROOT.glob(f"*{token}*.mp4"))
    if len(p) != 1:
        raise RuntimeError((token,p))
    fps, frames = read_video(p[0])
    return p[0],fps,frames


def ranked(left, right, left_range, right_range, limit=16):
    dl=[descriptor(x) for x in left]
    dr=[descriptor(x) for x in right]
    allc=[]
    for i in left_range:
        if dl[i] is None: continue
        for j in right_range:
            if dr[j] is None: continue
            allc.append((pair_cost(dl[i],dr[j]),i,j))
    allc.sort()
    out=[]
    for x in allc:
        if all(abs(x[1]-y[1])>2 or abs(x[2]-y[2])>2 for y in out):
            out.append(x)
        if len(out)>=limit: break
    return out


def seam_sheet(left,right,pairs,path,label_l,label_r):
    tw=252; th=187; cols=6
    sheet=np.full((len(pairs)*(th+30),cols*tw,3),35,np.uint8)
    for row,(cost,i,j) in enumerate(pairs):
        ids=[max(0,i-1),i,min(len(left)-1,i+1),max(0,j-1),j,min(len(right)-1,j+1)]
        src=[left,left,left,right,right,right]
        for col,(frames,idx) in enumerate(zip(src,ids)):
            im=cv2.resize(frames[idx],(tw,th),interpolation=cv2.INTER_AREA)
            y=row*(th+30); x=col*tw
            sheet[y:y+th,x:x+tw]=im
            lab=(label_l if col<3 else label_r)+f' f{idx}'
            cv2.putText(sheet,lab,(x+4,y+th+20),cv2.FONT_HERSHEY_SIMPLEX,.45,(240,240,240),1,cv2.LINE_AA)
        cv2.putText(sheet,f'{cost:.3f}',(4,row*(th+30)+18),cv2.FONT_HERSHEY_SIMPLEX,.5,(0,255,255),1,cv2.LINE_AA)
    cv2.imwrite(str(path),sheet)


def main():
    p9507,f9507,a=one('9507')
    p1520,f1520,b=one('1520')
    p3856,f3856,c=one('3856')
    contact_sheet(b,f1520,OUT/'1520_contact.png',cols=6,max_samples=48)
    # Original join is near 9507 f46 / 3856 f49. Search nearby against all bridge frames.
    ab=ranked(a,b,range(38,min(56,len(a))),range(len(b)))
    bc=ranked(b,c,range(len(b)),range(40,min(66,len(c))))
    seam_sheet(a,b,ab[:10],OUT/'seam_9507_to_1520.png','9507','1520')
    seam_sheet(b,c,bc[:10],OUT/'seam_1520_to_3856.png','1520','3856')
    info={'1520':{'fps':f1520,'frames':len(b),'duration':len(b)/f1520,
                  'size':[b[0].shape[1],b[0].shape[0]],'path':str(p1520)},
          'ab':[{'cost':x,'9507':i,'t9507':i/f9507,'1520':j,'t1520':j/f1520} for x,i,j in ab],
          'bc':[{'cost':x,'1520':i,'t1520':i/f1520,'3856':j,'t3856':j/f3856} for x,i,j in bc]}
    (OUT/'bridge_analysis.json').write_text(json.dumps(info,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(info,ensure_ascii=False,indent=2))

if __name__=='__main__': main()
