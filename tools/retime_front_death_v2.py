"""Remove first-part slow motion by decimation; leave approved fall unchanged."""
from pathlib import Path
import json, shutil, subprocess
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r'D:\StSmod\art_preview\front_character_death')
SOURCE=ROOT/'death_v1/frames_60fps'
OUT=ROOT/'death_v2_part1_0p8s'

def main():
    paths=sorted(SOURCE.glob('death_*.png'))
    assert len(paths)==488
    first_count=241
    selected=np.linspace(0,first_count-1,48).round().astype(int).tolist()
    # Keep the old six-frame join and all second-part frames exactly as-is.
    selected+=list(range(first_count,len(paths)))
    frames=OUT/'frames_60fps'
    if frames.exists():raise RuntimeError('Existing result must be preserved; choose a new version')
    frames.mkdir(parents=True)
    for i,index in enumerate(selected,1):
        shutil.copy2(paths[index],frames/f'death_{i:04d}.png')
    mp4=OUT/'front_death_v2_60fps.mp4'
    subprocess.run(['ffmpeg','-y','-v','error','-framerate','60','-i',str(frames/'death_%04d.png'),'-c:v','libx264','-crf','18','-pix_fmt','yuv420p',str(mp4)],check=True)
    subprocess.run(['ffmpeg','-y','-v','error','-i',str(mp4),'-filter_complex','fps=60,scale=556:-1:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=sierra2_4a','-loop','0',str(OUT/'front_death_v2_60fps.gif')],check=True)
    sheet=Image.new('RGB',(1500,520));draw=ImageDraw.Draw(sheet)
    picks=[0,9,19,29,39,47,48,50,53,55,140,len(selected)-1]
    for k,index in enumerate(picks):
        im=Image.open(frames/f'death_{index+1:04d}.png');im.thumbnail((250,230))
        x,y=k%6*250,k//6*260;sheet.paste(im,(x,y+22));draw.text((x+4,y+3),f'frame {index+1}',fill='white')
    sheet.save(OUT/'timing_seam_review.png')
    report={'frames':len(selected),'fps':60,'duration':len(selected)/60,'part1_frames':48,'part1_duration':.8,'join_frames':6,'part2_unchanged':True,'v1_source_frame_numbers':[i+1 for i in selected]}
    (OUT/'report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    assert len(set(selected[:48]))==48
    assert selected[0]==0 and selected[47]==240 and selected[-1]==487
    print(json.dumps({k:v for k,v in report.items() if k!='v1_source_frame_numbers'}))

if __name__=='__main__':main()
