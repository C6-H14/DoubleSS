from pathlib import Path
import json, shutil, subprocess, sys
import cv2
import numpy as np
from PIL import Image, ImageDraw
from prepare_inner_death_v1 import read_video

ROOT=Path(r'D:\StSmod\art_preview\front_character_death')
OUT=ROOT/'death_v1'
VIDEO=ROOT.parent/'video'

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    (ROOT/'source_v1').mkdir(exist_ok=True)
    clips=[]; report=[]
    sheet=Image.new('RGB',(1500,540)); draw=ImageDraw.Draw(sheet)
    for row,code in enumerate(('2044','8847')):
        path=next(VIDEO.glob(f'*2026-09-28-{code}-*.mp4'))
        shutil.copy2(path,ROOT/'source_v1'/path.name)
        frames,fps=read_video(path); clips.append((frames,fps))
        report.append({'source':str(path),'count':len(frames),'fps':fps,'size':list(frames[0].shape[:2][::-1])})
        for col,index in enumerate(np.linspace(0,len(frames)-1,6).astype(int)):
            im=Image.fromarray(frames[index]);im.thumbnail((250,240))
            x,y=col*250,row*270;sheet.paste(im,(x,y+22));draw.text((x+4,y+3),f'{code} frame {index+1}',fill='white')
    sheet.save(OUT/'source_contact.png')
    (OUT/'report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report),flush=True)
    if '--inspect' in sys.argv:return
    first,fps1=clips[0];second,fps2=clips[1]
    if first[0].shape!=second[0].shape:raise RuntimeError('Source sizes differ')
    # Minimal seam interpolation only; do not alter the approved fall/roll.
    a,b=first[-1],second[0]
    ga,gb=cv2.cvtColor(a,cv2.COLOR_RGB2GRAY),cv2.cvtColor(b,cv2.COLOR_RGB2GRAY)
    f=cv2.calcOpticalFlowFarneback(ga,gb,None,.5,4,25,5,7,1.5,0)
    back=cv2.calcOpticalFlowFarneback(gb,ga,None,.5,4,25,5,7,1.5,0)
    h,w=ga.shape;yy,xx=np.mgrid[:h,:w].astype(np.float32)
    bridge=[]
    for fraction in np.linspace(0,1,8)[1:-1]:
        t=float(fraction)
        wa=cv2.remap(a,xx-f[:,:,0]*t,yy-f[:,:,1]*t,cv2.INTER_LINEAR)
        wb=cv2.remap(b,xx-back[:,:,0]*(1-t),yy-back[:,:,1]*(1-t),cv2.INTER_LINEAR)
        bridge.append(cv2.addWeighted(wa,1-t,wb,t,0))
    def resample(frames,fps):
        return [frames[min(len(frames)-1,round(i*fps/60))] for i in range(round(len(frames)/fps*60))]
    final=resample(first,fps1)+bridge+resample(second,fps2)
    frames_dir=OUT/'frames_60fps';frames_dir.mkdir(exist_ok=True)
    for i,frame in enumerate(final,1):Image.fromarray(frame).save(frames_dir/f'death_{i:04d}.png')
    mp4=OUT/'front_death_v1_60fps.mp4'
    subprocess.run(['ffmpeg','-y','-v','error','-framerate','60','-i',str(frames_dir/'death_%04d.png'),'-c:v','libx264','-crf','18','-pix_fmt','yuv420p',str(mp4)],check=True)
    subprocess.run(['ffmpeg','-y','-v','error','-i',str(mp4),'-filter_complex','fps=60,scale=556:-1:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=sierra2_4a','-loop','0',str(OUT/'front_death_v1_60fps.gif')],check=True)
    boundary=round(len(first)/fps1*60)
    picks=[boundary-2,boundary,boundary+2,boundary+4,boundary+7,boundary+10,len(final)-1]
    qa=Image.new('RGB',(1750,250));draw=ImageDraw.Draw(qa)
    for k,i in enumerate(picks):
        im=Image.fromarray(final[i]);im.thumbnail((250,220));qa.paste(im,(k*250,22));draw.text((k*250+4,3),f'frame {i+1}',fill='white')
    qa.save(OUT/'seam_tail_check.png')
    report.append({'frames':len(final),'fps':60,'duration':len(final)/60,'bridge_frames':6,'source_timing_preserved':True})
    (OUT/'report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(report[-1],flush=True)

if __name__=='__main__':main()
