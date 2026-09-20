const fs = require('fs');

const sourcePath = 'E:/图与psd/img/DoubleSS/char/spine_work/setup_pose_rig_v1.json';
const outputPath = 'D:/StSmod/DoubleSS/art_candidates/side_head_split_v1/DoubleSS_character_side_head_v1.json';
const manifestPath = 'D:/StSmod/DoubleSS/art_candidates/side_head_split_v1/manifest.json';
const rig = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));
const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));

const disabledSlots = new Set([
  'reference', 'ponytail_R', 'ponytail_L', 'face', 'front_hair', 'ornament_R', 'ornament_L',
]);
for (const slot of rig.slots) {
  if (disabledSlots.has(slot.name)) delete slot.attachment;
}

const slotByName = new Map(rig.slots.map((slot) => [slot.name, slot]));
const reference = slotByName.get('reference');
const legR = slotByName.get('leg_R');
const legL = slotByName.get('leg_L');
const torso = slotByName.get('torso');
const skirt = slotByName.get('skirt');
const armR = slotByName.get('arm_R');
const armL = slotByName.get('arm_L');
const sword = slotByName.get('sword');

rig.slots = [
  reference,
  { name: 'side_head_back_hair', bone: 'root', attachment: 'side_head_v1/head_back_hair' },
  legR, legL, torso, skirt, armR, armL,
  { name: 'side_head_face_front', bone: 'root', attachment: 'side_head_v1/head_face_front' },
  { name: 'side_head_ornament_far', bone: 'root', attachment: 'side_head_v1/head_ornament_far' },
  { name: 'side_head_ornament_near', bone: 'root', attachment: 'side_head_v1/head_ornament_near' },
  sword,
];

const attachments = rig.skins[0].attachments;
for (const name of disabledSlots) delete attachments[name];

function region(name) {
  const layer = manifest.layers[name];
  return {
    x: layer.spine_x_at_scale_1,
    y: layer.spine_y_at_scale_1,
    width: layer.width,
    height: layer.height,
  };
}

attachments.side_head_back_hair = { 'side_head_v1/head_back_hair': region('head_back_hair') };
attachments.side_head_face_front = { 'side_head_v1/head_face_front': region('head_face_front') };
attachments.side_head_ornament_far = { 'side_head_v1/head_ornament_far': region('head_ornament_far') };
attachments.side_head_ornament_near = { 'side_head_v1/head_ornament_near': region('head_ornament_near') };

rig.skeleton.hash = '';
rig.skeleton.spine = '3.8.75';
rig.skeleton.images = './images/';
fs.writeFileSync(outputPath, `${JSON.stringify(rig, null, 2)}\n`, 'utf8');
console.log(outputPath);
