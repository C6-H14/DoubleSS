from pathlib import Path
from PIL import Image,ImageDraw

root=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
src=sorted((root/'death_splice_frames_review_v10_support_20f').glob('frame_*.png'))
alp=sorted((root/'death_splice_v11_alpha_refined_v5_drawn_emblem'/'alpha_frames').glob('*.png'))
ids=list(range(18,36)); crop=(480,210,730,570); scale=1
cw,ch=(crop[2]-crop[0])*scale,(crop[3]-crop[1])*scale
sheet=Image.new('RGB',(6*cw,6*(ch+28)),(25,25,25));d=ImageDraw.Draw(sheet)
for n,i in enumerate(ids):
    for row,paths in enumerate((src,alp)):
        im=Image.open(paths[i]).convert('RGBA').crop(crop)
        if row:
            bg=Image.new('RGBA',im.size,(45,45,45,255));im=Image.alpha_composite(bg,im)
        im=im.resize((cw,ch),Image.Resampling.NEAREST).convert('RGB')
        k=n+row*len(ids);x=(k%6)*cw;y=(k//6)*(ch+28)
        sheet.paste(im,(x,y));d.text((x+4,y+ch+4),f'{"src" if row==0 else "alpha"} f{i}',fill='white')
sheet.save(root/'death_frames_18_35_chest_review_v5.jpg',quality=96)
