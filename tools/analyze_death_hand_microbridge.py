from pathlib import Path
import json
import cv2
import numpy as np

from analyze_death_video_splice import read_video, contact_sheet, descriptor, pair_cost

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT=ROOT/'death_splice_analysis'


def load(token):
    p=list(ROOT.glob(f'*{token}*.mp4'))
    fps,frames=read_video(p[0])
    return p[0],fps,frames


def main():
    _,fa,a=load('1520'); p,fb,b=load('3034')
    contact_sheet(b,fb,OUT/'3034_contact.png',cols=6,max_samples=48)
    da=[descriptor(x) for x in a]
    db=[descriptor(x) for x in b]
    starts=sorted((pair_cost(da[i],db[j]),i,j) for i in range(3,7) for j in range(len(b)))[:12]
    ends=sorted((pair_cost(db[i],da[j]),i,j) for i in range(len(b)) for j in range(7,11))[:12]
    info={'path':str(p),'fps':fb,'frames':len(b),'duration':len(b)/fb,
          'size':[b[0].shape[1],b[0].shape[0]],
          'starts':[{'cost':c,'1520':i,'3034':j} for c,i,j in starts],
          'ends':[{'cost':c,'3034':i,'1520':j} for c,i,j in ends]}
    (OUT/'3034_analysis.json').write_text(json.dumps(info,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(info,ensure_ascii=False,indent=2))

if __name__=='__main__': main()
