"""Video compositing test using AI-redrawn elbow; original footage stays intact."""
from pathlib import Path
import subprocess, shutil

ROOT = Path(r'D:\StSmod\art_preview\alternate_fencer_ultimate')
OUT = ROOT/'ultimate_local_repair_v1'
OUT.mkdir(exist_ok=True)
FRAMES = OUT/'frames_60fps'
FRAMES.mkdir(exist_ok=True)
VIDEO = Path(r'D:\StSmod\art_preview\video')
SOURCE1 = next(VIDEO.glob('*9341*.mp4'))
SOURCE2 = next(VIDEO.glob('*5636*.mp4'))
KEY = Path(r'C:\Users\C6H14\.codex\generated_images\01a0c353-8aba-7b72-816f-4651704d5905\exec-2cf96442-bb4e-4933-8cad-b916dbda0dce.png')
shutil.copy2(KEY, OUT/'redrawn_end_key.png')
shutil.copy2(Path(r'C:\Users\C6H14\.codex\generated_images\01a0c353-8aba-7b72-816f-4651704d5905\exec-82352033-8bbd-4e8f-a860-c6a7bbcefee1.png'),OUT/'redrawn_mid_key.png')

def run(args):
    executable = r'C:\Users\C6H14\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe'
    subprocess.run([executable,'-hide_banner','-loglevel','error','-y',*args],check=True)

# The image model redraws the elbow; FFmpeg only composites this tiny region.
run(['-i',str(KEY),'-vf','scale=1112:834,crop=44:54:260:273,colorkey=0x173E2B:0.12:0.05,format=rgba','-frames:v','1',str(OUT/'elbow_patch.png')])
graph = (
    '[0:v]trim=end_frame=62,setpts=PTS-STARTPTS,fps=60,setsar=1[a];'
    '[1:v]setpts=PTS-STARTPTS,fps=60,setsar=1[b];'
    '[2:v]format=rgba,fade=t=in:st=2.72:d=0.72:alpha=1[p];'
    "[b][p]overlay=x='270-10*min(max((t-2.72)/0.72,0),1)':y=273:shortest=1[c];"
    # Local edge attenuation, not a global fade or a change to the character.
    '[3:v]format=rgba,geq=r=23:g=62:b=43:a=\'255*clip(X/72,0,1)\'[edge];'
    "[c][edge]overlay=x=1040:y=0:enable='between(t,1.45,2.40)':shortest=1[d];"
    '[a][d]concat=n=2:v=1:a=0,format=yuv420p[v]'
)
run(['-i',str(SOURCE1),'-i',str(SOURCE2),'-loop','1','-framerate','60','-i',str(OUT/'elbow_patch.png'),'-f','lavfi','-i','color=c=0x173E2B:s=72x450:r=60:d=4.02','-filter_complex',graph,'-map','[v]','-an','-c:v','libx264','-crf','17','-preset','fast','-movflags','+faststart',str(OUT/'ultimate_test.mp4')])
run(['-i',str(OUT/'ultimate_test.mp4'),'-vf','fps=60','-start_number','1',str(FRAMES/'ultimate_%04d.png')])
run(['-i',str(OUT/'ultimate_test.mp4'),'-filter_complex','[0:v]split[a][b];[a]palettegen=stats_mode=full[p];[b][p]paletteuse=dither=none','-loop','0',str(OUT/'ultimate_test.gif')])
print(OUT)
