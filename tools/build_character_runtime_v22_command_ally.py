from pathlib import Path
import importlib.util,json,shutil
from PIL import Image

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/'art_candidates/character_runtime_v21_cast_prayer_matanyone'
OUT=ROOT/'art_candidates/character_runtime_v22_command_ally'
SOURCE=ROOT/'art_candidates/video_source/command_ally/final_v3_degreen_noholes/alpha_frames'
IMAGES=OUT/'images'; EXPORT=OUT/'export'

def main():
    if OUT.exists(): shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True); EXPORT.mkdir(parents=True)
    for p in (BASE/'images').glob('*.png'): shutil.copy2(p,IMAGES/p.name)
    data=json.loads((BASE/'DoubleSS_character_v21_import_3.8.json').read_text(encoding='utf-8'))
    data['skeleton']['images']=str(IMAGES.resolve()).replace('\\','/')+'/'
    attachments=data['skins'][0]['attachments']['character_frame']; paths=sorted(SOURCE.glob('command_ally_*.png'))
    if len(paths)!=16: raise RuntimeError(f'Expected 16 frames, got {len(paths)}')
    for i,p in enumerate(paths,1):
        name=f'command_ally_{i:03d}'; src=Image.open(p).convert('RGBA'); resized=src.resize((556,417),Image.Resampling.LANCZOS)
        canvas=Image.new('RGBA',(1024,1024),(0,0,0,0)); canvas.alpha_composite(resized,(227,336)); canvas.save(IMAGES/f'{name}.png',optimize=True)
        attachments[name]={'path':name,'x':0,'y':210.0,'width':1024,'height':1024}
    keys=[]
    for i in range(1,17):
        key={'name':f'command_ally_{i:03d}'}
        if i>1:key['time']=round((i-1)*0.7/16,6)
        keys.append(key)
    data['animations']['command_ally']={'slots':{'character_frame':{'attachment':keys}},'events':[{'time':0.7,'name':'attack_end'}]}
    imp=OUT/'DoubleSS_character_v22_import_3.8.json'; imp.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    spec=importlib.util.spec_from_file_location('converter',ROOT/'tools/spine-compat-probe/convert_spine_38_to_34.py'); mod=importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
    mod.convert(imp,EXPORT/'character.json'); print('runtime v22 built')

if __name__=='__main__':main()
