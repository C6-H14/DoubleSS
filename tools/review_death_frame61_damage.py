from pathlib import Path
from PIL import Image,ImageDraw
root=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
dirs=[root/'death_splice_frames_review_v13_hold7_retract50pct',root/'death_splice_frames_review_v14_last_hold_transition']
ids=[60,61,62,63]
crop=(350,250,850,750);cw,ch=750,750
sheet=Image.new('RGB',(4*cw,2*(ch+30)),(25,25,25));d=ImageDraw.Draw(sheet)
for r,dr in enumerate(dirs):
 for c,i in enumerate(ids):
  im=Image.open(dr/f'frame_{i:04d}.png').convert('RGB').crop(crop).resize((cw,ch),Image.Resampling.NEAREST)
  x=c*cw;y=r*(ch+30);sheet.paste(im,(x,y));d.text((x+5,y+ch+5),f'{"before" if r==0 else "after"} f{i}',fill='white')
sheet.save(root/'death_frame61_damage_review.jpg',quality=97)
