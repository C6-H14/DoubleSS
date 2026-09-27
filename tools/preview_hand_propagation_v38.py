from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle"
BEFORE=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames";AFTER=BASE/"test_hand_manual_propagation_v39_dual_anchor_cyclic/alpha_frames";OUT=AFTER.parent
CROP=(445,360,585,465);S=3
def dark(p):
    im=Image.open(p).convert("RGBA");return Image.alpha_composite(Image.new("RGBA",im.size,(45,45,45,255)),im).convert("RGB")
def main():
    fs=[]
    for n in range(1,181):
        b=dark(BEFORE/f"idle_{n:03d}.png").crop(CROP).resize(((CROP[2]-CROP[0])*S,(CROP[3]-CROP[1])*S),Image.Resampling.NEAREST);a=dark(AFTER/f"idle_{n:03d}.png").crop(CROP).resize(b.size,Image.Resampling.NEAREST)
        f=Image.new("RGB",(b.width*2,b.height+18),(28,28,28));f.paste(b,(0,18));f.paste(a,(b.width,18));d=ImageDraw.Draw(f);d.text((4,2),f"BEFORE F{n:03d}",fill="white");d.text((b.width+4,2),"DUAL ANCHOR",fill="white");fs.append(f)
    fs[0].save(OUT/"hand_before_after_3s_60fps.gif",save_all=True,append_images=fs[1:],duration=17,loop=0,optimize=False,disposal=2)
    print(OUT/"hand_before_after_3s_60fps.gif")
if __name__=="__main__":main()
