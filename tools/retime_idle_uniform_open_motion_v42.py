from pathlib import Path
import json
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v41_gap_repaired_pixel_evidence/alpha_frames";OUT=BASE/"short_blink_final_v42_uniform_open_motion";FRAMES=OUT/"alpha_frames"

def premul(a):
 x=a.astype(np.float32);al=x[:,:,3:4]/255.;return np.concatenate([x[:,:,:3]*al,x[:,:,3:4]],2)
def unpremul(p):
 a=np.clip(p[:,:,3:4],0,255);rgb=np.zeros_like(p[:,:,:3]);np.divide(p[:,:,:3]*255.,a,out=rgb,where=a>1e-5);return np.rint(np.clip(np.concatenate([rgb,a],2),0,255)).astype(np.uint8)
def dark(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))

def main():
 FRAMES.mkdir(parents=True,exist_ok=True)
 old_pos=np.concatenate([np.linspace(0.,58.,91),np.concatenate([np.linspace(58.5,66.,8),np.linspace(67.,90.,13),np.linspace(91.,99.,8)]),np.linspace(99.4,120.,60)])
 new_pos=np.concatenate([np.linspace(0.,58.,111),np.concatenate([np.linspace(58.5,66.,8),np.linspace(67.,90.,13),np.linspace(91.,99.,8)]),np.linspace(99.4,120.,40)])
 if len(old_pos)!=180 or len(new_pos)!=180:raise RuntimeError((len(old_pos),len(new_pos)))
 source=[np.asarray(Image.open(SRC/f"idle_{n:03d}.png").convert("RGBA"),np.uint8) for n in range(1,181)];pm=[premul(a) for a in source]
 mapping=[];output=[]
 for i,p in enumerate(new_pos,1):
  idx=float(np.interp(p,old_pos,np.arange(180,dtype=float)));lo=int(np.floor(idx));hi=min(lo+1,179);t=idx-lo
  out=unpremul(pm[lo]*(1-t)+pm[hi]*t);Image.fromarray(out,"RGBA").save(FRAMES/f"idle_{i:03d}.png",optimize=True);output.append(out);mapping.append({"frame":i,"source_position":round(float(p),4),"old_frame_position":round(idx+1,4)})
 # Full-body preview and a side-by-side comparison.
 gif=[];compare=[]
 for i,(before,after) in enumerate(zip(source,output),1):
  a=dark(before);b=dark(after);a.thumbnail((445,334),Image.Resampling.LANCZOS);b.thumbnail((445,334),Image.Resampling.LANCZOS)
  f=Image.new("RGB",(a.width*2,a.height+18),(28,28,28));f.paste(a.convert("RGB"),(0,18));f.paste(b.convert("RGB"),(a.width,18));d=ImageDraw.Draw(f);d.text((4,2),f"V41 F{i:03d}",fill="white");d.text((a.width+4,2),"V42 UNIFORM OPEN MOTION",fill="white");compare.append(f)
  gif.append(b.convert("RGB"))
 compare[0].save(OUT/"v41_v42_timing_compare_3s_60fps.gif",save_all=True,append_images=compare[1:],duration=17,loop=0,optimize=False,disposal=2)
 gif[0].save(OUT/"idle_v42_3s_60fps.gif",save_all=True,append_images=gif[1:],duration=17,loop=0,optimize=False,disposal=2)
 meta={"frames":180,"fps":60,"duration":3.0,"blink_frames":[112,140],"open_before":{"frames":111,"source_range":[0,58],"source_step":58/110},"blink":{"frames":29,"source_range":[58.5,99]},"open_after":{"frames":40,"source_range":[99.4,120],"source_step":20.6/39},"old_open_speed_ratio_after_vs_before":(20.6/59)/(58/90),"new_open_speed_ratio_after_vs_before":(20.6/39)/(58/110),"mapping":mapping}
 (OUT/"v42_timing.json").write_text(json.dumps(meta,indent=2)+"\n",encoding="utf-8");print(json.dumps({k:v for k,v in meta.items() if k!='mapping'},indent=2))
if __name__=="__main__":main()
