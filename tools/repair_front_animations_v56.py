"""Versioned front-only Alpha, fringe and endpoint repair; no destructive writes."""
from pathlib import Path
import json, shutil, hashlib, gc
import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt
import audit_front_animations as audit
import build_character_runtime_v25_idle as packer
import build_character_runtime_v46_alt_cast_s_bridge as flow

ROOT=Path(r'D:\StSmod\DoubleSS')
CURRENT=audit.CURRENT
OUT=ROOT/'art_candidates/character_runtime_v56_front_repaired'
PRAYER=Path(r'E:\图与psd\img\DoubleSS\动画最终美术资源_20260924\art_candidates\video_source\cast_prayer\preview_v9_matanyone_grounded\alpha_frames')

def prem(a):
    b=a.copy();b[:,:,:3]=np.uint8((a[:,:,:3].astype(np.uint16)*a[:,:,3:4].astype(np.uint16)+127)//255);return b

def straight(a,is_pma):
    if not is_pma:return a.copy()
    b=a.copy();b[:,:,:3]=np.uint8(np.clip(a[:,:,:3].astype(float)*255/np.maximum(a[:,:,3:4],1),0,255));return b

def clean(a,animation):
    a=a.copy();rgb=a[:,:,:3];alpha=a[:,:,3]
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    pink=(hsv[:,:,0]>=125)&(hsv[:,:,0]<=175)&(hsv[:,:,1]>=50)&(alpha>0)
    safe=(alpha>=235)&~pink
    if not safe.any():return a,0
    dist,nearest=distance_transform_edt(~safe,return_indices=True)
    # Preserve multi-pixel violet spell bodies. Small fringe components are
    # recolored from nearby solid subject, not removed or globally eroded.
    protected=np.zeros(alpha.shape,bool)
    if animation in ('cast_attack_2','cast_debuff'):
        count,labels,stats,_=cv2.connectedComponentsWithStats((pink&(alpha>=80)).astype(np.uint8),8)
        for n in range(1,count):
            if stats[n,cv2.CC_STAT_AREA]>=350:protected|=labels==n
        protected=cv2.dilate(protected.astype(np.uint8),np.ones((3,3),np.uint8)).astype(bool)
    change=pink&(dist<=4)&~protected
    donor=rgb[nearest[0],nearest[1]];a[change,:3]=donor[change]
    a[alpha==0,:3]=0
    return a,int(change.sum())

def landmarks(a):
    hsv=cv2.cvtColor(a[:,:,:3],cv2.COLOR_RGB2HSV);yy,xx=np.indices(a.shape[:2]);alpha=a[:,:,3]
    hair=(a[:,:,0]>130)&(a[:,:,1]>95)&(a[:,:,2]<a[:,:,1]*.94)&(alpha>180)&(xx>370)&(xx<670)&(yy>345)&(yy<510)
    # Allow bright shoes; the audit's V<185 omitted a lit shoe in cast_attack_1.
    brown=(hsv[:,:,0]<25)&(hsv[:,:,1]>40)&(alpha>200)&(yy>685)&(yy<755)&(xx>355)&(xx<680)
    count,labels,stats,_=cv2.connectedComponentsWithStats(brown.astype(np.uint8),8)
    valid=np.zeros(alpha.shape,bool)
    for n in range(1,count):
        x,y,w,h,area=stats[n]
        if area>=15 and w>=5 and h>=3:valid|=labels==n
    ys,xs=np.where(valid)
    if not hair.any() or not len(ys):raise RuntimeError('Missing standing anchors')
    return [float(np.where(hair)[0].min()),float(np.percentile(ys,99.5)),float((xs.min()+xs.max())/2)]

def transform(a,m):
    return cv2.warpAffine(prem(a),m,(1024,1024),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT)

def display(a,bg):return np.uint8(np.clip(a[:,:,:3].astype(float)+bg*(1-a[:,:,3:4]/255),0,255))

def make_preview(animation,pixels,duration,folder,idle):
    png=folder/'frames_60fps';png.mkdir(parents=True,exist_ok=True)
    dark=[];light=[]
    crop=(280,300,900,800)
    writer=cv2.VideoWriter(str(folder/'preview_60fps.mp4'),cv2.VideoWriter_fourcc(*'mp4v'),60,(620,500))
    for i,a in enumerate(pixels):
        rgba=straight(a,True);Image.fromarray(rgba).save(png/f'{animation}_{i+1:04d}.png')
        d=Image.fromarray(display(a,32)).crop(crop);l=Image.fromarray(display(a,180)).crop(crop)
        dark.append(d);light.append(l);writer.write(cv2.cvtColor(np.asarray(d),cv2.COLOR_RGB2BGR))
    writer.release()
    delays=[round((i+1)*100/60)*10-round(i*100/60)*10 for i in range(len(pixels))]
    for label,frames in [('dark',dark),('light',light)]:
        # One fixed palette per clip, no transparency index or frame disposal
        # ambiguity. PNG/MP4 remain the full-color reference.
        idx=np.linspace(0,len(frames)-1,min(12,len(frames))).astype(int)
        palette_sheet=Image.new('RGB',(620,500*len(idx)))
        for j,k in enumerate(idx):palette_sheet.paste(frames[k],(0,500*j))
        palette=palette_sheet.quantize(colors=256,method=Image.Quantize.MEDIANCUT)
        quant=[im.quantize(palette=palette,dither=Image.Dither.NONE) for im in frames]
        quant[0].save(folder/f'preview_{label}_60fps.gif',save_all=True,append_images=quant[1:],duration=delays,loop=0,disposal=2,optimize=False)
        with Image.open(folder/f'preview_{label}_60fps.gif') as gif:
            actual=0
            for j in range(gif.n_frames):gif.seek(j);actual+=gif.info['duration']
            assert abs(actual-sum(delays))<=10
    selected=[0,len(pixels)//4,len(pixels)//2,3*len(pixels)//4,len(pixels)-1]
    sheet=Image.new('RGB',(1200,740),(32,32,32));draw=ImageDraw.Draw(sheet)
    for j,(label,a) in enumerate([('idle',idle)]+[(str(i+1),pixels[i]) for i in selected]):
        image=Image.fromarray(display(a,32)).crop((320,340,820,770)).resize((400,344))
        x=j%3*400;y=j//3*370;sheet.paste(image,(x,y));draw.text((x+8,y+350),animation+' '+label,fill='white')
    sheet.save(folder/'contact.jpg',quality=95)

def main():
    if OUT.exists():raise RuntimeError('Preserve existing version, choose a new output')
    OUT.mkdir(parents=True);backup=OUT/'backup_front';shutil.copytree(CURRENT,backup)
    shutil.copy2(ROOT/'target/DoubleSS.jar',OUT/'DoubleSS_before.jar')
    export=OUT/'export';export.mkdir();packer.EXPORT=export
    data=json.loads((CURRENT/'character.json').read_text());original=json.loads(json.dumps(data));reg=audit.atlas_util.regions()
    attachments={};items=[];page=1;atlas='';reports={};used={};stored={}
    idle_name=data['animations']['idle']['slots']['character_frame']['attachment'][0]['name']
    idle=audit.raw_frame(idle_name,data,reg);idle_straight=straight(idle,True);target=landmarks(idle_straight)
    def store(name,a):
        if name in stored:return
        stored[name]=True;image=Image.fromarray(a);bbox=image.getchannel('A').getbbox();assert bbox
        items.append((name,image,bbox));attachments[name]={'path':name,'x':0,'y':210,'width':1024,'height':1024}
    def flush():
        nonlocal items,page,atlas
        while items:
            items,section,_,_,_=packer.pack_page(items,page);atlas+=section;page+=1
    for animation,anim in original['animations'].items():
        keys=anim['slots']['character_frame']['attachment'];duration=max([k.get('time',0) for k in keys]+[e.get('time',0) for e in anim.get('events',[])])
        if animation in ('idle','death'):
            changed=0
            for k in keys:
                if k['name'] in stored:continue
                raw=audit.raw_frame(k['name'],original,reg)
                if animation=='death':
                    a,c=clean(straight(raw,True),animation);changed+=c
                    # Exact identity wherever no color correction is needed.
                    if c:raw=prem(a)
                store(k['name'],raw)
            reports[animation]={'geometry_unchanged':True,'recolored_pixels':changed,'duration':duration}
            flush();print(animation,'preserved',flush=True);continue
        source=[];cleanup=0
        if animation=='cast_prayer':
            for p in sorted(PRAYER.glob('cast_prayer_*.png')):
                image=Image.open(p).convert('RGBA').resize((512,512),Image.Resampling.LANCZOS)
                canvas=Image.new('RGBA',(1024,1024));canvas.paste(image,(285,310));a,c=clean(np.asarray(canvas),animation);source.append(a);cleanup+=c
            source_times=np.linspace(0,duration,len(source),endpoint=False)
        else:
            for k in keys:
                raw=audit.raw_frame(k['name'],original,reg)
                # These nine legacy groups are established straight Alpha by
                # audit and source builders; no heuristic double premultiply.
                a,c=clean(straight(raw,False),animation);source.append(a);cleanup+=c
            source_times=np.array([k.get('time',0) for k in keys])
        first,last=landmarks(source[0]),landmarks(source[-1])
        transforms=[];fixed=[]
        for i,a in enumerate(source):
            t=float(source_times[i]/duration)
            anchor=np.array(first)*(1-t)+np.array(last)*t
            s=(target[1]-target[0])/(anchor[1]-anchor[0])
            matrix=np.float32([[s,0,target[2]-s*anchor[2]],[0,s,target[1]-s*anchor[1]]])
            transforms.append(matrix.tolist());fixed.append(transform(a,matrix))
        count=round(duration*60);pixels=[]
        for i in range(count):pixels.append(fixed[max(0,int(np.searchsorted(source_times,i/60,side='right')-1))])
        # Only bridge near standing endpoints. The action itself is retained.
        steps=4
        target_frame=pixels[steps].astype(np.float32)
        fw,bw=flow.flow_between(idle.astype(np.float32),target_frame),flow.flow_between(target_frame,idle.astype(np.float32))
        for i in range(steps):pixels[i]=flow.morph(idle.astype(np.float32),target_frame,fw,bw,i/steps)
        begin=count-steps-1;end_source=pixels[begin].astype(np.float32)
        fw,bw=flow.flow_between(end_source,idle.astype(np.float32)),flow.flow_between(idle.astype(np.float32),end_source)
        for i in range(begin+1,count):pixels[i]=flow.morph(end_source,idle.astype(np.float32),fw,bw,(i-begin)/(count-1-begin))
        pixels[0]=idle.copy();pixels[-1]=idle.copy()
        timeline=[];dedup={}
        for i,a in enumerate(pixels):
            assert np.all(a[:,:,:3]<=a[:,:,3:4])
            digest=hashlib.sha256(a.tobytes()).hexdigest()
            if digest not in dedup:
                name=f'front56_{animation}_{len(dedup)+1:03d}';dedup[digest]=name;store(name,a)
            timeline.append({'time':round(i*duration/count,6),'name':dedup[digest]})
        timeline.append({'time':duration,'name':timeline[-1]['name']})
        replacement=json.loads(json.dumps(anim));replacement['slots']['character_frame']['attachment']=timeline
        data['animations'][animation]=replacement
        reports[animation]={'source_first_anchors':first,'source_last_anchors':last,'target_anchors':target,'first_matrix':transforms[0],'last_matrix':transforms[-1],'duration':duration,'frames_60fps':count,'recolored_pixels':cleanup,'first_last_exact_idle':True,'alpha':'premultiplied','source':'original_prayer_rgba' if animation=='cast_prayer' else 'legacy_straight_atlas'}
        make_preview(animation,pixels,duration,OUT/'previews'/animation,idle)
        flush();del pixels,fixed,source;gc.collect();print(animation,reports[animation],flush=True)
    data['skins']['default']['character_frame']=attachments
    data['slots'][0]['attachment']=idle_name
    (export/'character.atlas').write_text(atlas,encoding='utf-8')
    (export/'character.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    assert data['animations']['idle']==original['animations']['idle']
    assert data['animations']['death']==original['animations']['death']
    for name,r in reports.items():
        assert r['duration']==max([k.get('time',0) for k in original['animations'][name]['slots']['character_frame']['attachment']]+[e.get('time',0) for e in original['animations'][name].get('events',[])])
    (OUT/'report.json').write_text(json.dumps(reports,indent=2),encoding='utf-8')
    print('COMPLETE',OUT,flush=True)

if __name__=='__main__':main()
