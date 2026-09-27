from pathlib import Path
import json
import cv2

from analyze_death_video_splice import read_video,contact_sheet,descriptor,pair_cost

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT=ROOT/'death_splice_analysis'
FRAMES=OUT/'death_splice_frames_review_v5_hand_transition'

video=list(ROOT.glob('*6809*.mp4'))[0]
fps,frames=read_video(video)
start=cv2.imread(str(FRAMES/'frame_0025.png'))
end=cv2.imread(str(FRAMES/'frame_0060.png'))
contact_sheet(frames,fps,OUT/'6809_contact.png',cols=6,max_samples=48)
ds,de=descriptor(start),descriptor(end)
start_scores=sorted((pair_cost(ds,descriptor(f)),i) for i,f in enumerate(frames))
end_scores=sorted((pair_cost(de,descriptor(f)),i) for i,f in enumerate(frames))
info={'fps':fps,'frames':len(frames),'duration':len(frames)/fps,
      'size':[frames[0].shape[1],frames[0].shape[0]],
      'start_matches':start_scores[:15],'end_matches':end_scores[:15]}
(OUT/'6809_analysis.json').write_text(json.dumps(info,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(info,ensure_ascii=False,indent=2))
