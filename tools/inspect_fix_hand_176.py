from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle";SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames";V39=BASE/"test_hand_manual_propagation_v39_dual_anchor_cyclic/alpha_frames";V35=BASE/"test_hand_manual_propagation_v35_premul/alpha_frames";OUT=BASE/"test_hand_manual_propagation_v39_dual_anchor_cyclic"
CROP=(450,370,575,455);S=7
def dark(p):
 im=Image.open(p).convert("RGBA");return Image.alpha_composite(Image.new("RGBA",im.size,(45,45,45,255)),im).convert("RGB")
def main():
 cells=[]
 for n in (175,176,177):
  for label,root in (("SOURCE",SRC),("V39",V39),("V35",V35)):
   im=dark(root/f"idle_{n:03d}.png").crop(CROP).resize(((CROP[2]-CROP[0])*S,(CROP[3]-CROP[1])*S),Image.Resampling.NEAREST);cells.append((f"F{n} {label}",im))
 w=cells[0][1].width;h=cells[0][1].height+20;sheet=Image.new("RGB",(w*3,h*3),(28,28,28));d=ImageDraw.Draw(sheet)
 for i,(label,im) in enumerate(cells):x=(i%3)*w;y=(i//3)*h;sheet.paste(im,(x,y+20));d.text((x+5,y+3),label,fill="white")
 sheet.save(OUT/"frames175_176_177_hand_inspection_7x.png",optimize=True);print(OUT/"frames175_176_177_hand_inspection_7x.png")
if __name__=="__main__":main()
