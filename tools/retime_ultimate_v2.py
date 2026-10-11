"""Retain repaired pixels; sample first phase into 42 frames, rest into 78."""
from pathlib import Path
import shutil, json, subprocess
import cv2
from PIL import Image

ROOT = Path(r'D:\StSmod\art_preview\alternate_fencer_ultimate')
SOURCE = ROOT/'ultimate_local_repair_v1'/'frames_60fps'
OUT = ROOT/'ultimate_v2_120f_60fps'
FRAMES = OUT/'frames_60fps'
FRAMES.mkdir(parents=True, exist_ok=True)

def sample(start, end, count):
    return [round(start + (end-start)*i/(count-1)) for i in range(count)]

# v1's 154-frame opening ends after the three spirit swords are fully formed.
selected = sample(1,154,42) + sample(155,396,78)
for i,n in enumerate(selected,1):
    shutil.copy2(SOURCE/f'ultimate_{n:04d}.png',FRAMES/f'ultimate_{i:04d}.png')
exe = r'C:\Users\C6H14\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe'
def run(args):
    subprocess.run([exe,'-hide_banner','-loglevel','error','-y',*args],check=True)
run(['-framerate','60','-start_number','1','-i',str(FRAMES/'ultimate_%04d.png'),'-frames:v','120','-an','-c:v','libx264','-crf','17','-preset','fast','-pix_fmt','yuv420p','-movflags','+faststart',str(OUT/'ultimate_v2_2s.mp4')])
run(['-i',str(OUT/'ultimate_v2_2s.mp4'),'-filter_complex','[0:v]split[a][b];[a]palettegen=stats_mode=full[p];[b][p]paletteuse=dither=none','-loop','0',str(OUT/'ultimate_v2_2s.gif')])
metadata = {'fps':60,'frames':120,'duration_seconds':2,'summon_frames':[1,42],'summon_duration_seconds':0.7,'remaining_frames':[43,120],'remaining_duration_seconds':1.3,'source_frame_numbers':selected,'method':'frame selection only; no redraw, blending, or optical-flow interpolation','jar_exported':False}
(OUT/'frame_map.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
cap = cv2.VideoCapture(str(OUT/'ultimate_v2_2s.mp4'))
count,fps = cap.get(cv2.CAP_PROP_FRAME_COUNT),cap.get(cv2.CAP_PROP_FPS)
cap.release()
gif = Image.open(OUT/'ultimate_v2_2s.gif')
duration = 0
for i in range(gif.n_frames):
    gif.seek(i)
    duration += gif.info.get('duration',0)
print({'mp4_frames':count,'mp4_fps':fps,'seconds':count/fps,'gif_duration_ms':duration,'png_count':len(list(FRAMES.glob('*.png')))})
assert count == 120 and fps == 60 and duration == 2000
print(OUT)
