from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw

OUT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis\death_splice_v11_alpha_refined_v5_drawn_emblem')
paths=sorted((OUT/'alpha_frames').glob('*.png'))
frames=[Image.open(p).convert('RGBA') for p in paths]

def checker(size,cell=24):
    a=Image.new('RGBA',size,(225,225,225,255));d=ImageDraw.Draw(a)
    for y in range(0,size[1],cell):
        for x in range(0,size[0],cell):
            if (x//cell+y//cell)%2:d.rectangle((x,y,x+cell-1,y+cell-1),fill=(165,165,165,255))
    return a

picks=np.linspace(0,len(frames)-1,30).round().astype(int)
cw,ch,cols=320,268,6;rows=(len(picks)+cols-1)//cols
sheet=Image.new('RGB',(cols*cw,rows*ch),(28,28,28));draw=ImageDraw.Draw(sheet);cb=checker(frames[0].size)
for n,i in enumerate(picks):
    view=Image.alpha_composite(cb.copy(),frames[i]).resize((320,240),Image.Resampling.LANCZOS).convert('RGB')
    x=(n%cols)*cw;y=(n//cols)*ch;sheet.paste(view,(x,y));draw.text((x+5,y+244),f'frame {i}',fill='white')
sheet.save(OUT/'death_checker_contact.jpg',quality=97)
black=Image.new('RGBA',frames[0].size,(0,0,0,255))
views=[Image.alpha_composite(black.copy(),f).resize((560,416),Image.Resampling.LANCZOS).convert('RGB') for f in frames]
views[0].save(OUT/'death_black_preview.gif',save_all=True,append_images=views[1:],duration=42,loop=0,disposal=2,optimize=False)
(OUT/'report.json').write_text(json.dumps({'frames':len(frames),'fps':24,'duration':len(frames)/24,'method':'border-connected conservative chroma matte'},indent=2)+'\n',encoding='utf-8')
print(OUT/'death_checker_contact.jpg')
