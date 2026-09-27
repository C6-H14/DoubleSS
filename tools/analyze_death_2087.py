from pathlib import Path
import json,cv2,numpy as np
from analyze_death_video_splice import read_video,contact_sheet

root=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source')
out=root/'death_splice_analysis';video=list(root.glob('*2087*.mp4'))[0]
fps,frames=read_video(video);h,w=frames[0].shape[:2]
contact_sheet(frames,fps,out/'2087_contact.png',cols=6,max_samples=48)
# Dense chest review for S-S consistency.
ids=np.linspace(0,len(frames)-1,min(48,len(frames))).round().astype(int)
tw,th,cols=360,260,6;rows=(len(ids)+cols-1)//cols
sheet=np.full((rows*(th+30),cols*tw,3),32,np.uint8)
for n,i in enumerate(ids):
    im=frames[i];crop=im[100:650,350:850];crop=cv2.resize(crop,(tw,th),interpolation=cv2.INTER_AREA)
    y=(n//cols)*(th+30);x=(n%cols)*tw;sheet[y:y+th,x:x+tw]=crop
    cv2.putText(sheet,f'f{i} {i/fps:.2f}s',(x+4,y+th+20),cv2.FONT_HERSHEY_SIMPLEX,.48,(245,245,245),1,cv2.LINE_AA)
cv2.imwrite(str(out/'2087_chest_dense.png'),sheet)
meta={'path':str(video),'fps':fps,'frames':len(frames),'duration':len(frames)/fps,'width':w,'height':h}
(out/'2087_analysis.json').write_text(json.dumps(meta,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps(meta,ensure_ascii=False))
