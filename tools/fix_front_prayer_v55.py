"""Inspect and repair only front cast_prayer, preserving other runtime assets."""
from pathlib import Path
import json
import shutil
import cv2
from scipy.ndimage import distance_transform_edt
import numpy as np
from PIL import Image, ImageDraw
import build_character_runtime_v25_idle as packer
import build_character_runtime_v46_alt_cast_s_bridge as flow

ROOT=Path(r'D:\StSmod\DoubleSS')
CURRENT=ROOT/'src/main/resources/img/char/spine'
OUT=ROOT/'art_candidates/character_runtime_v55_prayer_fix'

def regions():
    result={}; page=None; name=None
    for line in (CURRENT/'character.atlas').read_text().splitlines():
        if not line.strip(): continue
        if not line.startswith(' '):
            if line.endswith('.png'): page=line;name=None
            elif ':' not in line: name=line;result[name]={'page':page}
        elif name:
            k,v=line.strip().split(':',1);result[name][k]=v.strip()
    return result

def extract(name,data,reg):
    att=data['skins']['default']['character_frame'][name]
    r=reg[att.get('path',name)]
    assert r['rotate']=='false'
    x,y=map(int,r['xy'].split(','));w,h=map(int,r['size'].split(','))
    ow,oh=map(int,r['orig'].split(','));ox,oy=map(int,r['offset'].split(','))
    page=Image.open(CURRENT/r['page']).convert('RGBA')
    canvas=Image.new('RGBA',(ow,oh));canvas.paste(page.crop((x,y,x+w,y+h)),(ox,oh-oy-h))
    a=np.asarray(canvas).astype(np.float32)
    a[:,:,:3]=np.clip(a[:,:,:3]*255/np.maximum(a[:,:,3:4],1),0,255)
    image=Image.fromarray(np.uint8(a),'RGBA')
    if (ow,oh)!=(1024,1024):
        image=image.resize((int(att['width']),int(att['height'])),Image.Resampling.LANCZOS)
        dest=Image.new('RGBA',(1024,1024));dest.paste(image,(round(512+att.get('x',0)-image.width/2),round(512-(att.get('y',0)-210)-image.height/2)));image=dest
    return image

def sheet(images,path):
    result=Image.new('RGB',(1200,400*((len(images)+3)//4)),(30,30,30));d=ImageDraw.Draw(result)
    for i,(label,img) in enumerate(images):
        bg=Image.new('RGBA',img.size,(30,30,30,255));bg.alpha_composite(img)
        tile=bg.resize((400,400));result.paste(tile,((i%4)*300,(i//4)*400));d.text(((i%4)*300,(i//4)*400+380),label,fill='white')
    result.save(path)

def main():
    OUT.mkdir(exist_ok=True,parents=True)
    data=json.loads((CURRENT/'character.json').read_text());reg=regions()
    keys=data['animations']['cast_prayer']['slots']['character_frame']['attachment']
    frames=[extract(k['name'],data,reg) for k in keys]
    idle=extract(data['animations']['idle']['slots']['character_frame']['attachment'][0]['name'],data,reg)
    folder=OUT/'before';folder.mkdir(exist_ok=True)
    for i,f in enumerate(frames):f.save(folder/f'prayer_{i+1:03}.png')
    idle.save(OUT/'idle.png')
    sheet([('idle',idle)]+[(str(i+1),frames[i]) for i in [0,9,19,29,len(frames)-3,len(frames)-2,len(frames)-1]],OUT/'before.jpg')
    print('frames',len(frames));print('attachments',data['skins']['default']['character_frame'][keys[-1]['name']])
    for label,img in [('idle',idle),('first',frames[0]),('last',frames[-1])]:
        a=np.asarray(img);y,x=np.where(a[:,:,3]>180);pink=(a[:,:,0].astype(int)>a[:,:,1].astype(int)+25)&(a[:,:,2].astype(int)>a[:,:,1].astype(int)+15)&(a[:,:,3]>0)
        print(label,'bbox',[int(x.min()),int(y.min()),int(x.max()),int(y.max())],'pink',int(pink.sum()))
    fixed=[];pink_total=0
    # Fixed scale from standing endpoints; effects and kneeling bounds do not
    # determine per-frame scale. Keep the grounded plane throughout the motion.
    scale=(720-393)/(721-433)
    for i,img in enumerate(frames):
        a=np.asarray(img).copy();rgb=a[:,:,:3].astype(int);alpha=a[:,:,3]
        pink=(rgb[:,:,0]>rgb[:,:,1]+25)&(rgb[:,:,2]>rgb[:,:,1]+15)&(alpha>0)
        valid=(alpha>=160)&~pink
        dist,nearest=distance_transform_edt(~valid,return_indices=True)
        replace=pink&(dist<=8)
        a[replace,:3]=a[nearest[0],nearest[1],:3][replace]
        # Detached magenta islands have no subject donor and are plate remnants.
        a[pink&~replace]=0
        pink_total+=int(pink.sum())
        center=524+14*i/(len(frames)-1)
        matrix=np.float32([[scale,0,515-scale*center],[0,scale,720-scale*721]])
        prem=np.asarray(packer.premultiply_alpha(Image.fromarray(a,'RGBA')))
        warped=cv2.warpAffine(prem,matrix,(1024,1024),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT)
        fixed.append(warped)
    pixels=[fixed[round(i*(len(fixed)-1)/71)] for i in range(72)]
    idle_pm=np.asarray(packer.premultiply_alpha(idle)).astype(np.float32)
    # Small pose/style corrections are motion-warped, not a stationary dissolve.
    target=pixels[4].astype(np.float32)
    fw,bw=flow.flow_between(idle_pm,target),flow.flow_between(target,idle_pm)
    for i in range(4):pixels[i]=flow.morph(idle_pm,target,fw,bw,i/4)
    # Begin after the original standing recovery, not while the knees still
    # overlap: flow cannot reliably invent a large bent-to-straight pose change.
    start=67;source=pixels[start].astype(np.float32)
    fw,bw=flow.flow_between(source,idle_pm),flow.flow_between(idle_pm,source)
    for i in range(start+1,72):pixels[i]=flow.morph(source,idle_pm,fw,bw,(i-start)/(71-start))
    pixels[-1]=idle_pm.astype(np.uint8)
    export=OUT/'export';export.mkdir(exist_ok=True)
    for p in CURRENT.iterdir():
        if p.is_file():shutil.copy2(p,export/p.name)
    before=json.loads(json.dumps(data));attachments=data['skins']['default']['character_frame']
    image_dir=OUT/'frames_60fps';image_dir.mkdir(exist_ok=True)
    items=[];timeline=[];previews=[]
    for i,a in enumerate(pixels):
        name=f'prayer_clean_v55_{i+1:03d}';im=Image.fromarray(a,'RGBA')
        attachments[name]={'path':name,'x':0,'y':210,'width':1024,'height':1024}
        items.append((name,im,im.getchannel('A').getbbox()))
        timeline.append({'time':round(i/60,6),'name':name})
        straight=a.astype(np.float32);straight[:,:,:3]=np.clip(straight[:,:,:3]*255/np.maximum(straight[:,:,3:4],1),0,255)
        frame=Image.fromarray(np.uint8(straight),'RGBA');frame.save(image_dir/f'prayer_{i+1:03d}.png')
        bg=Image.new('RGBA',(1024,1024),(30,30,30,255));bg.alpha_composite(frame)
        previews.append(bg.crop((340,370,710,740)).resize((555,555)).convert('RGB'))
    timeline.append({'time':1.2,'name':timeline[-1]['name']})
    data['animations']['cast_prayer']={'slots':{'character_frame':{'attachment':timeline}},'events':[{'time':1.2,'name':'attack_end'}]}
    assert all(data['animations'][k]==v for k,v in before['animations'].items() if k!='cast_prayer')
    packer.EXPORT=export;page=packer.next_page_number(export)
    while items:
        items,section,_,_,_=packer.pack_page(items,page)
        with (export/'character.atlas').open('a') as f:f.write(section)
        page+=1
    (export/'character.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    # GIF timing quantized in centiseconds while retaining 60 output frames/s.
    previews[0].save(OUT/'prayer_fixed_60fps.gif',save_all=True,append_images=previews[1:],duration=[20,10,20]*24,loop=0,disposal=2)
    writer=cv2.VideoWriter(str(OUT/'prayer_fixed_60fps.mp4'),cv2.VideoWriter_fourcc(*'mp4v'),60,(554,554))
    for im in previews:writer.write(cv2.cvtColor(np.asarray(im.resize((554,554))),cv2.COLOR_RGB2BGR))
    writer.release()
    sheet([('idle',idle)]+[(str(i+1),Image.open(image_dir/f'prayer_{i+1:03d}.png')) for i in [0,17,35,53,63,67,71]],OUT/'after.jpg')
    report={'scale':scale,'removed_pink_pixels':pink_total,'frames':72,'fps':60,'duration':1.2,'end_exact_idle':bool(np.array_equal(pixels[-1],idle_pm.astype(np.uint8))),'other_animations_preserved':True}
    (OUT/'report.json').write_text(json.dumps(report,indent=2));print(report)

if __name__=='__main__':main()
