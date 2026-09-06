# 战斗统计系统（卡平衡分析）— 设计规格

日期：2026-08-30。状态：已与用户确认，进入实施。

## 目标

每场游戏结束（死亡或胜利）时，在游戏根目录 `analysis/` 下写出
`yyyyMMdd_HHmmss.txt`，统计四个 act（"四个大层" = actNum 0-3，已确认）内
**卡组中每张牌**的产出数值，以及每 act 的战斗统计。角色无关：原版角色与
SS 系角色走完全相同的统计路径（钩子全在 AbstractPlayer/AbstractMonster 基类层），
原版角色只是没有骰子行。

## 用户已确认的决策

1. **buff/debuff 归因**：基础项 = 直接数值 + 效果台账（按 power 记"生效期间
   伤害/格挡"）。加强项 = 力量/敏捷：记每张牌直接授予的力量/敏捷层数，并把
   命中里力量/敏捷的实际加成按比例分给授予牌。持恒（Permanent tag）牌的伤害
   不吃力量/敏捷加成，不归属。**不**做逐卡反事实重放。
2. **骰子作为准卡牌**：每种骰子单独一行。骰子带 `sources: ArrayList<AbstractCard>`：
   - 卡牌打出时充能（channelOrb 时刻栈顶 = 该牌）→ source = 该牌
   - power 产骰（持续性 power 在回合开始/结束时触发，栈为空）→ 查 power→施加牌 映射
   - 遗物产骰 → sources 为空 → 进"无来源"桶，不记到任何牌
   - 多来源均分（各牌加总 = 实际总伤害；用户澄清：骰子数值生成即固定，按生成事件归因）
3. **战损口径**：净 HP 损失（结束 HP − 开始 HP，带符号）+ 实际受到伤害，都记。
   小怪/精英/BOSS 分开。
4. **开关**：`SpireConfig("Double","Common")` key `StatsExport`，"true"/"false"
   string-boolean（照抄 `Initialized` 模式），默认 true。
5. 多 task 拆分实施，每步编译部署，用户游戏内验证。

## 架构

| 组件 | 职责 |
|---|---|
| `SS/stats/CardStats.java`（新） | 数据模型、卡牌栈、事件记录 API、力量/敏捷来源表、power→牌映射、buff 台账 |
| `SS/stats/StatsExporter.java`（新） | txt 导出（UTF-8，中文表头） |
| `SS/patches/CombatStatsPatch.java`（新） | 游戏类钩子（下表） |
| `SS/Dice/*`、`SS/action/dice/*`（直改） | 骰子 source 系统 |
| `modcore.java`（小改） | 开局重置 + 开关读取 + 每帧驱动 + 终局导出触发 |

## 钩子清单（签名均已在 D:\StSmod\desktop-1.0 反编译源码核实）

| # | 挂点 | 做什么 |
|---|---|---|
| 1 | `AbstractMonster.damage(DamageInfo)` :631 Postfix | 玩家对怪伤害事件：`info.output` 最终值、`info.type`（NORMAL/THORNS/HP_LOSS/DICE/DELAY）、`info.owner`。归属栈顶牌 / 骰子上下文 / power 上下文；遍历玩家 buff + 目标 debuff 记台账；力量贡献归属 |
| 2 | `AbstractPlayer.damage(DamageInfo)` :1433 Postfix | 玩家受伤累计；`isDying` false→true = 死亡 |
| 3 | `AbstractCreature.addBlock(int)` :447 Postfix | 格挡事件：target 为玩家 → 栈顶牌 / 骰子 block 上下文；敏捷贡献归属 |
| 4 | `AbstractPlayer.useCard(c,m,int)` :1404 Prefix | 卡牌栈入栈（记录 `actionManager.actions.size()` 水位）+ 出牌计数 |
| 5 | `MonsterRoom.onPlayerEntry` :45 Postfix | 战斗开始：房间类型（MonsterRoomBoss/MonsterRoomElite/普通，后两者 extends MonsterRoom）、敌人数、act、deck 卡 ID 快照、起始 HP、`GameActionManager.turn` 基线 |
| 6 | `AbstractRoom.endBattle()` :436 Postfix | 战斗胜利收尾：回合数 = turn − 基线；净 HP 变化 |
| 7 | `AbstractPlayer.channelOrb(AbstractOrb)` :2282 Postfix | 骰子 source 捕获（栈顶牌优先；空栈走 power→牌 映射） |
| 8 | `ApplyPowerAction` 构造器 Postfix | power 实例 → 施加牌 映射（source 为玩家且栈顶有牌）；力量/敏捷直接授予累计 |
| 9 | `DamageAllEnemiesAction.update` / `GainBlockAction.update` Postfix | 清骰子 HitAll / 骰子格挡的静态上下文（防误归属） |

终局导出：`modcore.receivePostUpdate` 检测 `AbstractDungeon.deathScreen != null
|| AbstractDungeon.victoryScreen != null`（公开静态字段，DeathScreen.java:66 /
Cutscene.java:124 赋值）→ 写文件，一局一次。

## 核心机制

### 卡牌栈（归属根基，全系统最 tricky 部分）

`useCard` 只是把 action 塞进队列，伤害几帧后才结算。方案 = **按队列深度排水出栈**：
- Prefix 入栈时记录 `actionManager.actions.size()` 为水位 N
- 每帧（receivePostUpdate）：栈顶水位 ≥ 当前 queue size → 弹出
- 嵌套出牌（GhostBomb→虚无牌）自动 LIFO 正确：内层牌水位 > 外层，先弹
- 诊断：栈顶滞留 > 10 秒或战斗结束仍非空 → 记诊断节并清空

### 骰子 source

- `AbstractDice` 加 `public ArrayList<AbstractCard> sources`
- `DiceDamageEnemyAction`（自有类）构造器加 dice 参数；单目标分支
  `DamageInfo` 填 `SpireField<AbstractOrb> diceOrb`（AllyDamagePatch 同款模式）
- HitAll 分支（内部 DamageAllEnemiesAction 自建 DamageInfo）：静态
  `diceAllHitContext` + 钩子 9 清除
- 骰子格挡：myEvoke 时静态 `diceBlockContext` + 钩子 9 清除，addBlock 钩子识别
- 各骰子 myEvoke 调用点直改传 `this`

### 力量/敏捷贡献

- 每牌记：力量直接授予 / 敏捷直接授予（钩子 8 累加）
- 力量来源表（card → 累计授予层数）；命中归属：源牌**非持恒**时，
  `min(当前力量, output)` 按来源比例分给各授予牌（近似：衰减后仍按累计比例，
  在导出文件注释里说明）
- 持恒：源牌 `tags.contains(AbstractCardEnum.Permanent)` → 跳过力量/敏捷归属
- 范围铁律：**只归属游戏真实生效的加成**（实施时读 StrengthPower 确认 DICE 类型
  是否吃力量；不吃则骰子伤害不归属力量，不凭空造数）
- buff 台账：每次伤害/格挡事件遍历玩家 buff（伤害/格挡 DuringActive 累计）与
  受击目标 debuff（DuringActiveOnTarget 累计）；每 power ID 记总层数 + 来源牌(前3)

## 数据模型（一局内存）

- 每牌×每 act：打出次数、总伤害、总伤害(单敌场)、总伤害(群怪场)、总格挡、
  力量贡献、敏捷贡献、力量授予、敏捷授予；每轮打出 = 对 act 内"该牌在 deck"的
  每场(场内打出/场内回合)求平均（需 per-combat per-card 打出计数）
- 每场战斗：act、类型(小怪/精英/BOSS)、敌人数、回合数、起始/结束 HP、受到伤害
- 每 act：平均战斗回合；小怪/精英/BOSS 各：场数、平均净 HP 损失、平均受伤、平均回合
- 每 power ID：施加层数、来源牌、期间伤害、期间格挡、对目标期间伤害
- 骰子×act：总伤害、总格挡、来源构成
- 全局：整局合计牌表（零额外成本）

## 导出格式（节选，中文表头）

```
== 运行信息 == 角色/飞升/结果/到达幕层/卡包配置
== 战斗统计 == 每 act：平均回合 + 三类房各(场数 平均净HP损失 平均受伤 平均回合)
== 卡牌统计-第N幕 == 卡名|打出|均伤|均伤(单敌)|均伤(群怪)|均格挡|每回合打出|力量贡献|敏捷贡献|力量授予|敏捷授予
== 卡牌统计-全局 ==
== BUFF/DEBUFF 台账 == Power|层数|来源牌|期间伤害|期间格挡|目标debuff期受伤
== 骰子统计 == 骰子|act|来源|伤害|格挡
== 诊断 == 无来源伤害/格挡、未跟踪战斗事件、卡牌栈异常
```

## Task 拆分（实施顺序）

1. CardStats 骨架：数据模型 + 卡牌栈(排水) + 开关 + 导出 + 终局检测
2. 基础事件钩子：伤害×2、addBlock、useCard、onPlayerEntry、endBattle
3. 骰子 source 系统：sources 字段 + SpireField + DiceDamageEnemyAction +
   channelOrb/ApplyPowerAction 钩子 + 各骰子直改
4. 力量/敏捷贡献 + 持恒豁免 + buff 台账
5. 完整导出 + 联调（用户原版 + SS 各玩一场核对）

## 风险

- 卡牌栈排水是唯一 tricky 机制；GhostBomb 嵌套为验收标准；诊断节兜底
- `GameActionManager.turn` 是否每场重置、Strength 对 DICE 类型是否生效：
  实施时读源码确认
- 产骰 power 点位约 6-8 处（HaoggernautPower/UnderlyingLogicPower/
  SulfurBlackPower 等），逐个登记；漏了会体现在诊断节
