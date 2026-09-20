# 战斗结算统计扩展 v2 设计（单卡资源产销 / 卡包审计 / 战损方差 / Power 深度 / DoubleSS 机制）

日期：2026-09-17
状态：待用户审阅
前置：`docs/superpowers/specs/2026-08-30-combat-stats-design.md`（现有 CardStats 系统）

## 0. 需求与已定口径

在现有 Markdown 战斗结算统计上扩展五个模块：

1. **单卡资源产销**：净能量、净过牌、手牌滞留、条件触发率
2. **卡包整体审计**：卡及派生效果归包（本体 / 七宗罪 / Hao / Lost / Shock / C6H14），伤害/格挡贡献占比、净能量/净过牌、牌库占比 vs 打出占比、稀释件标记
3. **战斗节奏**：启动税 T1-T2 承伤占比、战损标准差 + 最大单场、溢空格挡率、溢出伤害
4. **Power 深度监控**：废弃单纯层数，改 BUFF/DEBUFF 台账 = 分幕场均层数 + 激活回合覆盖率 + 等效价值
5. **DoubleSS 特色**：因果账本（罪孽/美德）、奄息依赖度、身份对位契合度

**硬性要求**：导出去掉"按实例"部分，统一按卡 ID 统整；导出显示用**牌名**代替 ID。

用户已拍板的四个口径：

| 问题 | 决定 |
|---|---|
| 条件触发率机制 | 通用注册表（CardConditionRegistry），每张条件牌注册一个谓词 |
| Power 等效范围 | 通用公式（易伤/虚弱/力量/眩晕/Buffer/Intangible/供能）+ DoubleSS 招牌（奄息/罪孽在模块 5 单列） |
| 手牌滞留基准 | EndTurn 计数：同回合抽入打出 = 0；回合结束未打出计 1 次"弃置"；滞留 = 经历的 EndTurn 次数 |
| 身份切换入口 | 由我定位 → **`UpdateFiendStanceDescriptions.update()` / `UpdateManagerStanceDescriptions.update()`**（各带"已是该身份"早退分支，早退后代码 = 精确的真实切换点；全 mod 切换途径均汇入这两处） |

## 1. 钩子落点表（全部已对照 desktop-1.0 反编译源码核实）

| 事件 | 钩点 | 方式 | 记录什么 |
|---|---|---|---|
| 能量增加 | `AbstractPlayer.gainEnergy(int):1609` | SpirePostfix（CombatStatsPatch 新增 OnGainEnergy） | 全部能量增加的唯一收口（GainEnergyAction.update 只调它；SS 角色未覆写）。归属：卡牌栈顶 → 无则本回合最后打出的牌 → 再无则 unattributed |
| 能量扣减 | 现有 `useCard` 前后钩 | 扩展现有 OnPlay：Prefix/Postfix 取 `player.energy.energy` 差 | 差 = 本张牌实际付出（含 -1 特殊费用、0 费牌=0），同步段（useCard:1425 `this.energy.use(costForTurn)`），无异步歧义 |
| 抽牌 | `AbstractPlayer.draw(int)`（`paramtypez={int.class}` 消歧，无参 `draw():1713` 委托给它） | SpirePostfix（新增 OnDraw） | 所有抽牌收口（DrawCardAction/FastDrawCardAction 逐张、开局首抽直调 draw(n) 都汇入）。归属同上（栈顶 → 本回合最后打出牌 → unattributed）；手牌末尾 numCards 张即本次抽入，重置其滞留计数 |
| 回合结束 | **`AbstractRoom.endTurn()`**（AbstractRoom.java:405；唯一调用点 :259 由 `isEndingTurn` 守卫，每玩家回合恰好一次） | SpirePrefix（新增 OnRoomEndTurn） | **不能用 `EndTurnAction.update()`**：room.endTurn() 先排队 DiscardAtEndOfTurnAction（FIFO 先执行、手牌在那里被清），EndTurnAction 被排在队尾，执行时手牌已空。Prefix 是唯一手牌/格挡俱在的点。① 手牌滞留：遍历 `player.hand.group`，每张 `currentDwell++`（打出时结算进 handDwellSum）；`retain/selfRetain` 牌（DiscardAtEndOfTurnAction 走 limbo 不真弃）不记 `handDiscarded++`，其余记 1 次"未打出弃置"；② 溢空格挡：快照 `player.currentBlock`（本版本字段名，AbstractCreature.java:60 public int；**不是** `block`）累加到 Combat.blockCleared（下回合开始 loseBlock 清零的格挡；Barricade/Blur/Calipers 会使分子略偏高，已知近似） |
| 溢出伤害 | 现有 `onMonsterDamage` | 扩展：`overkill = max(0, info.output - target.currentHealth)`（伤害前剩余） | 累加 Combat.overkill + 全局 |
| 敌方眩晕 | 无新钩子 | 现有 `update()` 回合采样：新玩家回合时遍历存活怪物，`intent == Intent.STUN` 者计 1 次 | 战斗级 `stunTurns`（敌方被眩晕跳过的行动回合数，防御价值代理指标）。**原版 StS 1.0 眩晕意图无伤害数值**：4 处 `setMove(…, Intent.STUN)` 全为 2 参重载（AbstractMonster.java:444，不带 baseDamage），`createIntent()` 因 `baseDamage > -1` 判空跳过 `calculateDamage` → `getIntentDmg()` 恒 -1（已核实 BronzeAutomaton/Byrd/ShelledParasite/Lagavulin 四例），不存在"眩晕全额伤害"可折算 |
| 身份切换 | 两个 action 的 `update()` | **直接内联**（我们的类，不打 patch）：早退分支之后调 `CardStats.onStanceSwitch("Fiend"/"Manager")` | 精确真实切换次数（早退天然去重） |
| 罪孽变动 | `SinsPower.stackPower/reducePower` + `onInitialApplication` | **直接内联** `CardStats.onKarmaChange(delta)` / `onKarmaApply(amount)`（我们的类） | 每场罪孽/美德峰值；整局 ±50 边界触发次数（进入边界态才计，delta 前后值比对去重）。已核实：GainVirtueAction 两分支都走 ApplyPowerAction（已存在→stackPower、首次→onInitialApplication，后者在 powers.add 之后调用）→ 全路径覆盖 |
| 奄息免死 | `DyingPower.onPlayerDeath` 两条成功路径（:157 斗篷保护期 / :165 正常保命） | **直接内联** `CardStats.onDyingSaved(roomInfo)` | 整局免死次数 + 关卡；每场"≤2 层危险回合数"在 update() 回合采样时统计 |
| 条件触发 | 现有 `onPlay`（useCard Prefix） | 扩展：按 cardID 查 CardConditionRegistry，先计 total，再跑谓词计 met | 三圣颂：`-sinsAmount >= card.magicNumber`，其中 `sinsAmount = player.hasPower("Double:SinsPower") ? player.getPower("Double:SinsPower").amount : 0`（与 AbstractC6H14Card.getVirtue()=`-countPower("Double:SinsPower")` 同式；countPower 是 AbstractDoubleCard 的 protected 方法（:229），注册表在 SS.stats 包无法调用，故单行复刻并注明保持同式）；巨浪：`target != null && target.hasPower("Vulnerable")`（hasPower public，AbstractCreature.java:808） |
| Power 回合覆盖 | 无新钩子 | 现有 `update()` 回合采样：新玩家回合时，对台账内每个 power ID 查 `player.hasPower(id)`（buff）/ 任一存活怪物 `hasPower(id)`（debuff）→ 活跃则 `coverageTurns[act]++` | 覆盖率 = coverageTurns / 总回合 |

不新增的钩子说明：回合号直接用 `GameActionManager.turn − Combat.turnBaseline`（update() 已在轮询，T1 = 差值 1）；格挡总获得复用现有 `onPlayerBlock`（补一个 Combat 级累加字段）。

## 2. 数据模型扩展

全部字段加到现有嵌套类，act 分桶沿用 `long[ACTS]` / `int[ACTS]` 惯例。

### CardInstance（新增）
```
long[]  energySpent    // 每幕实际付出能量
long[]  energyGained   // 每幕获得能量（归属到本卡）
long[]  drawn          // 每幕归属到本卡的抽牌数
long    handDwellSum   // 打出时累计的滞留回合数（整局，不分幕）
long    handDiscarded  // 回合结束未打出次数（整局）
int     currentDwell   // 临时态：当前在手滞留回合数（每回合末 +1，抽入/打出时清零；不入快照，SL 后从 0 重新累计）
int     conditionTotal // 条件牌打出尝试次数（非条件牌 0）
int     conditionMet   // 条件满足次数
```
（滞留/弃置不分幕：跨幕牌实例在 SL/幕切换重映射下整局累计，分幕意义不大且增加快照面。currentDwell 不入快照。）

### Combat（新增）
```
long    dmgT1, dmgT2          // 第 1/2 回合实际 HP 扣减（启动税分子）
long    blockGained           // 本场总获得格挡（溢空格挡率分母）
long    blockCleared          // 回合末清空格挡累计（溢空格挡率分子）
long    overkill              // 本场溢出伤害
long    damageDealt           // 本场玩家总伤害（含骰子；Overkill 率分母）
int     stunTurns             // 敌方眩晕跳过行动回合数（防御价值代理；意图无伤害值，不做 HP 折算）
int     stanceSwitches        // 本场切换身份次数
int     fiendTurns, managerTurns  // 恶魔之焰/群主身份的回合数（回合采样）
int     lowDyingTurns         // 奄息 ≤2 层的玩家回合数
int     sinPeak, virtuePeak   // 本场罪孽峰值 / 美德峰值（-amount 的最大值）
int     maxHpStart, maxHpEnd  // 本场最大生命值变动
int     dyingSaves            // 本场免死次数
```

### PowerLedger（扩展）
```
public String name = powerId   // 显示名（onPowerApplied 建条目时从 power.name 取；AbstractPower.name public @38；快照恢复缺失时退回 powerId）
int[]   stacksAppliedAct   // 每幕授予层数（现有 stacksApplied 保留为总和）
int[]   coverageTurns      // 每幕激活回合数
long    hitValueSum        // 持有期间玩家每次命中的伤害值累加（onMonsterDamage 按命中事件 += amount；Shackled 等负力量"少打"等效用）
```
保留全部旧字段（stacksApplied/damageDuring/blockDuring/damageTakenDuring/damageDealtToTarget/blockSaved/granters/isDebuff）。
另：`ledgerBlockSaved` 的探测从 `atDamageGive` 改为 `atDamageReceive`（原探测语义是"目标造成的伤害修正"，用在怪物身上会把 Weakened 的 25% 误记为"替玩家挡伤"——本版本怪物防御 debuff 几乎不覆写 atDamageReceive，改后该桶接近 0，属修正而非回归；Weakened/Intangible 的减伤改由 onPlayerHpLoss 显式钩精确累计进 blockSaved）。

### 整局级（新增静态字段）
```
long    karmaPlus50Hits, karmaMinus50Hits  // 整局 50 罪孽 / 50 美德触发次数
long    dyingSaveTotal                     // 整局免死总次数
List<String> dyingSaveRooms                // 免死发生的关卡描述（幕-房间类型-序号）
int     stanceSwitchTotal                  // 整局切换总次数
Map<String,Long> unattributedEnergyGain, unattributedDraws  // 诊断桶
```

### CardConditionRegistry（新文件 `SS/stats/CardConditionRegistry.java`）
```
interface Condition { String label(); boolean met(AbstractCard card, AbstractMonster target); }
static void register(String cardId, Condition c)   // 在 CardStats 静态初始化时注册两张
static Condition lookup(String cardId)
```
初始注册两条：
- `Double:Trisagion`：label="美德≥10"，谓词 `-player.countPower("Double:SinsPower") >= card.magicNumber`
- `Double:GreatWave`：label="目标带易伤"，谓词 `target != null && target.hasPower("Vulnerable")`

## 3. 指标公式（五模块）

### 模块 1 单卡（按卡 ID 统整，分幕 + 全局）
- **净能量** = energyGained − energySpent（每幕/全局各一行）。校验锚点：巨浪吃易伤返能（3 费打出、易伤时每段返 1，Shock/GreatWave.java:54 `GainEnergyAction(1)`）→ 净能量 > 0；KernelMapping（C6H14，`GainEnergyAction(magicNumber)`）→ 净能量 = +magicNumber − 费用。
- **净过牌** = drawn/plays − 1（平均每打出一次抽回多少）。
- **手牌表现**：平均滞留 = handDwellSum/plays（回合数）；打出率 = plays/(plays + handDiscarded)。
- **条件触发率**（仅注册表内的牌显示）：met/total，附 label。

### 模块 2 卡包综合审计（全局，新大节）
归包规则（优先级从上到下）：
1. 牌带 `AbstractCardEnum.Sins` tag → **七宗罪**（现 22 张，tag 是规范定义，不维护硬编码列表）
2. `modcore.cardParentMap.get(cardID)` 命中 → 对应包（Hao / Lost / Shock / C6H14）
3. 其余 → **本体**（原版 StS 牌 + SS 非罪孽核心牌）
骰子归包：`SS:AttackHaoDice` / `SS:DefendHaoDice` → Hao；其余骰子（Attack/Defend/Eternal*/Immolate/Ironwave/Peptide/Wither）→ 本体。

每个包一行 + 机制交互行：
- 伤害贡献占比% = （该包卡直伤 + 归属该包骰子的骰伤害）/ 全场伤害
- 格挡贡献占比% = 同上口径
- 总净能量、总净过牌（该包卡汇总）
- 牌库张数占比 vs 战斗打出次数占比：牌库占比取 `Combat.deckAtStart`（最后一场的快照），打出占比取全局 plays；**打出占比 < 牌库占比 × 0.5 时标"⚠ 卡组稀释件"**
- 机制交互行（自动生成）：`骰伤害 X%（占全场） · 身份切换牌 Y 次`（身份切换牌 = 打出后触发过 onStanceSwitch 归属回合内的牌，简化为：该包含切换卡时显示"含身份切换卡 N 张"）

### 模块 3 战斗节奏（分幕 × 战斗类型聚合 + 每场明细行追加列）
- **启动税 T1-T2 承伤占比** = (dmgT1 + dmgT2) / damageTaken
- **战损标准差 + 最大单场**：对同幕同类型全部战斗的 netHpLoss() 求 stddev 与 max
- **溢空格挡率** = blockCleared / blockGained
- **溢出伤害 Overkill**：每场明细显示绝对值，聚合行显示 Σ overkill / Σ 造成伤害
每场明细行（现有）追加：启动期占比、溢空格挡率、Overkill。

### 模块 4 Power 深度账本（重构原 BUFF/DEBUFF 台账，全局）
每行一个 power（显示 `AbstractPower.name`，`AbstractPower.java:38` public 字段）：
- **分幕场均层数** = stacksAppliedAct[a] / 第 a 幕战斗场数
- **激活回合覆盖率%** = coverageTurns[a] / 第 a 幕总玩家回合数
- **等效价值**（通用公式，按 power ID 选择）：
  - 易伤（debuff）：增伤贡献 = damageDealtToTarget × 1/3（实际伤害中 0.5/1.5 是易伤贡献）
  - 力量（buff）：增伤贡献 = 现有 strengthContrib 汇总（授予比例分摊法，已有）
  - 虚弱（debuff）：减战损等效 = damageTakenDuring × 1/3（0.25/0.75 = 1/3）
  - Shackled（负力量，挂在玩家身上的 debuff，GainStrengthPower POWER_ID="Shackled"）：输出减少等效 = hitValueSum（持有期间每次命中少打的 |负力量| 累加；onMonsterDamage 时若玩家持有 Shackled，按该次命中 amount += amount）
  - 眩晕（enemy 眩晕）：本版本意图**无伤害值**（2 参 `setMove` → baseDamage=-1，`getIntentDmg()` 恒 -1），不做 HP 折算（遵守"不凭空造数"）。防御贡献以 `Combat.stunTurns`（敌方被眩晕跳过的行动回合数，回合采样）单列报告，不进等效伤害列
  - Buffer / Intangible：减战损等效 = 现有 ledgerBlockSaved / ledgerDamageDealt 探针已存的挡伤/免伤值
  - 供能（Energized 等 +1/回）：供能等效 = coverageTurns（激活回合数 × 1 能量）
  - 抽牌等效：保留列；当前无此类 power，恒 0（YAGNI，不建归属）
  - 其余 power：等效列空（只报层数/覆盖）

### 模块 5 DoubleSS 机制收支（全局，新大节）
- **因果账本**：每场（sinPeak / virtuePeak / 净最大生命值变动 = maxHpEnd − maxHpStart）+ 整局（50 罪孽触发 ×N、50 美德触发 ×N、整局净最大生命值变动）。±50 事件在 onKarmaChange 中"进入边界态"才计数（锁后不再触发，天然去重）。
- **奄息依赖度**：整局免死次数 + 关卡列表；每场 lowDyingTurns（奄息 ≤2 层的玩家回合数）——回合采样时读 `player.getPower("Double:DyingPower")` 的 amount（无此 power = 不算危险回合）。
- **身份对位契合度**：每场平均切换身份次数 = stanceSwitchTotal / 战斗场数；恶魔之焰契合度 = Fiend 回合内打出的七宗罪（Sins tag）牌占 Fiend 回合内全部打出的比例；群主契合度 = Manager 回合内打出的群友卡（`Manager` tag 或 Hao 包）占 Manager 回合内全部打出的比例。onPlay 时按当前身份 power 存在性记 per-turn stance play 计数（Combat 加 `fiendSinPlays, fiendPlays, managerHaoPlays, managerPlays` 四个 int）。

## 4. 导出结构重构

终局 Markdown 新节结构（节内表格沿用现有风格）：

```
# 战斗统计 <时间戳>
## 运行信息                          （保留）
## 战斗统计-第X幕                    （每场明细行追加：启动期占比 / 溢空格挡率 / Overkill / 切换身份 / 免死）
## 战斗节奏-分幕聚合                 （新：类型×幕：启动税均值 / 战损 stddev / 最大单场 / 溢空格挡率 / Overkill 率）
## 卡牌统计-第X幕                    （按卡 ID 统整，显示牌名；删除"按实例"节；每卡追加：资源收支 / 手牌表现 / 条件触发率行）
## 卡牌汇总-全局                     （同上，全局）
## 卡包综合审计                      （新：五包表格 + 机制交互行 + 稀释件标记）
## 骰子统计                          （保留 + 归包列）
## Power深度账本                     （重构原台账：分幕场均层数 / 覆盖率 / 等效价值列）
## DoubleSS机制收支                  （新：因果账本 / 奄息 / 身份对位）
## 诊断                              （保留 + unattributedEnergyGain / unattributedDraws 桶）
```

显示名：用现有 `displayName()` 思路，但去掉实例后缀逻辑——同 ID 多张合并为一行（plays/dmg 等已是 ID 级汇总），牌名 + `(EX)` 升级后缀。**删除整个"按实例"导出节**（用户要求）。

## 5. SL 快照 v2 兼容

`_cardstats_state.json` 加 `"v":2`：
- 序列化：CardInstance/Combat/PowerLedger 新字段全部入快照（沿用 longArr/intArr 工具）
- 反序列化：新字段缺失（旧 v1 文件）→ 默认 0，不报错；`v` 缺失按 v1 处理
- 整局级新字段（karma/dying/stance）入快照；`dyingSaveRooms` 存字符串数组

## 6. 文件清单

| 文件 | 改动 |
|---|---|
| `SS/stats/CardStats.java` | 模型扩展、新钩子方法（onGainEnergy/onDraw/onEndTurn/onStanceSwitch/onKarmaChange/onDyingSaved）、update() 回合采样（眩晕意图/覆盖/低奄息/身份回合）、onPlay 扩展（能量差/条件/身份 play）、onMonsterDamage 扩展（overkill）、Combat 字段、导出重构 |
| `SS/stats/CardConditionRegistry.java` | 新增：注册表 + Trisagion/GreatWave 两条谓词 |
| `SS/patches/CombatStatsPatch.java` | 新增 4 个 SpirePatch 类：OnGainEnergy（AbstractPlayer.gainEnergy(int) Postfix）、OnDraw（AbstractPlayer.draw(int) Postfix，`paramtypez={int.class}` 消歧）、OnRoomEndTurn（AbstractRoom.endTurn() Prefix）；OnPlay 扩 Postfix 取 energy 差；OnMonsterDamage 扩 Prefix 记伤害前 HP |
| `SS/action/common/UpdateFiendStanceDescriptions.java` | 早退后内联 `CardStats.onStanceSwitch("Fiend")` |
| `SS/action/common/UpdateManagerStanceDescriptions.java` | 早退后内联 `CardStats.onStanceSwitch("Manager")` |
| `SS/power/SinsPower.java` | stackPower/reducePower 内联 `CardStats.onKarmaChange(amount)` |
| `SS/power/DyingPower.java` | onPlayerDeath 两条成功路径内联 `CardStats.onDyingSaved(...)` |

约定沿用：全部钩子在 `CardStats.enabled` 门槛内（inline 调用点先判 enabled）；不碰任何伤害/状态/动画逻辑；不打 `<init>` 构造器 patch。

## 7. 阶段拆分

- **A 模型+钩子**：数据字段 + OnGainEnergy/OnDraw/OnRoomEndTurn + overkill + onPlay 能量差 → 构建
- **B 模块 1**：条件注册表 + 滞留/弃置/抽牌归属 + 单卡导出行
- **C 模块 3**：dmgT1/T2、blockGained/Cleared、战斗节奏聚合节
- **D 模块 4**：PowerLedger 扩展 + 回合采样覆盖 + 等效公式 + 账本节重构
- **E 模块 2+5**：归包工具 + 卡包审计节 + Sins/Dying/stance 内联 + 机制收支节
- **F 收尾**：删按实例节、ID→牌名、快照 v2、构建部署、打一局验证

## 8. 验证清单（打完一局后）

1. `analysis/*.md` 新节齐全；"按实例"节消失；所有卡行显示牌名
2. 巨浪打出吃易伤 → 净能量 > 0；全神贯注净能量 = +2 − 费用
3. 三圣颂美德不足时打出 → 条件触发率 total+1、met 不变
4. 身份切换一次 → stanceSwitches +1（同场来回切两次 = 2）
5. 原版职业跑一局 → enabled 门槛不生效，无新增开销、无文件写出
6. 读档（SL）后新字段不丢（v2 快照恢复）
7. 构建零告警，jar 双路径部署（antrun）

## 9. 风险与已知近似

- **抽牌/返能归属启发式**（栈顶 → 本回合最后打出牌 → unattributed）：多牌连续抽牌时后一张牌可能被归给前一张；unattributed 桶在诊断节可见，可事后判断偏差量级
- **Shackled 等效**用受击次数 × 力量均值近似（力量可能中途变化），误差可接受
- **条件谓词复刻**：Trisagion 谓词单行复刻 getVirtue()（protected 跨包不可调用），若日后 getVirtue 改逻辑需同步（代码注释标明）
- 七宗罪集合以 `Sins` tag 为唯一规范；新罪孽牌只要带 tag 即自动入审计
