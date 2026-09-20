# 二硫键角色 Spine 骨骼动画—实施计划

> 前置设计：`docs/superpowers/specs/2026-09-16-character-spine-design.md`

## 当前实施状态（2026-09-17）

- [x] 建立时间戳备份 `spine_backup/20260916-204544`。
- [x] 四个源文件与备份的 SHA-256 全部一致。
- [x] 建立 `spine_work`、`Character_rig.psd` 和各隔离目录。
- [x] Spine 3.8.75 可正常进入 PRO 编辑界面。
- [x] 确认未经转换的 3.8 JSON 不能被游戏 Runtime 直接读取，首个差异为数组式 `skins`。
- [x] 建立严格的 `convert_spine_38_to_34.py` 转换工具和游戏 Runtime 解析探针。
- [x] 普通骨骼、区域附件和基础动画通过游戏 Runtime 解析。
- [x] 加权 Mesh 与 `deform` 动画通过游戏 Runtime 解析（21 骨骼、13 插槽、`bounce`）。
- [x] 转换器会拒绝 `transform`、路径/物理约束和剪裁附件等未批准功能，不会静默生成坏文件。
- [ ] 在真实游戏渲染环境中验证 atlas、PNG 和转换后 JSON 的组合。
- [x] 将工作副本 `Character_rig.psd` 等比例放大并保存为 1400×1400；原始 PSD 未修改。
- [x] 生成第一版透明长剑候选 `ai_candidates/sword_candidate_v1.png`（1241×1268 RGBA）。
- [x] 用户确认长剑候选；正式副本保存到 `spine_work/images/weapon/sword.png`，哈希与候选一致。
- [x] 建立 `spine_work/LAYER_MANIFEST.md`，明确左右命名、顶层绘制顺序和附件名称。
- [x] Image 2 重绘的正式全身立绘和 13 个透明角色附件已固化到 `spine_work/images`。
- [x] 建立并保存 `spine_work/DoubleSS_character.spine`；Setup Pose 与正式全身立绘重合。
- [x] 建立 25 根基础骨骼的 `setup_pose_rig_v1`，包含躯干、头部、四肢、双马尾和持剑层级。
- [x] 骨骼版已导入正式 Spine 工程；四肢贴图已由 root 区域附件转换为 Mesh，下一步进行多骨骼权重绑定。
- [x] 双臂与双腿的整肢附件已转换为保持原始 Setup Pose 的基础 Mesh；未使用会纳入透明大画布的自动 `Generate` 网格。
- [x] 双臂 Mesh 已分别手工增加肩、肘、腕三组控制点，双腿 Mesh 已分别增加髋、膝、踝三组控制点；四个附件均为 10 个顶点，外观未发生位移。转换前备份为 `DoubleSS_character_before_manual_mesh.spine`，全四肢控制点阶段备份为 `DoubleSS_character_all_limb_vertices.spine`。
- [x] 已定位自动权重失败原因与安全处理方向：直接“新建/生成网格”会重置附件局部坐标并造成肢体偏移；删除整画布的四个远端角点可保持 Setup Pose 原位。右腿试验中 `upper_leg_R`、`lower_leg_R`、`foot_R` 三骨绑定成功，但 Spine 3.8.75 仍拒绝自动计算权重，因此后续采用确定性的手工顶点权重。
- [ ] 当前 Spine 窗口保留一份**未保存**的右腿紧致网格试验（正式 `DoubleSS_character.spine` 未覆盖）。继续时先隐藏 `weapon/sword` 与 `reference`，再从层级树重新选择 `limbs/leg_R`；髋部点设为 `upper_leg_R`，膝部点设为大腿/小腿过渡，踝部点设为小腿/脚过渡。验证弯膝后再保存独立试验副本并复制到其余三肢。
- [ ] 在 Spine 中缩放长剑并绑定到 `hand_R`。
- [x] 角色拆件已切换为 Image 2 重绘路线；原 PSD 和手工拆件实验均保留，未覆盖。

## 总体目标

以 `Character.psd` 的可见角色设计为唯一人物基准，使用 Spine 3.8.75 制作 `idle`、`hit`、`attack_sword` 三个首期动画，并接入 DoubleSS Mod。所有主动打出的攻击牌默认播放普通挥剑；复杂卡牌可以关闭默认动作并在游戏 Action 队列中触发任意数量的相同或不同动画。

## 阶段 0：隔离并记录并行工作

1. 开始前检查 Git 状态，记录用户和 Claude 已有的未提交改动。
2. 不修改与本功能无关的代码、动画探针或构建产物。
3. 每次开始操作 Photoshop 或 Spine 前，确认对应应用没有被用户或其他代理占用。
4. 美术源文件、工作文件、Spine 工程和游戏导出物严格分目录保存。

验收：能够明确区分本任务新增文件与原有未提交修改。

## 阶段 1：备份和工作目录

在 E 盘建立：

```text
E:\图与psd\img\DoubleSS\char\spine_backup\<时间戳>\
E:\图与psd\img\DoubleSS\char\spine_work\
├─ source\
├─ ai_candidates\
├─ images\
├─ export_3_8\
└─ export_game\
```

操作：

1. 复制原始 `Character.psd`、`Character.png`、`PreAnim.psd` 和 `PreAnim.png` 到时间戳备份目录。
2. 校验备份文件大小或哈希。
3. 从副本创建 `Character_rig.psd`；原文件从此只读。
4. 若 E 盘剩余空间低于 10 GB，把新增工作目录迁移到 `D:\StSmod\DoubleSS_ArtWork`，不移动原始素材。

验收：原始文件未改变，备份可打开，所有后续编辑均指向副本。

## 阶段 2：Spine 3.8 最小兼容性实验

在正式拆图前制作最小测试资源：

```text
test.png
test.atlas
test.json
```

内容仅包含一根骨骼、一个插槽和一个简单循环动画，避免 Mesh 和约束。

接入步骤：

1. 在角色构造流程中临时加载测试骨架。
2. 构建 Mod 并启动游戏。
3. 检查 `ModTheSpire.log` 和游戏画面。
4. 若 3.8 JSON 可直接读取，记录可用字段范围。
5. 若失败，对比 `skeleton.json` 现有 Spine 3.4.02 样本，制作确定性的 3.8 → 游戏格式转换工具。
6. 转换工具只覆盖本项目实际采用的骨骼、插槽、附件、简单 Mesh、权重和基础动画字段；遇到未知字段必须报错，不静默丢弃。

验收：最小骨架在游戏中稳定循环，加载失败时静态角色回退生效。

## 阶段 3：角色拆件和遮挡补画

当前进度（2026-09-16）：`Character_rig.psd` 已放大到 1400×1400，标准顶层图层组已建立，原始可见图层已收入并锁定在 `[ignore] 00_REFERENCE`。下一步从角色右马尾（画面左侧）开始逐件拆分。

美术路线更新（2026-09-16）：停止以放大后的低清原图进行精细手工抠件，改用 Image 2 忠实高清重制后逐件生成。正式基准已锁定为六头身、窄头型、收束双马尾、轻微单腿承重顶胯姿势；双手自然下垂。正式文件为 `spine_work/images/master/character_master.png`，上一版保存在同目录的 `character_master_v3_previous.png`。所有拆件必须以正式基准和原始 `Character.png` 为共同约束，明显的外貌或服装偏差必须修正。

### 3.1 工作画布

1. 将 350×350 人物等比例放大到 1400×1400 工作画布。
2. 保持人物脚底锚点、整体轮廓和当前可见像素相对关系。
3. 可见区域锁定为参考层，任何生成或补画不得覆盖其最终显示部分。

### 3.2 计划拆件

```text
head/
  back_hair
  face
  front_hair
  ponytail_L_1..3
  ponytail_R_1..3
  eye_L_open / eye_L_closed
  eye_R_open / eye_R_closed
  mouth_idle / mouth_hit
  ornament_L / ornament_R

body/
  torso
  collar
  bow
  ss_mark
  pelvis
  skirt_L / skirt_C / skirt_R

arm_L/
  upper_arm / forearm / hand
arm_R/
  upper_arm / forearm / hand

leg_L/
  thigh / shin / foot
leg_R/
  thigh / shin / foot

weapon/
  sword
```

### 3.3 AI 补画

需要补出的区域包括肩膀、袖管内侧、前臂连接处、手掌握持区域、头发根部、裙摆后方和大腿上端。

规则：

1. 每次只处理一个局部部件。
2. 输入图明确区分编辑目标和参考图。
3. 提示中重复约束：只扩展隐藏区域，不改变任何现有可见像素、服装设计、脸、比例和配色。
4. AI 结果保存到 `ai_candidates`，不直接覆盖 PSD。
5. 在 Photoshop 中用蒙版只取被遮挡的新区域，并人工清理接缝。

### 3.4 长剑

1. 从 `PreAnim` 抠取长剑。
2. 补全被右手遮挡的剑柄和护手。
3. 保持剑刃、金色护手和整体造型。
4. 缩放并旋转到 `Character` 右手，待机时剑尖指向屏幕右下。

验收：隐藏原始参考层后，所有部件能组合成与 `Character` 可见部分一致的完整角色；关节旋转 15～30° 时不出现明显空洞。

## 阶段 4：Spine 绑定

1. 使用 PhotoshopToSpine 命名规则导出分件 PNG 和初始 JSON。
2. 在 Spine 3.8.75 创建 `DoubleSS.spine`。
3. 建立 `root → pelvis → torso → neck → head` 主链。
4. 建立左右手臂、左右腿、双马尾、裙摆和右手长剑层级。
5. 身体和四肢优先刚性附件；双马尾、裙摆和袖口才使用有限 Mesh 权重。
6. 首期不使用路径约束、剪裁附件、物理约束或复杂嵌套变形。
7. 将脚底对齐游戏原角色锚点，并按当前 `Character.png` 的占屏高度校准。

验收：Setup Pose 与原图重合；全身骨骼测试旋转无明显断层；图层前后关系正确。

## 阶段 5：首期动画

### `idle`

- 建议循环长度 2.4 秒。
- 身体轻微呼吸和上下移动。
- 双马尾产生延迟摆动。
- 裙摆小幅反向摆动。
- 保持右手持剑。
- 眨眼使用独立轨道或可随机触发的短动画，每 2～5 秒一次。

### `hit`

- 建议长度 0.35～0.45 秒。
- 实际扣血时触发，包含闭眼、身体后仰和头发/裙摆惯性。
- 致死伤害也尝试触发，但不得延迟游戏死亡逻辑；允许被尸体表现截断。

### `attack_sword`

- 建议长度 0.65～0.85 秒。
- 右上蓄力、向敌方单次斜斩、收势、回到持剑待机。
- 不添加发光、剑气或音效。
- 普通攻击忙碌时忽略新的普通挥剑请求。

验收：三段动画在 Spine 预览中无穿帮、抖动和断层，并能自然返回 `idle`。

## 阶段 6：Mod 动画架构

计划新增或修改：

```text
src/main/java/SS/animation/CharacterAnimationRequest.java
src/main/java/SS/animation/CharacterAnimationController.java
src/main/java/SS/action/common/PlayCharacterAnimationAction.java
src/main/java/SS/cards/AbstractDoubleCard.java
src/main/java/SS/characters/AbstractSSCharacter.java
src/main/java/SS/characters/MyCharacter.java
src/main/java/SS/modcore/modcore.java
```

### 默认行为

满足以下条件时播放一次 `attack_sword`：

```text
card.type == ATTACK
!card.isInAutoplay
!card.dontTriggerOnUseCard
```

覆盖所有来源的主动攻击牌，包括无色牌、活动牌和其他 Mod 的牌。

### 单牌扩展点

`AbstractDoubleCard` 提供：

```text
默认自动动作模式
关闭默认动作
返回单个自定义动作
手动向 Action 队列插入多个动画
```

请求策略：

```text
IGNORE_IF_BUSY
INTERRUPT
FORCE_RESTART
```

复杂牌默认不阻塞游戏结算；若要让伤害落在斩击命中帧，由卡牌显式插入 `WaitAction` 或同步 Action。

### 优先级

```text
hit > 特殊动作 > attack_sword > idle
```

缺失动画：写入明确错误日志，忽略该请求并保持当前动画。

### 状态规则

- 一次性动作完成后回到 `idle`。
- 暂停不主动切换动画。
- SL 后重建场景并从 `idle` 开始，不序列化动画轨道时间。
- 战斗结束和切换房间不强制播放额外动作；无一次性动作时保持 `idle`。
- 加载骨骼失败时继续显示静态 `Character.png`。

验收：普通主动攻击挥剑一次；自动打出不挥剑；复杂测试牌可连续触发相同或不同动画；缺失动画只报错、不崩溃。

## 阶段 7：游戏验证

### 基础测试

1. 角色选择并进入战斗。
2. `idle` 长时间循环，无漂移和累积偏移。
3. 主动使用普通攻击，立即播放挥剑。
4. 快速连续使用攻击牌，普通挥剑不反复重启。
5. 自动打出或复制攻击牌，不播放普通挥剑。
6. 实际扣血播放 `hit`；获得格挡、治疗和充能骰子不播放额外动作。
7. 致死伤害不阻塞死亡流程。
8. 暂停、恢复、SL、战斗结束和房间切换后状态合理。

### 兼容与异常测试

1. 动画名拼写错误：日志报错，角色保持当前动作。
2. JSON/atlas/PNG 缺失：显示静态角色，不崩溃。
3. 1920×1080、窗口模式和不同 `Settings.scale` 下检查脚底位置和人物尺寸。
4. 检查图集边缘渗色、透明黑边、大小写路径错误。
5. 运行 Maven 编译与现有相关测试，检查与其他未提交改动的冲突。

验收：完成首期验收清单，并保留一组截图或短录屏用于对比原静态角色。

## 二期保留事项

- 若后续制作角色立绘（区别于当前战斗骨骼角色），头部可能需要改用侧脸构图；开始绘制前单独确认侧脸朝向、角度和视线。侧脸版本仍必须以当前已确认角色为外貌、发型、服装和气质基准，不得擅自改造。
- 2026-09-17 用户已确认面朝画面右侧约 42° 的三分之四侧脸完整角色图，唯一基准为 `D:\StSmod\DoubleSS\art_candidates\character_side_42deg_approved.png`。早期 90° 纯侧面版本废弃。自动隔离头部的首次结果改变了眼睛比例并放大马尾，禁止采用；后续必须从已确认整图精确拆分 `face_base`、`front_hair`、后发/马尾与发饰，再替换 Spine，并保留旧正脸资源备份。
- [x] 已从确认图原像素完成第一版头部拆分，目录为 `D:\StSmod\DoubleSS\art_candidates\side_head_split_v1`：包含完整头部回退件、脸与前发、后发与双马尾、近侧发饰、远侧发饰，以及统一画布版、紧裁版、坐标清单和预览图。四个拆分层的 Alpha 重组误差为 0；未使用发生变脸的自动重绘头部。
- 四阶段受伤反馈：无伤、格挡、`AbstractAlly` 承伤、实际扣血。
- 自动出牌的特殊施法动作。
- 攻击施法、非攻击施法、骰子和卡牌专属动作。
- `victory`、`death` 等扩展动画。
- 未来迁移《杀戮尖塔 2》时替换底层触发适配层，保留卡牌动作声明接口。

## 2026-09-17 制作检查点

- [x] 侧身完整角色的头部、躯干、裙子、前后双臂、前后双腿、鞋与长剑已完成独立图片装配。
- [x] 已建立实际骨骼层级：`root → pelvis → torso/双腿 → chest/双臂 → neck → head`，各图片不再统一挂在 `root`。
- [x] 已生成并成功导入 Spine 3.8.75 数据源 `DoubleSS_character_side_full_v3.json`。
- [x] 已预置循环待机 `idle` 与正式普通挥剑 `attack_sword`；动作总长 0.80 秒，0.12 秒蓄力、0.28 秒命中、0.48 秒收势，并在 0.28 秒触发 `attack_hit` 事件。
- [x] 当前可编辑工程保存为 `E:\图与psd\img\DoubleSS\char\spine_work\DoubleSS_character_side_rigged_v3.spine`；旧工程和旧图片均保留。
- [x] 已在 Spine 中逐帧复核 `attack_sword` 的蓄力、命中、水平收势和回到待机姿势；持剑手、袖口和肩部随骨骼连续运动，动画名已统一为代码约定的 `attack_sword`。
- [ ] 下一步：导出 atlas/PNG/JSON 到 Mod 资源目录，再接入阶段 6 的动画控制器与默认攻击牌触发逻辑。
