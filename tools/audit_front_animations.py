"""Read-only audit of all front runtime frames, Alpha and endpoint alignment."""
from pathlib import Path
import json, hashlib
import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageSequence
import fix_front_prayer_v55 as atlas_util

ROOT=Path(r'D:\StSmod\DoubleSS')
CURRENT=ROOT/'src/main/resources/img/char/spine'
OUT=ROOT/'art_candidates/front_animation_audit_20260929'
PAGES={}

def raw_frame(name,data,regions):
    att=data['skins']['default']['character_frame'][name];r=regions[att.get('path',name)]
    if r['page'] not in PAGES:PAGES[r['page']]=Image.open(CURRENT/r['page']).convert('RGBA')
    x,y=map(int,r['xy'].split(','));w,h=map(int,r['size'].split(','));ow,oh=map(int,r['orig'].split(','));ox,oy=map(int,r['offset'].split(','))
    assert r['rotate']=='false'
    crop=PAGES[r['page']].crop((x,y,x+w,y+h))
    image=Image.new('RGBA',(ow,oh));image.paste(crop,(ox,oh-oy-h))
    if (ow,oh)!=(int(att['width']),int(att['height'])):image=image.resize((int(att['width']),int(att['height'])),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(1024,1024));canvas.paste(image,(round(512+att.get('x',0)-image.width/2),round(512-(att.get('y',0)-210)-image.height/2)))
    return np.asarray(canvas).copy()

def stats(raw):
    a=raw[:,:,3];rgb=raw[:,:,:3].astype(np.float32)
    semi=(a>0)&(a<250);bad=semi&(rgb.max(2)>a+3)
    rate=float(bad.sum()/max(semi.sum(),1));mode='straight_or_mixed' if rate>.02 else 'pma_compatible'
    straight=rgb if mode=='straight_or_mixed' else np.clip(rgb*255/np.maximum(a[:,:,None],1),0,255)
    h=cv2.cvtColor(np.uint8(straight),cv2.COLOR_RGB2HSV)
    pink=(h[:,:,0]>=125)&(h[:,:,0]<=175)&(h[:,:,1]>=65)&(a>=8)
    yy,xx=np.indices(a.shape)
    # Hair/head and shoes only near the character; no gold spell halo scaling.
    hair=(straight[:,:,0]>130)&(straight[:,:,1]>95)&(straight[:,:,2]<straight[:,:,1]*.94)&(a>180)&(xx>370)&(xx<670)&(yy>345)&(yy<510)
    shoes=(h[:,:,0]<25)&(h[:,:,1]>45)&(h[:,:,2]<185)&(a>180)&(yy>665)&(yy<760)&(xx>360)&(xx<680)
    def bbox(mask):
        y,x=np.where(mask)
        return None if not len(y) else [int(x.min()),int(y.min()),int(x.max()),int(y.max())]
    top=int(np.where(hair)[0].min()) if hair.any() else None
    ground=int(np.percentile(np.where(shoes)[0],99.5)) if shoes.any() else None
    return {'alpha_mode':mode,'pma_violation_pixels':int(bad.sum()),'semi_pixels':int(semi.sum()),'pma_violation_rate':round(rate,4),'pink_candidate_pixels':int(pink.sum()),'pink_bbox':bbox(pink),'opaque_bbox':bbox(a>180),'head_hair_top':top,'shoe_ground':ground,'shoe_bbox':bbox(shoes)},np.dstack((straight,a)).astype(np.uint8)

def rendered(raw,bg=32):
    # Actual source-one / one-minus-alpha RGB blend used by game Spine PMA.
    return np.uint8(np.clip(raw[:,:,:3].astype(float)+bg*(1-raw[:,:,3:4]/255),0,255))

def tile(rgb):return Image.fromarray(rgb).crop((320,340,820,770)).resize((400,344),Image.Resampling.LANCZOS)

def main():
    OUT.mkdir(exist_ok=True,parents=True)
    data=json.loads((CURRENT/'character.json').read_text());regions=atlas_util.regions();results={};rows=[]
    idle_name=data['animations']['idle']['slots']['character_frame']['attachment'][0]['name']
    idle_raw=raw_frame(idle_name,data,regions);idle_stats,_=stats(idle_raw)
    for animation,anim in data['animations'].items():
        keys=anim.get('slots',{}).get('character_frame',{}).get('attachment',[])
        folder=OUT/animation;folder.mkdir(exist_ok=True)
        png=folder/'keyframes_png';png.mkdir(exist_ok=True)
        metrics=[];seen={};thumbs={}
        sampled=set([0,len(keys)//4,len(keys)//2,3*len(keys)//4,len(keys)-1])
        for i,k in enumerate(keys):
            name=k['name']
            if name in seen:m=seen[name].copy()
            else:
                raw=raw_frame(name,data,regions);m,straight=stats(raw);seen[name]=m.copy()
                Image.fromarray(straight).save(png/(name+'.png'))
                if i in sampled:thumbs[i]=tile(rendered(raw))
            if i in sampled and i not in thumbs:thumbs[i]=tile(rendered(raw_frame(name,data,regions)))
            metrics.append({'key':i+1,'time':k.get('time',0),'name':name,**m})
        duration=max([k.get('time',0) for k in keys]+[e.get('time',0) for e in anim.get('events',[])])
        first,last=metrics[0],metrics[-1]
        report={'keys':len(keys),'unique_regions':len(seen),'duration':duration,'non_pma_regions':sum(m['alpha_mode']=='straight_or_mixed' for m in seen.values()),'max_pink_candidates':max(m['pink_candidate_pixels'] for m in metrics),'worst_pink_key':max(metrics,key=lambda x:x['pink_candidate_pixels'])['key'],'first':first,'last':last,'all_frames':metrics}
        results[animation]=report
        sheet=Image.new('RGB',(1200,750),(32,32,32));draw=ImageDraw.Draw(sheet)
        tiles=[('idle reference',tile(rendered(idle_raw)))]+[(f'{animation} key {i+1}',thumbs[i]) for i in sorted(sampled)]
        for j,(label,img) in enumerate(tiles):x=j%3*400;y=j//3*375;sheet.paste(img,(x,y));draw.text((x+8,y+350),label,fill='white')
        sheet.save(folder/'contact_runtime.jpg',quality=94)
        rows.append((animation,report));print(animation,len(keys),report['non_pma_regions'],report['max_pink_candidates'],flush=True)
        # A full timed 60fps opaque game-blend preview: no transparent GIF ambiguity.
        gif_folder=folder/'frames_60fps';gif_folder.mkdir(exist_ok=True)
        previews=[];index=0
        for i in range(max(1,round(duration*60))):
            t=i/60
            while index+1<len(keys) and keys[index+1].get('time',0)<=t:index+=1
            name=keys[index]['name'];raw=raw_frame(name,data,regions)
            image=tile(rendered(raw));image.save(gif_folder/f'frame_{i+1:04d}.png');previews.append(image)
        previews[0].save(folder/'runtime_dark_60fps.gif',save_all=True,append_images=previews[1:],duration=[20,10,20]*(len(previews)//3)+[20,10,20][:len(previews)%3],loop=0,disposal=2)
    (OUT/'audit.json').write_text(json.dumps({'idle_reference':idle_stats,'animations':results},indent=2),encoding='utf-8')
    overview=Image.new('RGB',(1600,len(rows)*375),(32,32,32));draw=ImageDraw.Draw(overview)
    for j,(name,r) in enumerate(rows):
        keys=data['animations'][name]['slots']['character_frame']['attachment']
        for c,(label,n) in enumerate([('idle',idle_name),('start',keys[0]['name']),('end',keys[-1]['name'])]):overview.paste(tile(rendered(raw_frame(n,data,regions))),(c*400,j*375))
        draw.text((1205,j*375+20),f'{name}\nkeys={r["keys"]}\nnon-PMA={r["non_pma_regions"]}\npink(max)={r["max_pink_candidates"]}\nhead start/end={r["first"]["head_hair_top"]}/{r["last"]["head_hair_top"]}\nfeet start/end={r["first"]["shoe_ground"]}/{r["last"]["shoe_ground"]}',fill='white')
    overview.save(OUT/'all_endpoints.jpg',quality=94)
    print('DONE',OUT)

if __name__=='__main__':main()
