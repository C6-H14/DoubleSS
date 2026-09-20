const fs = require('fs');

const setupPath = 'E:/图与psd/img/DoubleSS/char/spine_work/setup_pose_rig_v1.json';
const bodyManifestPath = 'D:/StSmod/DoubleSS/art_candidates/side_body_split_v1/manifest.json';
const headManifestPath = 'D:/StSmod/DoubleSS/art_candidates/side_head_split_v1/manifest.json';
const completedManifestPath = 'D:/StSmod/DoubleSS/art_candidates/side_body_split_v1/completed/manifest.json';
const outputPath = 'D:/StSmod/DoubleSS/art_candidates/side_body_split_v1/DoubleSS_character_side_full_v2.json';

const rig = JSON.parse(fs.readFileSync(setupPath, 'utf8'));
const body = JSON.parse(fs.readFileSync(bodyManifestPath, 'utf8'));
const head = JSON.parse(fs.readFileSync(headManifestPath, 'utf8'));
const completed = JSON.parse(fs.readFileSync(completedManifestPath, 'utf8'));

const bodyParts = [
  'arm_far_upper', 'arm_far_forearm', 'hand_far',
  'leg_far_thigh', 'leg_far_lower', 'foot_far',
  'torso_blouse',
  'leg_near_thigh', 'leg_near_lower', 'foot_near',
  'skirt_with_waistband',
  'arm_near_upper', 'arm_near_forearm', 'hand_near',
];
const headParts = ['head_back_hair', 'head_face_front', 'head_ornament_far', 'head_ornament_near'];

rig.skeleton.hash = '';
rig.skeleton.spine = '3.8.75';
rig.skeleton.images = './images/';
rig.slots = [];
for (const name of bodyParts.slice(0, 6)) {
  rig.slots.push({ name, bone: 'root', attachment: `side_body_v2/${name}` });
}
rig.slots.push({ name: 'head_back_hair', bone: 'root', attachment: 'side_head_v1/head_back_hair' });
rig.slots.push({ name: 'torso_blouse', bone: 'root', attachment: 'side_body_v2/torso_blouse' });
for (const name of bodyParts.slice(7, 10)) {
  rig.slots.push({ name, bone: 'root', attachment: `side_body_v2/${name}` });
}
rig.slots.push({ name: 'skirt_with_waistband', bone: 'root', attachment: 'side_body_v2/skirt_with_waistband' });
for (const name of bodyParts.slice(11)) {
  rig.slots.push({ name, bone: 'root', attachment: `side_body_v2/${name}` });
}
for (const name of headParts.slice(1)) {
  rig.slots.push({ name, bone: 'root', attachment: `side_head_v1/${name}` });
}
rig.slots.push({ name: 'sword', bone: 'root', attachment: 'weapon/sword' });

const attachments = {};
function bodyRegion(name) {
  const layer = body.layers[name];
  const dims = completed.parts[name] || { width: layer.width, height: layer.height };
  return {
    x: layer.spine_x_at_scale_1,
    y: layer.spine_y_at_scale_1,
    width: dims.width,
    height: dims.height,
  };
}
function headRegion(name) {
  const layer = head.layers[name];
  return {
    x: layer.spine_x_at_scale_1,
    y: layer.spine_y_at_scale_1,
    width: layer.width,
    height: layer.height,
  };
}
for (const name of bodyParts) {
  attachments[name] = { [`side_body_v2/${name}`]: bodyRegion(name) };
}
for (const name of headParts) {
  attachments[name] = { [`side_head_v1/${name}`]: headRegion(name) };
}
attachments.sword = {
  'weapon/sword': { x: -86, y: -206, rotation: -12, scaleX: 0.4, scaleY: 0.4, width: 1241, height: 1268 },
};
rig.skins = [{ name: 'default', attachments }];
rig.animations = {};
fs.writeFileSync(outputPath, `${JSON.stringify(rig, null, 2)}\n`, 'utf8');
console.log(outputPath);
