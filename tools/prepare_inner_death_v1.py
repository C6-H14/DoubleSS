from pathlib import Path
import cv2
import numpy as np
from PIL import Image, ImageDraw
import json
import subprocess

ROOT = Path(r'D:\StSmod\art_preview\alternate_fencer_death')
OUT = ROOT / 'death_v1'
VIDEO = ROOT.parent / 'video'

def read_video(path):
    cap = cv2.VideoCapture(str(path))
    fps = cap.get(cv2.CAP_PROP_FPS)
    frames = []
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        frames.append(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
    cap.release()
    return frames, fps

def main():
    OUT.mkdir(exist_ok=True)
    clips = []
    report = []
    sheet = Image.new('RGB', (1200, 600))
    draw = ImageDraw.Draw(sheet)
    for row, code in enumerate(('4912', '9848')):
        path = next(VIDEO.glob(f'*{code}*.mp4'))
        frames, fps = read_video(path)
        clips.append((frames, fps))
        report.append(dict(source=str(path), count=len(frames), fps=fps,
                           size=list(frames[0].shape[:2][::-1])))
        indices = np.linspace(0, len(frames)-1, 6).astype(int)
        for col, index in enumerate(indices):
            im = Image.fromarray(frames[index]); im.thumbnail((200, 265))
            sheet.paste(im, (col*200, row*300+25))
            draw.text((col*200+4,row*300+5),f'{code} frame {index+1}',fill='white')
    sheet.save(OUT/'source_contact.png')
    (OUT/'source_report.json').write_text(json.dumps(report, indent=2),encoding='utf-8')
    print(json.dumps(report))
    first, fps1 = clips[0]
    second, fps2 = clips[1]
    def resample(frames, fps):
        count = round(len(frames)/fps*60)
        return [frames[min(len(frames)-1,round(i*fps/60))] for i in range(count)]
    a, b = first[-1], second[0]
    gray_a = cv2.cvtColor(a, cv2.COLOR_RGB2GRAY)
    gray_b = cv2.cvtColor(b, cv2.COLOR_RGB2GRAY)
    forward = cv2.calcOpticalFlowFarneback(gray_a, gray_b, None, .5, 4, 25, 5, 7, 1.5, 0)
    backward = cv2.calcOpticalFlowFarneback(gray_b, gray_a, None, .5, 4, 25, 5, 7, 1.5, 0)
    h,w = gray_a.shape
    yy,xx = np.mgrid[:h,:w].astype(np.float32)
    bridge = []
    for t in np.linspace(0,1,8)[1:-1]:
        t = float(t)
        wa = cv2.remap(a, xx-forward[:,:,0]*t, yy-forward[:,:,1]*t, cv2.INTER_LINEAR)
        wb = cv2.remap(b, xx-backward[:,:,0]*(1-t), yy-backward[:,:,1]*(1-t), cv2.INTER_LINEAR)
        bridge.append(cv2.addWeighted(wa,1-t,wb,t,0))
    final = resample(first,fps1)+bridge+resample(second,fps2)
    # Only extinguish the remaining detached embers after the body is gone.
    blank = np.asarray(Image.open(ROOT/'reference_frames/death_end_background.png').convert('RGB').resize((w,h)))
    last = final[-1]
    final += [cv2.addWeighted(last,1-t,blank,t,0) for t in np.linspace(0,1,22)[1:]]
    final += [blank]*12
    frames_dir = OUT/'frames_60fps'
    frames_dir.mkdir(exist_ok=True)
    for index, frame in enumerate(final,1):
        Image.fromarray(frame).save(frames_dir/f'death_{index:04d}.png')
    subprocess.run(['ffmpeg','-y','-v','error','-framerate','60','-i',str(frames_dir/'death_%04d.png'),'-c:v','libx264','-crf','18','-pix_fmt','yuv420p',str(OUT/'death_v1_60fps.mp4')],check=True)
    subprocess.run(['ffmpeg','-y','-v','error','-i',str(OUT/'death_v1_60fps.mp4'),'-filter_complex','fps=60,scale=556:-1:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=sierra2_4a','-loop','0',str(OUT/'death_v1_60fps.gif')],check=True)
    report.append(dict(output_frames=len(final),output_fps=60,duration=len(final)/60,bridge_frames=6,tail_cleanup_frames=33))
    (OUT/'source_report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('Export complete:',len(final),'frames')

if __name__ == '__main__':
    main()
