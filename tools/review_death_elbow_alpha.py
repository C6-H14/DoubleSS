from pathlib import Path
from PIL import Image,ImageDraw

out=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis\death_splice_v11_alpha_refined_v2')
paths=sorted((out/'alpha_frames').glob('*.png'))
ids=[30,34,38,42,46,50,54,58,62,66,70,74,78,82]
crop=(430,350,760,710); cw,ch=660,748; cols=4; rows=(len(ids)+cols-1)//cols
sheet=Image.new('RGB',(cols*cw,rows*ch),(28,28,28)); d=ImageDraw.Draw(sheet)
for n,i in enumerate(ids):
    im=Image.open(paths[i]).convert('RGBA').crop(crop)
    bg=Image.new('RGBA',im.size,(210,210,210,255)); bd=ImageDraw.Draw(bg)
    for y in range(0,im.height,16):
        for x in range(0,im.width,16):
            if (x//16+y//16)%2:bd.rectangle((x,y,x+15,y+15),fill=(140,140,140,255))
    view=Image.alpha_composite(bg,im).resize((660,720),Image.Resampling.NEAREST).convert('RGB')
    x=(n%cols)*cw;y=(n//cols)*ch;sheet.paste(view,(x,y));d.text((x+5,y+724),f'frame {i}',fill='white')
sheet.save(out/'death_elbow_detail_contact.png')
