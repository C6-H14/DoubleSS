"""Candidate-only visual cleanup and full atlas/Alpha regression checks."""
import json, hashlib
import cv2
import numpy as np
from PIL import Image, ImageDraw
import audit_front_animations as audit
import repair_front_animations_v56 as repair

def main():
    export=repair.OUT/'export';audit.CURRENT=export;audit.atlas_util.CURRENT=export;audit.PAGES.clear()
    data=json.loads((export/'character.json').read_text());regions=audit.atlas_util.regions()
    idle_name=data['animations']['idle']['slots']['character_frame']['attachment'][0]['name']
    idle=audit.raw_frame(idle_name,data,regions)
    changes=0;dirty=set()
    hurt=data['animations']['hurt']['slots']['character_frame']['attachment']
    endpoint_names={hurt[0]['name'],hurt[-1]['name']}
    for name in dict.fromkeys(k['name'] for k in hurt):
        raw=audit.raw_frame(name,data,regions);alpha=raw[:,:,3]
        if name in endpoint_names:
            r=regions[name];x,y=map(int,r['xy'].split(','));w,h=map(int,r['size'].split(','));ox,oy=map(int,r['offset'].split(','));ow,oh=map(int,r['orig'].split(','))
            audit.PAGES[r['page']].paste(Image.fromarray(idle).crop((ox,oh-oy-h,ox+w,oh-oy)),(x,y));dirty.add(r['page'])
            continue
        yy,xx=np.indices(alpha.shape)
        # The reviewed source contains an opaque brown, broken floor line
        # between the shoes. Preserve tall ankle/shoe columns, remove only
        # shallow floor fragments in the verified empty inter-foot region.
        solid=(alpha>180).astype(np.uint8)
        support=cv2.filter2D(solid,cv2.CV_16U,np.ones((17,1),np.uint8))
        remove=(alpha>0)&(support<=7)&(yy>=705)&(yy<=730)&(xx>=450)&(xx<=585)
        if not remove.any():continue
        raw[remove]=0;changes+=int(remove.sum())
        r=regions[name];x,y=map(int,r['xy'].split(','));w,h=map(int,r['size'].split(','));ox,oy=map(int,r['offset'].split(','));ow,oh=map(int,r['orig'].split(','))
        assert (ow,oh)==(1024,1024)
        audit.PAGES[r['page']].paste(Image.fromarray(raw).crop((ox,oh-oy-h,ox+w,oh-oy)),(x,y));dirty.add(r['page'])
    for page in dirty:audit.PAGES[page].save(export/page,optimize=True)
    idle_name=data['animations']['idle']['slots']['character_frame']['attachment'][0]['name']
    idle=audit.raw_frame(idle_name,data,regions)
    death_first=data['animations']['death']['slots']['character_frame']['attachment'][0]['name']
    audit.raw_frame(death_first,data,regions)
    r=regions[death_first];x,y=map(int,r['xy'].split(','));w,h=map(int,r['size'].split(','));ox,oy=map(int,r['offset'].split(','));ow,oh=map(int,r['orig'].split(','))
    # Death originally starts from this idle. Keep those initial colors exact.
    audit.PAGES[r['page']].paste(Image.fromarray(idle).crop((ox,oh-oy-h,ox+w,oh-oy)),(x,y))
    audit.PAGES[r['page']].save(export/r['page'],optimize=True)
    pixels=[audit.raw_frame(k['name'],data,regions) for k in hurt[:-1]]
    repair.make_preview('hurt',pixels,.45,repair.OUT/'previews/hurt',idle)
    results={};all_seen=set();overview=Image.new('RGB',(1200,12*370),(32,32,32));draw=ImageDraw.Draw(overview)
    for row,(animation,anim) in enumerate(data['animations'].items()):
        keys=anim['slots']['character_frame']['attachment'];violations=0;maxpink=0
        for k in keys:
            name=k['name']
            if name in all_seen:continue
            all_seen.add(name);raw=audit.raw_frame(name,data,regions)
            violations+=int(np.count_nonzero(raw[:,:,:3]>raw[:,:,3:4]))
            metrics,_=audit.stats(raw);maxpink=max(maxpink,metrics['pink_candidate_pixels'])
        first=audit.raw_frame(keys[0]['name'],data,regions);last=audit.raw_frame(keys[-1]['name'],data,regions)
        assert violations==0,(animation,violations)
        if animation not in ('idle','death'):assert np.array_equal(first,idle) and np.array_equal(last,idle),animation
        results[animation]={'pma_channel_violations':violations,'max_pink_candidates':maxpink,'first_exact_idle':bool(np.array_equal(first,idle)),'last_exact_idle':bool(np.array_equal(last,idle))}
        for col,(label,a) in enumerate([('idle',idle),('first',first),('last',last)]):
            overview.paste(audit.tile(audit.rendered(a)),(col*400,row*370));draw.text((col*400+5,row*370+350),animation+' '+label,fill='white')
        print(animation,results[animation],flush=True)
    original=json.loads((repair.OUT/'backup_front/character.json').read_text())
    assert original['animations']['idle']==data['animations']['idle']
    assert original['animations']['death']==data['animations']['death']
    inner=repair.ROOT/'src/main/resources/img/char/spine_inner'
    results['_checks']={'hurt_ground_line_pixels_removed':changes,'idle_timeline_unchanged':True,'death_timeline_unchanged':True,'region_count':len(all_seen),'inner_hashes':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in inner.iterdir() if p.is_file()}}
    (repair.OUT/'validation.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
    overview.save(repair.OUT/'endpoints_validated.jpg',quality=95)
    print('VALIDATED',changes,flush=True)

if __name__=='__main__':main()
