const fs = require('fs');

const bodyManifestPath = 'D:/StSmod/DoubleSS/art_candidates/side_body_split_v1/manifest.json';
const headManifestPath = 'D:/StSmod/DoubleSS/art_candidates/side_head_split_v1/manifest.json';
const completedManifestPath = 'D:/StSmod/DoubleSS/art_candidates/side_body_split_v1/completed/manifest.json';
const outputPath = 'D:/StSmod/DoubleSS/art_candidates/side_body_split_v1/DoubleSS_character_side_attack_sword_aligned_v5.json';

const body = JSON.parse(fs.readFileSync(bodyManifestPath, 'utf8'));
const head = JSON.parse(fs.readFileSync(headManifestPath, 'utf8'));
const completed = JSON.parse(fs.readFileSync(completedManifestPath, 'utf8'));

// Setup pose joint locations are expressed in Spine world coordinates.  The
// hierarchy is deliberately simple so the first animation pass remains easy
// to tune in Spine 3.8.
const bones = [
  { name: 'root' },
  { name: 'pelvis', parent: 'root', x: 20, y: 100 },
  { name: 'torso', parent: 'pelvis', x: -20, y: 90, rotation: 90, length: 210 },
  { name: 'chest', parent: 'torso', x: 210, length: 70 },
  { name: 'neck', parent: 'chest', x: 70, length: 35 },
  { name: 'head', parent: 'neck', x: 35, length: 120 },

  { name: 'upper_arm_far', parent: 'chest', y: 100, rotation: -180, length: 170 },
  { name: 'forearm_far', parent: 'upper_arm_far', x: 170, y: -20, length: 180 },
  { name: 'hand_far', parent: 'forearm_far', x: 180, y: -30, length: 65 },

  { name: 'upper_arm_near', parent: 'chest', y: -120, rotation: -180, length: 170 },
  { name: 'forearm_near', parent: 'upper_arm_near', x: 170, y: 40, length: 175 },
  { name: 'hand_near', parent: 'forearm_near', x: 175, y: 60, length: 65 },

  { name: 'upper_leg_far', parent: 'pelvis', x: -85, rotation: -90, length: 330 },
  { name: 'lower_leg_far', parent: 'upper_leg_far', x: 330, y: -15, length: 310 },
  { name: 'foot_far', parent: 'lower_leg_far', x: 310, y: -40, rotation: 75, length: 120 },

  { name: 'upper_leg_near', parent: 'pelvis', x: 45, rotation: -90, length: 330 },
  { name: 'lower_leg_near', parent: 'upper_leg_near', x: 330, y: -10, length: 320 },
  { name: 'foot_near', parent: 'lower_leg_near', x: 320, y: -20, rotation: 75, length: 120 },
];

const boneByName = new Map(bones.map((bone) => [bone.name, bone]));
const worldByName = new Map();
function worldTransform(name) {
  if (worldByName.has(name)) return worldByName.get(name);
  const bone = boneByName.get(name);
  if (!bone.parent) {
    const result = { x: bone.x || 0, y: bone.y || 0, rotation: bone.rotation || 0 };
    worldByName.set(name, result);
    return result;
  }
  const parent = worldTransform(bone.parent);
  const radians = parent.rotation * Math.PI / 180;
  const x = bone.x || 0;
  const y = bone.y || 0;
  const result = {
    x: parent.x + x * Math.cos(radians) - y * Math.sin(radians),
    y: parent.y + x * Math.sin(radians) + y * Math.cos(radians),
    rotation: parent.rotation + (bone.rotation || 0),
  };
  worldByName.set(name, result);
  return result;
}

function localRegion(worldRegion, boneName) {
  const bone = worldTransform(boneName);
  const radians = -bone.rotation * Math.PI / 180;
  const dx = worldRegion.x - bone.x;
  const dy = worldRegion.y - bone.y;
  return {
    x: Number((dx * Math.cos(radians) - dy * Math.sin(radians)).toFixed(3)),
    y: Number((dx * Math.sin(radians) + dy * Math.cos(radians)).toFixed(3)),
    rotation: Number(((worldRegion.rotation || 0) - bone.rotation).toFixed(3)),
    ...(worldRegion.scaleX == null ? {} : { scaleX: worldRegion.scaleX }),
    ...(worldRegion.scaleY == null ? {} : { scaleY: worldRegion.scaleY }),
    width: worldRegion.width,
    height: worldRegion.height,
  };
}

function bodyWorldRegion(name) {
  const layer = body.layers[name];
  // Alignment-first production pass: preserve the approved source pixels and
  // their exact crop dimensions.  The earlier completed limbs changed widths
  // and therefore shifted visible contours even when their centers matched.
  return { x: layer.spine_x_at_scale_1, y: layer.spine_y_at_scale_1, width: layer.width, height: layer.height };
}
function headWorldRegion(name) {
  const layer = head.layers[name];
  return { x: layer.spine_x_at_scale_1, y: layer.spine_y_at_scale_1, width: layer.width, height: layer.height };
}

const slotSpecs = [
  ['arm_far_upper', 'upper_arm_far', 'side_body_v1/arm_far_upper'],
  ['arm_far_forearm', 'forearm_far', 'side_body_v1/arm_far_forearm'],
  ['hand_far', 'hand_far', 'side_body_v1/hand_far'],
  ['leg_far_thigh', 'upper_leg_far', 'side_body_v1/leg_far_thigh'],
  ['leg_far_lower', 'lower_leg_far', 'side_body_v1/leg_far_lower'],
  ['foot_far', 'foot_far', 'side_body_v1/foot_far'],
  ['head_back_hair', 'head', 'side_head_v1/head_back_hair'],
  ['torso_blouse', 'torso', 'side_body_v1/torso_blouse'],
  ['leg_near_thigh', 'upper_leg_near', 'side_body_v1/leg_near_thigh'],
  ['leg_near_lower', 'lower_leg_near', 'side_body_v1/leg_near_lower'],
  ['foot_near', 'foot_near', 'side_body_v1/foot_near'],
  ['skirt_with_waistband', 'pelvis', 'side_body_v1/skirt_with_waistband'],
  ['arm_near_upper', 'upper_arm_near', 'side_body_v1/arm_near_upper'],
  ['arm_near_forearm', 'forearm_near', 'side_body_v1/arm_near_forearm'],
  ['hand_near', 'hand_near', 'side_body_v1/hand_near'],
  ['head_face_front', 'head', 'side_head_v1/head_face_front'],
  ['head_ornament_far', 'head', 'side_head_v1/head_ornament_far'],
  ['head_ornament_near', 'head', 'side_head_v1/head_ornament_near'],
  ['sword', 'hand_far', 'weapon/sword'],
];

const slots = slotSpecs.map(([name, bone, attachment]) => ({ name, bone, attachment }));
const attachments = {};
for (const [name, boneName, path] of slotSpecs) {
  let worldRegion;
  if (name === 'sword') {
    worldRegion = { x: -86, y: -206, rotation: -12, scaleX: 0.4, scaleY: 0.4, width: 1241, height: 1268 };
  } else if (name.startsWith('head_')) {
    worldRegion = headWorldRegion(name);
  } else {
    worldRegion = bodyWorldRegion(name);
  }
  attachments[name] = { [path]: localRegion(worldRegion, boneName) };
}

const animations = {
  idle: {
    bones: {
      torso: { rotate: [{ angle: 0 }, { time: 1, angle: 1.4 }, { time: 2, angle: 0 }] },
      head: { rotate: [{ angle: 0 }, { time: 1, angle: -1.8 }, { time: 2, angle: 0 }] },
      upper_arm_far: { rotate: [{ angle: 0 }, { time: 1, angle: -1.2 }, { time: 2, angle: 0 }] },
      upper_arm_near: { rotate: [{ angle: 0 }, { time: 1, angle: 1.2 }, { time: 2, angle: 0 }] },
    },
  },
  attack_sword: {
    bones: {
      pelvis: { translate: [{ x: 0, y: 0 }, { time: 0.12, x: -5 }, { time: 0.28, x: 10 }, { time: 0.8, x: 0 }] },
      torso: { rotate: [{ angle: 0 }, { time: 0.12, angle: -6 }, { time: 0.28, angle: 10 }, { time: 0.48, angle: 6 }, { time: 0.8, angle: 0 }] },
      upper_arm_far: { rotate: [{ angle: 0 }, { time: 0.12, angle: -25 }, { time: 0.28, angle: 115 }, { time: 0.48, angle: 72 }, { time: 0.8, angle: 0 }] },
      forearm_far: { rotate: [{ angle: 0 }, { time: 0.12, angle: 25 }, { time: 0.28, angle: -65 }, { time: 0.48, angle: -20 }, { time: 0.8, angle: 0 }] },
      hand_far: { rotate: [{ angle: 0 }, { time: 0.12, angle: -12 }, { time: 0.28, angle: 32 }, { time: 0.48, angle: 12 }, { time: 0.8, angle: 0 }] },
      upper_arm_near: { rotate: [{ angle: 0 }, { time: 0.28, angle: -10 }, { time: 0.48, angle: -5 }, { time: 0.8, angle: 0 }] },
      head: { rotate: [{ angle: 0 }, { time: 0.28, angle: -6 }, { time: 0.48, angle: -3 }, { time: 0.8, angle: 0 }] },
    },
    events: [{ time: 0.28, name: 'attack_hit' }],
  },
};

const rig = {
  skeleton: { hash: '', spine: '3.8.75', x: -512, y: -768, width: 1024, height: 1536, images: './images/' },
  bones,
  slots,
  skins: [{ name: 'default', attachments }],
  events: { attack_hit: {} },
  animations,
};

fs.writeFileSync(outputPath, `${JSON.stringify(rig, null, 2)}\n`, 'utf8');
console.log(outputPath);
