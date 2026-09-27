from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v42_uniform_open_motion/alpha_frames";OUT=BASE/"short_blink_final_v43_saturation_stable";FRAMES=OUT/"alpha_frames"

def circular_gaussian(values,sigma=2.2):
    r=int(np.ceil(sigma*3));x=np.arange(-r,r+1);k=np.exp(-(x*x)/(2*sigma*sigma));k/=k.sum();pad=np.r_[values[-r:],values,values[:r]];return np.convolve(pad,k,mode="same")[r:r+len(values)]
def dark(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))

def main():
    FRAMES.mkdir(parents=True,exist_ok=True);src=[];stats=[]
    for n in range(1,181):
        a=np.asarray(Image.open(SRC/f"idle_{n:03d}.png").convert("RGBA"),np.uint8);src.append(a);h=cv2.cvtColor(a[:,:,:3],cv2.COLOR_RGB2HSV);m=(a[:,:,3]>=220)&(h[:,:,1]>=20)&(h[:,:,1]<=210)&(h[:,:,2]>=35)&(h[:,:,2]<=245);stats.append(float(h[:,:,1][m].mean()))
    stats=np.asarray(stats);target=circular_gaussian(stats,2.2);delta=np.clip(target-stats,-2.5,2.5);rows=[];output=[]
    for n,(a,ds) in enumerate(zip(src,delta),1):
        hsv=cv2.cvtColor(a[:,:,:3],cv2.COLOR_RGB2HSV).astype(np.float32);alpha=a[:,:,3].astype(np.float32)/255.;sat_weight=np.clip((hsv[:,:,1]-8)/42,0,1)*alpha
        hsv[:,:,1]=np.clip(hsv[:,:,1]+ds*sat_weight,0,255);rgb=cv2.cvtColor(np.rint(hsv).astype(np.uint8),cv2.COLOR_HSV2RGB);out=a.copy();visible=a[:,:,3]>0;out[visible,:3]=rgb[visible];Image.fromarray(out,"RGBA").save(FRAMES/f"idle_{n:03d}.png",optimize=True);output.append(out);rows.append({"frame":n,"sat_before":round(float(stats[n-1]),4),"sat_target":round(float(target[n-1]),4),"delta":round(float(ds),4)})
    def series(frames):
        vals=[]
        for a in frames:
            h=cv2.cvtColor(a[:,:,:3],cv2.COLOR_RGB2HSV);m=(a[:,:,3]>=220)&(h[:,:,1]>=20)&(h[:,:,1]<=210)&(h[:,:,2]>=35)&(h[:,:,2]<=245);vals.append(float(h[:,:,1][m].mean()))
        return np.asarray(vals)
    after=series(output);jb=np.r_[np.diff(stats),stats[0]-stats[-1]];ja=np.r_[np.diff(after),after[0]-after[-1]]
    summary={"frames":180,"max_abs_jump_before":round(float(np.abs(jb).max()),4),"max_abs_jump_after":round(float(np.abs(ja).max()),4),"loop_jump_before_180_to_1":round(float(jb[-1]),4),"loop_jump_after_180_to_1":round(float(ja[-1]),4),"largest_corrections":sorted(rows,key=lambda r:abs(r['delta']),reverse=True)[:15]}
    (OUT/"v43_saturation_report.json").write_text(json.dumps({"summary":summary,"frames_detail":rows},indent=2)+"\n",encoding="utf-8")
    # Side-by-side 60 fps preview.
    gif=[]
    for n,(a,b) in enumerate(zip(src,output),1):
        ia=dark(a);ib=dark(b);ia.thumbnail((445,334),Image.Resampling.LANCZOS);ib.thumbnail((445,334),Image.Resampling.LANCZOS);f=Image.new("RGB",(ia.width*2,ia.height+18),(28,28,28));f.paste(ia.convert("RGB"),(0,18));f.paste(ib.convert("RGB"),(ia.width,18));d=ImageDraw.Draw(f);d.text((4,2),f"V42 F{n:03d}",fill="white");d.text((ia.width+4,2),"V43 SATURATION STABLE",fill="white");gif.append(f)
    gif[0].save(OUT/"v42_v43_saturation_compare_3s_60fps.gif",save_all=True,append_images=gif[1:],duration=17,loop=0,optimize=False,disposal=2)
    print(json.dumps(summary,indent=2))
if __name__=="__main__":main()
