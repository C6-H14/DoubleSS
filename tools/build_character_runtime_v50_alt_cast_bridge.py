"""Bridge the first/last cast frames to the actual idle artwork."""
import shutil,json
import numpy as np
from PIL import Image
import build_character_runtime_v48_alt_cast as previous
import build_character_runtime_v46_alt_cast_s_bridge as flow
import bridge_alt_buff_v3 as media

r=previous.runtime
r.OUT=previous.ROOT/'art_candidates/character_runtime_v50_alt_cast_bridge'
r.IMAGES=r.OUT/'images'
r.EXPORT=r.OUT/'export'

if __name__=='__main__':
    source=previous.ROOT/'art_candidates/character_runtime_v49_alt_cast_aligned/images'
    frames=[np.array(Image.open(p).convert('RGBA'),dtype=np.float32) for p in sorted(source.glob('cast_alpha_*.png'))]
    idle=np.array(Image.open(flow.IDLE).convert('RGBA'),dtype=np.float32)
    # Four-frame in/out joins: endpoints are idle, interior pose is untouched.
    first,last=frames[3].copy(),frames[-4].copy()
    inf, inb=flow.flow_between(idle,first),flow.flow_between(first,idle)
    outf,outb=flow.flow_between(last,idle),flow.flow_between(idle,last)
    for j in range(4):
        t=j/3.
        frames[j]=idle if j==0 else (first if j==3 else flow.morph(idle,first,inf,inb,t))
        frames[-4+j]=last if j==0 else (idle if j==3 else flow.morph(last,idle,outf,outb,t))
    staged=previous.ROOT/'art_candidates/alt_cast_runtime_bridge_v50'
    staged.mkdir(parents=True,exist_ok=True)
    for i,pixels in enumerate(frames,1):
        path=staged/f'cast_{i:03d}.png'
        media.to_straight(pixels.astype(np.float32)).save(path)
        r.FRAME_OVERRIDES[i]=path
    r.main()
    for path in r.EXPORT.iterdir():
        if path.is_file(): shutil.copy2(path,r.CURRENT/path.name)
    # Export a correctly composited join review (runtime RGB is premultiplied).
    sheet=Image.new('RGB',(6*260,2*310),(32,32,32))
    for cell,i in enumerate(list(range(6))+list(range(38,44))):
        pixels=frames[i]
        rgb=np.uint8(np.clip(pixels[:,:,:3]+32*(1-pixels[:,:,3:4]/255.),0,255))
        im=Image.fromarray(rgb).crop((350,365,670,750)).resize((250,300))
        sheet.paste(im,((cell%6)*260,(cell//6)*310))
    sheet.save(r.OUT/'join_review.png')
    (r.OUT/'bridge.json').write_text(json.dumps({'frames':44,'fps':60,'bridge_frames_each_end':4,'endpoint_reference':str(flow.IDLE)},indent=2))
