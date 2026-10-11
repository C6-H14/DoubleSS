"""Shorten summoning, halve charge/recovery, preserve strike frames."""
from pathlib import Path
import shutil,json,subprocess
import cv2
from PIL import Image

ROOT=Path(r'D:\StSmod\art_preview\alternate_fencer_ultimate')
SOURCE=ROOT/'ultimate_v2_120f_60fps'/'frames_60fps'
OUT=ROOT/'ultimate_v3_81f_60fps'
FRAMES=OUT/'frames_60fps'
FRAMES.mkdir(parents=True,exist_ok=True)
def sample(a,b,n):
    return [round(a+(b-a)*i/(n-1)) for i in range(n)]
selected=sample(1,42,30)+sample(43,68,13)+list(range(69,92))+sample(92,120,15)
for i,n in enumerate(selected,1):
    shutil.copy2(SOURCE/f'ultimate_{n:04d}.png',FRAMES/f'ultimate_{i:04d}.png')
exe=r'C:\Users\C6H14\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe'
def run(args):
    subprocess.run([exe,'-hide_banner','-loglevel','error','-y',*args],check=True)
run(['-framerate','60','-start_number','1','-i',str(FRAMES/'ultimate_%04d.png'),'-frames:v',str(len(selected)),'-an','-c:v','libx264','-crf','17','-preset','fast','-pix_fmt','yuv420p','-movflags','+faststart',str(OUT/'ultimate_v3_1p35s.mp4')])
run(['-i',str(OUT/'ultimate_v3_1p35s.mp4'),'-filter_complex','[0:v]split[a][b];[a]palettegen=stats_mode=full[p];[b][p]paletteuse=dither=none','-loop','0',str(OUT/'ultimate_v3_1p35s.gif')])
metadata={'fps':60,'frames':len(selected),'duration_seconds':len(selected)/60,'source':'ultimate_v2_120f_60fps','source_frame_numbers':selected,'phases':[{'name':'summon','old':[1,42],'new':[1,30],'seconds':0.5},{'name':'charge','old':[43,68],'new':[31,43],'seconds':13/60},{'name':'strike','old':[69,91],'new':[44,66],'seconds':23/60},{'name':'recovery','old':[92,120],'new':[67,81],'seconds':0.25}],'jar_exported':False}
(OUT/'frame_map.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
cap=cv2.VideoCapture(str(OUT/'ultimate_v3_1p35s.mp4'))
count,fps=cap.get(cv2.CAP_PROP_FRAME_COUNT),cap.get(cv2.CAP_PROP_FPS)
cap.release()
gif=Image.open(OUT/'ultimate_v3_1p35s.gif')
duration=0
for i in range(gif.n_frames):
    gif.seek(i)
    duration+=gif.info.get('duration',0)
assert count==81 and fps==60 and duration==1350
assert (FRAMES/'ultimate_0081.png').read_bytes()==(SOURCE/'ultimate_0120.png').read_bytes()
print({'frames':count,'fps':fps,'seconds':count/fps,'gif_ms':duration,'strike_preserved':True,'last_frame_preserved':True})
print(OUT)
