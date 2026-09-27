from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
BEFORE=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames"
AFTER=BASE/"test_hand_manual_propagation_v35_premul/alpha_frames"
OUT=BASE/"test_hand_manual_propagation_v35_premul"
CROP=(445,360,585,465); SCALE=3

def dark(im):
    return Image.alpha_composite(Image.new("RGBA",im.size,(45,45,45,255)),im.convert("RGBA")).convert("RGB")

def main():
    frames=[]
    for n in range(1,181):
        b=dark(Image.open(BEFORE/f"idle_{n:03d}.png")).crop(CROP)
        a=dark(Image.open(AFTER/f"idle_{n:03d}.png")).crop(CROP)
        size=(b.width*SCALE,b.height*SCALE);b=b.resize(size,Image.Resampling.NEAREST);a=a.resize(size,Image.Resampling.NEAREST)
        f=Image.new("RGB",(size[0]*2,size[1]+18),(28,28,28));f.paste(b,(0,18));f.paste(a,(size[0],18));d=ImageDraw.Draw(f);d.text((4,2),f"BEFORE F{n:03d}",fill="white");d.text((size[0]+4,2),"PREMUL PROPAGATED",fill="white");frames.append(f)
    frames[0].save(OUT/"hand_before_after_3s_60fps.gif",save_all=True,append_images=frames[1:],duration=17,loop=0,optimize=False,disposal=2)
    print(OUT/"hand_before_after_3s_60fps.gif")

if __name__=="__main__":main()
