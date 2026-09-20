# 战斗统计扩展 v2 实施计划（单卡资源产销 / 卡包审计 / 战斗节奏 / Power 深度 / DoubleSS 机制）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 CardStats Markdown 结算统计上扩展五模块（单卡资源产销、卡包整体审计、战斗节奏、Power 深度账本、DoubleSS 机制收支），并按用户硬性要求删掉「按实例」导出节、统一按卡 ID 统整、导出用牌名代替 ID。

**Architecture:** 全部增量钩子（4 个新 SpirePatch + 现有 OnPlay/OnMonsterDamage 扩展 + 4 处自有类内联调用点）写入 CardStats 既有数据模型（CardInstance/Combat/PowerLedger 加字段 + 整局级静态字段），终局导出重构为新节结构，SL 快照升 v2（新字段缺失容错回退 v1 语义）。

**Tech Stack:** Java 8、BaseMod + ModTheSpire 3.23.2（SpirePatch/SpirePrefixPatch/SpirePostfixPatch/paramtypez）、Gson（老 API new JsonParser().parse()）、Maven（system scope 依赖，antrun 部署）。

**Spec:** docs/superpowers/specs/2026-09-17-combat-stats-extension-design.md（本计划所有口径以此为准；spec §1 钩子表已全部对照 D:/StSmod/desktop-1.0 反编译源码核实）

## Global Constraints

- **构建命令**（每个代码任务的门）：
  `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
  期望 `BUILD SUCCESS`。Maven 输出含 NUL 字节必须 `tr -d '\000'` 过滤。
- **最终任务**用 `mvn -o -B clean package`——antrun 自动把 jar 部署到两处：
  `E:/SteamLibrary/steamapps/common/SlayTheSpire/mods/DoubleSS.jar`
  `E:/SteamLibrary/steamapps/common/SlayTheSpire/DoubleSS/content/DoubleSS.jar`
- **无 git 仓库** → 无 commit 步骤。**无 junit** → 无单测；最终验证 = 用户打一局对照 spec §8 清单。
- **CardStats.enabled 门槛**：所有 CardStats 入口方法第一行 `if (!enabled) return;`（4 处内联调用点依赖此约定，调用处不重复判）。
- **不打 <init> 构造器 patch**（项目红线）；所有 SpirePatch 纯统计、不碰伤害/状态/动画逻辑。
- **本版本字段名**（desktop-1.0 已核实，勿按新版 StS 习惯写错）：
  - 格挡 = `currentBlock`（AbstractCreature:60 `public int`，**不是 block**）
  - HP = `currentHealth` / `maxHealth`（:58-59 `public int`）
  - 能量 = `player.energy.energy`（EnergyManager.energy `public int`）
  - 回合 = `GameActionManager.turn`（`public static int`，跨战斗累积，战斗开始记 baseline）
  - 意图 = `monster.intent`（public）；`AbstractPower.name`（public）
- **MTS 重载消歧**：AbstractPlayer.draw 有 `draw()` 和 `draw(int)` 两个重载 → patch draw 时必须 `paramtypez = { int.class }` 锁定 int 版本（无参版委托到它，全路径都过它，且避免双计数）。gainEnergy(int)、AbstractRoom.endTurn() 均无重载，不需要 paramtypez。
- **回合号约定**：`turnNo = GameActionManager.turn - combat.turnBaseline`（T1 = 1）。GameActionManager 在玩家回合开始 `++turn`（:367），故 turnNo==N 时正处于第 N 个玩家回合。
- **Power ID 常量**（已核实）：Vulnerable="Vulnerable"、Weakened="Weakened"、Strength="Strength"、Shackled="Shackled"（GainStrengthPower，负力量，挂玩家）、IntangiblePlayer="IntangiblePlayer"、Energized="Energized"。SS：SinsPower="Double:SinsPower"、DyingPower="Double:DyingPower"、FiendStance="Double:FiendStance"、ManagerStance="Double:ManagerStance"。
- **卡包 ID**（modcore.cardParentMap 的 value）：Double:HaoPackage / Double:LostPackage / Double:ShockPackage / Double:C6H14Package；其余（含原版 StS + SS 非罪孽核心）= 本体。
- **七宗罪集合**：`card.hasTag(SS.path.AbstractCardEnum.Sins)`（现 22 张，tag 是规范定义，勿硬编码列表）。
- **骰子归包**：orbId 为 Double:AttackHaoDice / Double:DefendHaoDice → Hao；其余骰子 → 本体。
- **切换牌**：Double:FreedomOfSpeech、Double:TransportProtein（含这两张的包显示「含身份切换卡 N 张」）。
- 编码/注释风格沿用 CardStats.java 现状：中文注释、静态方法、`String.format(java.util.Locale.US, ...)`。

## 文件结构

| 文件 | 责任 |
|---|---|
| SS/stats/CardStats.java | 数据模型扩展、全部新事件入口、update() 回合采样、导出重构、快照 v2（核心文件） |
| SS/stats/CardConditionRegistry.java | **新建**：条件牌注册表 + 三圣颂/巨浪两条谓词 |
| SS/patches/CombatStatsPatch.java | 新增 OnGainEnergy / OnDraw / OnRoomEndTurn 3 个 SpirePatch 类；OnPlay 加 Postfix 取能量差；OnMonsterDamage 加 Prefix 记伤害前 HP |
| SS/action/common/UpdateFiendStanceDescriptions.java | 早退后内联 `CardStats.onStanceSwitch("Fiend")` |
| SS/action/common/UpdateManagerStanceDescriptions.java | 早退后内联 `CardStats.onStanceSwitch("Manager")` |
| SS/power/SinsPower.java | stackPower/reducePower 内联 onKarmaChange(delta)；onInitialApplication 内联 onKarmaApply(amount) |
| SS/power/DyingPower.java | onPlayerDeath 两条成功路径内联 onDyingSaved() |


---

### Task 1: 数据模型扩展（CardInstance / Combat / PowerLedger / 整局级字段）

**Files:**
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（模型类 :90-189、整局状态区 :196-208、clear() :327-348、onCombatStart :749-779、finalizeCombat :790-796）

**Interfaces:**
- Consumes: 现有 CardInstance/Combat/PowerLedger 嵌套类、ACTS=4、clear()、onCombatStart、finalizeCombat
- Produces: 后续所有任务依赖的新字段（精确名称见下）：
  - CardInstance: `long[] energySpent, energyGained, drawn`、`long handDwellSum, handDiscarded`、`int currentDwell, conditionTotal, conditionMet`
  - Combat: `long dmgT1, dmgT2, blockGained, blockCleared, overkill, damageDealt`、`int stunTurns, stanceSwitches, fiendTurns, managerTurns, lowDyingTurns, sinPeak, virtuePeak, maxHpStart, maxHpEnd, dyingSaves`、`int fiendSinPlays, fiendPlays, managerHaoPlays, managerPlays`、`Set<AbstractMonster> stunned`
  - PowerLedger: `public String name`、`int[] stacksAppliedAct, coverageTurns`、`long hitValueSum`
  - 静态: `karmaPlus50Hits, karmaMinus50Hits, dyingSaveTotal, stanceSwitchTotal`、`ArrayList<String> dyingSaveRooms`、`unattributedEnergyGain, unattributedDraws`、`AbstractCard lastPlayedCard`
  - 方法: `onKarmaChange(int delta)`、`onKarmaApply(int amount)`、`onStanceSwitch(String stance)`、`onDyingSaved()`

- [ ] **Step 1: CardInstance 加字段**（在 `copyIndex` 字段 :111 之后、无参构造器 :113 之前插入）

```java
        // ---- v2 资源产销 ----
        /** 每幕实际付出能量（useCard 前后 energy 差）。 */
        public long[] energySpent = new long[ACTS];
        /** 每幕归属到本卡的能量增加（gainEnergy 栈顶/本回合最后打出牌归属）。 */
        public long[] energyGained = new long[ACTS];
        /** 每幕归属到本卡的抽牌数。 */
        public long[] drawn = new long[ACTS];
        /** 打出时累计的滞留回合数（整局；EndTurn 计数口径）。 */
        public long handDwellSum = 0;
        /** 回合结束未打出次数（整局；retain/selfRetain 不计）。 */
        public long handDiscarded = 0;
        /** 临时态：当前在手滞留回合数（回合末 +1，抽入/打出清零；不入快照）。 */
        public int currentDwell = 0;
        /** 条件牌：打出尝试次数 / 条件满足次数（注册表外的牌恒 0）。 */
        public int conditionTotal = 0;
        public int conditionMet = 0;
```

- [ ] **Step 2: Combat 加字段**（在 `finalized` 字段 :139 之后、rounds() :142 之前插入）

```java
        // ---- v2 战斗节奏 / DoubleSS 机制 ----
        /** 第 1/2 回合实际 HP 扣减（启动税分子）。 */
        public long dmgT1 = 0, dmgT2 = 0;
        /** 本场总获得格挡（溢空格挡率分母）。 */
        public long blockGained = 0;
        /** 回合末清空格挡累计（溢空格挡率分子；room.endTurn Prefix 快照 currentBlock）。 */
        public long blockCleared = 0;
        /** 本场溢出伤害（伤害超出怪物剩余 HP 的部分）。 */
        public long overkill = 0;
        /** 本场玩家总伤害（含骰子；Overkill 率分母）。 */
        public long damageDealt = 0;
        /** 敌方眩晕跳过行动回合数（意图无伤害值，防御价值代理，不做 HP 折算）。 */
        public int stunTurns = 0;
        /** 本场切换身份次数。 */
        public int stanceSwitches = 0;
        /** 恶魔之焰/群主身份回合数（回合采样）+ 身份回合内打出计数。 */
        public int fiendTurns = 0, managerTurns = 0;
        public int fiendSinPlays = 0, fiendPlays = 0, managerHaoPlays = 0, managerPlays = 0;
        /** 奄息 <=2 层的玩家回合数（无 DyingPower = 不危险）。 */
        public int lowDyingTurns = 0;
        /** 本场罪孽峰值 / 美德峰值。 */
        public int sinPeak = 0, virtuePeak = 0;
        /** 本场最大生命值变动。 */
        public int maxHpStart = 0, maxHpEnd = 0;
        /** 本场免死次数。 */
        public int dyingSaves = 0;
        /** 当前处于眩晕意图的怪物（意图边沿检测，防重复计数）。 */
        public final Set<AbstractMonster> stunned = new HashSet<>();
```

- [ ] **Step 3: PowerLedger 加字段**（在 `isDebuff` 字段 :164 之后、构造器 :166 之前插入；构造器体第一行加 `this.name = powerId;`）

```java
        /** 显示名（onPowerApplied 建条目时取 power.name；快照恢复缺失时保持 powerId）。 */
        public String name = null;
        /** 每幕授予层数（stacksApplied 保留为总和）。 */
        public int[] stacksAppliedAct = new int[ACTS];
        /** 每幕激活回合数（新玩家回合采样：buff 查玩家 / debuff 查任一存活怪物）。 */
        public int[] coverageTurns = new int[ACTS];
        /** 持有期间玩家每次命中少打的力量值累加（Shackled 负力量等效）。 */
        public long hitValueSum = 0;
```

- [ ] **Step 4: 整局级静态字段**（在 `forcedPops` 字段 :207 之后插入）

```java
    // ---- v2 DoubleSS 机制 / 资源产销 诊断 ----
    private static long karmaPlus50Hits = 0;   // 整局 50 罪孽触发次数
    private static long karmaMinus50Hits = 0;  // 整局 50 美德触发次数
    private static long dyingSaveTotal = 0;    // 整局免死总次数
    private static int stanceSwitchTotal = 0;  // 整局切换身份总次数
    private static final ArrayList<String> dyingSaveRooms = new ArrayList<>();
    private static long unattributedEnergyGain = 0; // 诊断桶：无归属能量增加
    private static long unattributedDraws = 0;      // 诊断桶：无归属抽牌
    /** 本回合最后打出的牌（gainEnergy/draw 无栈顶时的归属兜底；回合末清空）。 */
    private static AbstractCard lastPlayedCard = null;
```

- [ ] **Step 5: clear() 补清零**（:347 `channeledDice.clear();` 之后追加）

```java
        karmaPlus50Hits = 0;
        karmaMinus50Hits = 0;
        dyingSaveTotal = 0;
        stanceSwitchTotal = 0;
        dyingSaveRooms.clear();
        unattributedEnergyGain = 0;
        unattributedDraws = 0;
        lastPlayedCard = null;
```

- [ ] **Step 6: onCombatStart 记 maxHpStart**（:769 `c.hpStart = ...` 之后加一行）

```java
        c.maxHpStart = AbstractDungeon.player.maxHealth;
```

- [ ] **Step 7: finalizeCombat 记 maxHpEnd**（:794 `c.hpEnd = hpEnd;` 之后加一行）

```java
        c.maxHpEnd = AbstractDungeon.player.maxHealth; // endBattle 体内先调 player.onVictory（罪孽结算 maxHp 变动）再返回，Postfix 时已生效
```

- [ ] **Step 8: 新增四个事件入口方法**（在 `powerSourcesFor` 方法 :1102-1108 之后插入）

```java
    // ==================== v2 事件入口 ====================

    /**
     * 罪孽变动（SinsPower.stackPower/reducePower 内联）。delta = 变动后-变动前（已含 +-50 钳制）。
     * 峰值：每场罪孽/美德峰值；+-50 边界：进入边界态才计（前后值比对天然去重，lock 后不再变动）。
     */
    public static void onKarmaChange(int delta) {
        if (!enabled) {
            return;
        }
        AbstractPower p = AbstractDungeon.player != null
                ? AbstractDungeon.player.getPower("Double:SinsPower") : null;
        if (p == null) {
            return;
        }
        int now = p.amount;
        int prev = now - delta;
        if (now == 50 && prev != 50) {
            karmaPlus50Hits++;
        }
        if (now == -50 && prev != -50) {
            karmaMinus50Hits++;
        }
        if (currentCombat != null) {
            if (now > currentCombat.sinPeak) {
                currentCombat.sinPeak = now;
            }
            if (-now > currentCombat.virtuePeak) {
                currentCombat.virtuePeak = -now;
            }
        }
    }

    /** 罪孽首次授予（SinsPower.onInitialApplication 内联；ApplyPowerAction 在 powers.add 之后调用它）。 */
    public static void onKarmaApply(int amount) {
        onKarmaChange(amount);
    }

    /** 身份切换（两个 Update*StanceDescriptions action 早退守卫之后内联；早退天然去重）。 */
    public static void onStanceSwitch(String stance) {
        if (!enabled) {
            return;
        }
        stanceSwitchTotal++;
        if (currentCombat != null) {
            currentCombat.stanceSwitches++;
        }
    }

    /** 奄息免死（DyingPower.onPlayerDeath 两条成功路径内联）。 */
    public static void onDyingSaved() {
        if (!enabled) {
            return;
        }
        dyingSaveTotal++;
        if (currentCombat != null) {
            currentCombat.dyingSaves++;
        }
        String room = "A" + AbstractDungeon.actNum + "-F" + AbstractDungeon.floorNum;
        AbstractRoom r = AbstractDungeon.getCurrRoom();
        if (r instanceof MonsterRoomBoss) {
            room += "-BOSS";
        } else if (r instanceof MonsterRoomElite) {
            room += "-精英";
        }
        dyingSaveRooms.add(room);
    }
```

- [ ] **Step 9: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`


---

### Task 2: 新增 SpirePatch 钩子 + 现有钩子扩展（能量/抽牌/回合结束/伤害前HP）

**Files:**
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/patches/CombatStatsPatch.java`（OnPlay :52-58、OnMonsterDamage :60-67 扩展；OnBlockGain :166-173 之后加 3 个新类）
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（onPlay :801-830、onMonsterDamage :833-880、onPlayerHpLoss :883-907、onPlayerBlock :910-948、ledgerBlockSaved :1340-1387 扩展）

**Interfaces:**
- Consumes: Task 1 全部新字段；CardStats 现有 stack/getOrCreate/stackTopCard/lastPlayedCard/ledgers
- Produces: 入口签名（精确）：`onPlayEnd(int energyDelta)`、`onGainEnergy(int e)`、`onDraw(int numCards)`、`onRoomEndTurn()`、`onMonsterDamage(DamageInfo, AbstractMonster, int hpBefore)`（原双参签名废弃，patch 同步改）

- [ ] **Step 1: CardStats.onPlay 末尾扩展**（:828 注释 `// 水位在 useCard 方法体执行前捕获` 之前插入）

```java
        // v2：滞留结算（打出即清零）+ 本回合最后打出牌 + 身份对位计数
        ins.handDwellSum += ins.currentDwell;
        ins.currentDwell = 0;
        lastPlayedCard = c;
        AbstractPlayer pl = AbstractDungeon.player;
        if (pl.hasPower("Double:FiendStance")) {
            currentCombat.fiendPlays++;
            if (c.hasTag(AbstractCardEnum.Sins)) {
                currentCombat.fiendSinPlays++;
            }
        } else if (pl.hasPower("Double:ManagerStance")) {
            currentCombat.managerPlays++;
            boolean hao = "Double:HaoPackage".equals(SS.modcore.modcore.cardParentMap.get(c.cardID));
            if (c.hasTag(AbstractCardEnum.Manager) || hao) {
                currentCombat.managerHaoPlays++;
            }
        }
```

- [ ] **Step 2: CardStats 新增四个入口**（onPlay 方法结束后、onMonsterDamage 注释之前插入）

```java
    /** 打出结束（useCard Postfix）：energyDelta = 打出前-打出后（本张牌实际付出；-1 特殊费用/0费=0）。 */
    public static void onPlayEnd(int energyDelta) {
        if (!enabled || energyDelta <= 0 || currentCombat == null) {
            return;
        }
        Frame top = stack.peek();
        if (top != null) {
            getOrCreate(top.card).energySpent[currentCombat.act] += energyDelta;
        }
    }

    /** 能量增加（gainEnergy Postfix，全路径唯一收口；GainEnergyAction.update 只调它，SS 角色未覆写）。归属：栈顶 → 本回合最后打出牌 → 无归属桶。 */
    public static void onGainEnergy(int e) {
        if (!enabled || e <= 0) {
            return;
        }
        AbstractCard target = stackTopCard();
        if (target == null) {
            target = lastPlayedCard;
        }
        if (target == null || currentCombat == null) {
            unattributedEnergyGain += e;
            return;
        }
        getOrCreate(target).energyGained[currentCombat.act] += e;
    }

    /** 抽牌（draw(int) Postfix，全路径收口：无参 draw() :1713 委托 draw(1)、逐张抽、开局首抽 draw(n) 都汇入 int 版本）。手牌末尾 numCards 张即本次抽入，重置其滞留计数。 */
    public static void onDraw(int numCards) {
        if (!enabled || numCards <= 0) {
            return;
        }
        AbstractCard target = stackTopCard();
        if (target == null) {
            target = lastPlayedCard;
        }
        if (target != null && currentCombat != null) {
            getOrCreate(target).drawn[currentCombat.act] += numCards;
        } else {
            unattributedDraws += numCards;
        }
        List<AbstractCard> hand = AbstractDungeon.player.hand.group;
        int from = Math.max(0, hand.size() - numCards);
        for (int i = from; i < hand.size(); i++) {
            CardInstance ins = instances.get(hand.get(i).uuid);
            if (ins != null) {
                ins.currentDwell = 0;
            }
        }
    }

    /**
     * 回合结束（AbstractRoom.endTurn Prefix——唯一入口 :259，isEndingTurn 守卫每玩家回合一次）。
     * 必须钩这里而非 EndTurnAction.update()：room.endTurn() 先排队 DiscardAtEndOfTurnAction
     * （FIFO 先执行、手牌在那里清空），EndTurnAction 被排在队尾，执行时手牌已空。
     * ① 在手牌 currentDwell++；retain/selfRetain 走 limbo 不真弃，不记 handDiscarded，其余记 1。
     * ② 溢空格挡：currentBlock 快照（下回合开始 loseBlock 清零；Barricade/Blur/Calipers 使分子略偏高，已知近似）。
     */
    public static void onRoomEndTurn() {
        if (!enabled) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        for (AbstractCard c : p.hand.group) {
            CardInstance ins = getOrCreate(c);
            ins.currentDwell++;
            if (!c.retain && !c.selfRetain) {
                ins.handDiscarded++;
            }
        }
        if (currentCombat != null) {
            currentCombat.blockCleared += p.currentBlock;
        }
        lastPlayedCard = null; // 新回合的 gainEnergy/draw 不再归属旧回合的牌
    }
```


- [ ] **Step 3: onMonsterDamage 改签名 + 扩展**

签名改 `public static void onMonsterDamage(DamageInfo info, AbstractMonster target, int hpBefore)`。
在 `if (amount <= 0) { return; }` 之后、骰子分支之前插入：

```java
        // v2：本场总伤害（含骰子，先于归属分支累计）+ 溢出伤害（超出怪物伤害前剩余 HP）
        currentCombat.damageDealt += amount;
        currentCombat.overkill += Math.max(0, amount - hpBefore);
```

在卡牌直伤分支 `if (top != null) {` 内、`ledgerDamageDealt(amount, target);` 之前插入：

```java
            // v2：玩家持有 Shackled（负力量）时每次命中少打的力量值
            AbstractPower sh = AbstractDungeon.player.getPower("Shackled");
            if (sh != null && sh.amount < 0) {
                PowerLedger L = ledgers.get("Shackled");
                if (L != null) {
                    L.hitValueSum += Math.min(-sh.amount, amount);
                }
            }
```

- [ ] **Step 4: onPlayerHpLoss 扩展**（`currentCombat.damageTaken += loss;` 之后插入）

```java
        // v2：启动税分桶（turnNo = 绝对回合 - 基线，T1=1）
        int turnNo = GameActionManager.turn - currentCombat.turnBaseline;
        if (turnNo == 1) {
            currentCombat.dmgT1 += loss;
        } else if (turnNo == 2) {
            currentCombat.dmgT2 += loss;
        }
        // v2：减伤等效（精确折算，替代 ledgerBlockSaved 的 atDamageGive 误判）
        if (info != null && info.owner != null && info.owner != AbstractDungeon.player
                && info.owner.hasPower("Weakened")) {
            PowerLedger L = ledgers.get("Weakened");
            if (L != null) {
                L.blockSaved += loss / 3; // loss = base*0.75 → 少打 base-loss = loss/3
            }
        }
        if (info != null && info.owner != null && info.owner != AbstractDungeon.player
                && AbstractDungeon.player.hasPower("IntangiblePlayer") && info.output > loss) {
            PowerLedger L = ledgers.get("IntangiblePlayer");
            if (L != null) {
                L.blockSaved += info.output - loss; // 无形把伤害压到 1：差额为免伤
            }
        }
```

- [ ] **Step 5: onPlayerBlock 扩展**（`int act = currentCombat.act;` 之后插入一行）

```java
        currentCombat.blockGained += amount;
```

- [ ] **Step 6: ledgerBlockSaved 探测改 atDamageReceive**（:1349 与 :1376 两处 `pw.atDamageGive(1f, ...)` 都改成 `pw.atDamageReceive(1f, DamageInfo.DamageType.NORMAL)`）。方法头注释补一句：原 atDamageGive 探测「目标造成的伤害修正」，怪物 Weakened 的 0.75 会被误记为替玩家挡伤；改 atDamageReceive（目标受到的伤害修正）语义才正确。本版本怪物防御 debuff 几乎不覆写它，该桶接近 0 属修正而非回归。

- [ ] **Step 7: CombatStatsPatch 扩展 OnPlay / OnMonsterDamage**

OnPlay（:52-58）改为：

```java
    @SpirePatch(clz = AbstractPlayer.class, method = "useCard")
    public static class OnPlay {
        private static int energyBefore;

        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance, AbstractCard c, AbstractMonster m, int energyOnUse) {
            energyBefore = __instance.energy.energy;
            CardStats.onPlay(c);
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractCard c, AbstractMonster m, int energyOnUse) {
            // useCard:1425 this.energy.use(costForTurn) 在同步段，前后差 = 本张牌实际付出
            CardStats.onPlayEnd(energyBefore - __instance.energy.energy);
        }
    }
```

OnMonsterDamage（:60-67）改为：

```java
    @SpirePatch(clz = AbstractMonster.class, method = "damage")
    public static class OnMonsterDamage {
        private static int hpBefore;

        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            hpBefore = __instance.currentHealth;
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance, DamageInfo info) {
            if (info != null && info.owner == AbstractDungeon.player) {
                CardStats.onMonsterDamage(info, __instance, hpBefore);
            }
        }
    }
```

- [ ] **Step 8: CombatStatsPatch 文件尾加 3 个新钩子类**（OnBlockGain 类的 `}` 之后、类结束 `}` 之前）

```java
    // ==================== v2：能量 / 抽牌 / 回合结束 ====================

    /** 能量增加唯一收口：GainEnergyAction.update 只调它；SS 角色未覆写 gainEnergy。 */
    @SpirePatch(clz = AbstractPlayer.class, method = "gainEnergy")
    public static class OnGainEnergy {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, int e) {
            CardStats.onGainEnergy(e);
        }
    }

    /** 抽牌收口：draw() 与 draw(int) 是重载对，paramtypez={int.class} 锁定 int 版本（无参版委托 draw(1)；patch 无参版会双计数）。 */
    @SpirePatch(clz = AbstractPlayer.class, method = "draw", paramtypez = { int.class })
    public static class OnDraw {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, int numCards) {
            CardStats.onDraw(numCards);
        }
    }

    /** 回合结束：钩 AbstractRoom.endTurn()（:405 唯一签名无重载）而非 EndTurnAction.update()（后者执行时手牌已被 DiscardAtEndOfTurnAction 清空）。唯一调用点 :259 由 player.isEndingTurn 守卫；MonsterRoom/Elite/Boss 无覆写（已核实）。 */
    @SpirePatch(clz = AbstractRoom.class, method = "endTurn")
    public static class OnRoomEndTurn {
        @SpirePrefixPatch
        public static void Prefix(AbstractRoom __instance) {
            CardStats.onRoomEndTurn();
        }
    }
```

（`AbstractRoom` import 已存在于 CombatStatsPatch :16，勿重复添加。）

- [ ] **Step 9: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`
注：MTS 钩子加载错误（paramtypez/endTurn 签名）不在编译期暴露，会在游戏启动时写入 `sendToDevs/mts_process_launch.log`——Task 9 打第一局时核对该日志无本 mod SpirePatch 加载失败记录。


---

### Task 3: update() 回合采样 + Power 记账扩展（模块 4 数据侧）

**Files:**
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（update() :1122-1151、onPowerApplied :1040-1093、onCombatStart :749-779）

**Interfaces:**
- Consumes: Task 1 字段（coverageTurns/stacksAppliedAct/stunned/fiendTurns/managerTurns/lowDyingTurns）；GameActionManager.turn
- Produces: 无新公开方法；`sampleNewTurn(Combat c, int turnNo)` 私有方法（后续导出任务只读字段）

**采样点语义**（已核实）：`GameActionManager.turn` 在玩家回合开始 `++`（:367）。现有 update() 已有 `turn > maxTurn` 判断（:1131）——把它升级为"新回合采样点"，每回合恰好一次。debuff 覆盖 = 任一存活怪物 `hasPower(id)`；buff 覆盖 = 玩家 `hasPower(id)`。

- [ ] **Step 1: onCombatStart 初始化 maxTurn 采样基线**（无需改动——`c.maxTurn = c.turnBaseline`（:771）已使第一次 `turn > maxTurn` 恰好落在 T1）

- [ ] **Step 2: update() 的 maxTurn 块升级为新回合采样**（:1130-1133 替换为）

```java
        // 回合计数 + v2 新回合采样（turn 跨战斗累积，maxTurn 初值=基线，第一次超越恰为 T1）
        if (currentCombat != null && GameActionManager.turn > currentCombat.maxTurn) {
            currentCombat.maxTurn = GameActionManager.turn;
            sampleNewTurn(currentCombat, GameActionManager.turn - currentCombat.turnBaseline);
        }
```

- [ ] **Step 3: 新增私有方法 sampleNewTurn**（在 update() 方法之后插入）

```java
    /**
     * 新玩家回合采样（每回合一次）：Power 激活覆盖 / 奄息危险 / 身份回合。
     * 在回合开始时采样——此时玩家回合内将生效的 buff 已就位（applyStartOfTurnPowers 已执行）。
     */
    private static void sampleNewTurn(Combat c, int turnNo) {
        if (turnNo < 1) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        // 身份回合
        if (p.hasPower("Double:FiendStance")) {
            c.fiendTurns++;
        } else if (p.hasPower("Double:ManagerStance")) {
            c.managerTurns++;
        }
        // 奄息危险（<=2 层）
        AbstractPower dy = p.getPower("Double:DyingPower");
        if (dy != null && dy.amount <= 2) {
            c.lowDyingTurns++;
        }
        // Power 激活覆盖（buff 查玩家 / debuff 查任一存活怪物）
        for (PowerLedger L : ledgers.values()) {
            boolean active;
            if (L.isDebuff) {
                active = false;
                com.megacrit.cardcrawl.monsters.AbstractMonsterGroup g =
                        AbstractDungeon.getCurrRoom() != null ? AbstractDungeon.getCurrRoom().monsters : null;
                if (g != null) {
                    for (AbstractMonster m : g.monsters) {
                        if (!m.isDeadOrEscaped() && m.hasPower(L.powerId)) {
                            active = true;
                            break;
                        }
                    }
                }
            } else {
                active = p.hasPower(L.powerId);
            }
            if (active) {
                L.coverageTurns[c.act]++;
            }
        }
    }
```

- [ ] **Step 4: update() 加眩晕意图边沿检测**（在 Step 2 的 maxTurn 块之后、卡牌栈排水块之前插入）

```java
        // v2 眩晕边沿检测：怪物意图转为 STUN 时计 1（该回合跳过行动）；转回其他意图时移出集合，
        // 允许再次眩晕再计。已核实四例眩晕怪（Lagavulin/BronzeAutomaton/Byrd/ShelledParasite）
        // 的 STUN 意图回合都真实执行了 STUNNED 文本+RollMoveAction，意图被新意图替换 → 每跳过一回合恰好计一次。
        if (currentCombat != null && AbstractDungeon.getCurrRoom() != null
                && !AbstractDungeon.getCurrRoom().isBattleOver) {
            com.megacrit.cardcrawl.monsters.AbstractMonsterGroup g =
                    AbstractDungeon.getCurrRoom().monsters;
            for (AbstractMonster m : g.monsters) {
                if (m.isDeadOrEscaped()) {
                    currentCombat.stunned.remove(m);
                    continue;
                }
                boolean st = m.intent == com.megacrit.cardcrawl.monsters.AbstractMonster.Intent.STUN;
                if (st && !currentCombat.stunned.contains(m)) {
                    currentCombat.stunned.add(m);
                    currentCombat.stunTurns++;
                } else if (!st) {
                    currentCombat.stunned.remove(m);
                }
            }
        }
```

- [ ] **Step 5: onPowerApplied 记分幕层数 + 显示名**（:1077-1092 台账块内修改）

台账创建处（:1080-1083）加 name 捕获：
```java
            if (L == null) {
                L = new PowerLedger(power.ID, debuff);
                if (power.name != null && !power.name.isEmpty()) {
                    L.name = power.name;
                }
                ledgers.put(power.ID, L);
            }
```
授予层数处（:1084-1091 `if (power.amount > 0) {` 块内、`L.stacksApplied += power.amount;` 之后）加：
```java
                L.stacksAppliedAct[act] += power.amount;
```
（`act` 变量已存在于 :1061 `int act = currentCombat != null ? currentCombat.act : 0;`——注意它在 `if (playerOwned && grantCardList != null && power.amount > 0)` 块内声明，台账块拿不到。把台账块的 act 改为独立取：在台账块 `boolean monsterDebuff = ...` 之后加一行 `int ledgerAct = currentCombat != null ? currentCombat.act : 0;`，分幕累计用它。）

- [ ] **Step 6: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`


---

### Task 4: 条件触发率（CardConditionRegistry + 单卡导出行，模块 1 收尾）

**Files:**
- Create: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardConditionRegistry.java`
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（onPlay 加条件计数、export() 卡牌分组行加资源收支/手牌表现/条件行、新增导出辅助方法）

**Interfaces:**
- Consumes: Task 1/2 字段（conditionTotal/conditionMet/energySpent/energyGained/drawn/handDwellSum/handDiscarded）
- Produces: `CardConditionRegistry.lookup(String cardId)` 返回 `CardConditionRegistry.Condition`（可 null）；导出辅助 `appendCardGroupStats(StringBuilder, List<CardInstance>, Integer act)`（Task 8 复用）

**条件谓词口径**（spec 已核实）：
- `Double:Trisagion`（三圣颂）：label="美德≥10"；谓词 = 美德值 ≥ card.magicNumber。美德值 = `-SinsPower.amount`（复刻 AbstractC6H14Card.getVirtue()，:79 `return -countPower("Double:SinsPower")`，countPower :229 = `hasPower ? amount : 0`）。注意与 use() 的门槛 `if (getVirtue() < magicNumber) return;`（:49）同式——条件满足 = 牌真正生效。
- `Double:GreatWave`（巨浪）：label="目标带易伤"；谓词 = `target != null && target.hasPower("Vulnerable")`。

- [ ] **Step 1: 新建 CardConditionRegistry.java**

```java
package SS.stats;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件牌触发率注册表（spec 模块 1：门槛/条件触发率）。
 * 每张"有条件才生效"的牌注册一个谓词：onPlay 时 total++，谓词成立 met++。
 * 新增条件牌只需 register 一条，不改 CardStats。
 */
public class CardConditionRegistry {

    public interface Condition {
        /** 显示在导出行里的条件名（如"美德≥10"）。 */
        String label();

        /** 打出瞬间判定条件是否满足（与牌 use() 的门槛同式）。 */
        boolean met(AbstractCard card, AbstractMonster target);
    }

    private static final Map<String, Condition> CONDITIONS = new HashMap<>();

    public static void register(String cardId, Condition c) {
        CONDITIONS.put(cardId, c);
    }

    public static Condition lookup(String cardId) {
        return cardId == null ? null : CONDITIONS.get(cardId);
    }

    static {
        // 三圣颂：use() 门槛 getVirtue() < magicNumber 时直接 return（Trisagion.java:49）。
        // getVirtue() = -countPower("Double:SinsPower")（AbstractC6H14Card:79，protected 跨包不可调），
        // 此处单行复刻保持同式——若 getVirtue 改逻辑需同步。
        register("Double:Trisagion", new Condition() {
            @Override
            public String label() {
                return "美德≥N";
            }

            @Override
            public boolean met(AbstractCard card, AbstractMonster target) {
                AbstractPower sins = AbstractDungeon.player.getPower("Double:SinsPower");
                int virtue = (sins == null) ? 0 : -sins.amount;
                return virtue >= card.magicNumber;
            }
        });
        // 巨浪：目标带易伤时每段伤害返 1 能量并叠 1 虚弱（GreatWave.java:53-58）
        register("Double:GreatWave", new Condition() {
            @Override
            public String label() {
                return "目标带易伤";
            }

            @Override
            public boolean met(AbstractCard card, AbstractMonster target) {
                return target != null && target.hasPower("Vulnerable");
            }
        });
    }
}
```

- [ ] **Step 2: onPlay 加条件计数（签名透传目标怪物 m）**

onPlay 签名改 `public static void onPlay(AbstractCard c, AbstractMonster m)`（m = useCard 的目标，巨浪条件判定要用；其他逻辑不受影响）。
CombatStatsPatch.OnPlay.Prefix（Task 2 Step 7 已写）里的调用同步改 `CardStats.onPlay(c, m);`。

在 onPlay 内 Task 2 Step 1 代码块之后、`// 水位...` 注释之前插入：

```java
        // v2：条件牌触发率（注册表外的牌不计数；判定用 useCard 的目标 m，与牌 use() 门槛同刻）
        CardConditionRegistry.Condition cond = CardConditionRegistry.lookup(c.cardID);
        if (cond != null) {
            ins.conditionTotal++;
            if (cond.met(c, m)) {
                ins.conditionMet++;
            }
        }
```

（近似说明：巨浪的返能判定实际发生在 use() 尾部 atbLambda（伤害动作结算后），此处用打出瞬间判定——打出瞬间与 lambda 执行之间目标获得/失去易伤是罕见边界，误差可接受，风险节已记。）

- [ ] **Step 3: 导出辅助方法 appendCardGroupStats**（在 appendCardBlock :1693 附近、导出区插入；act 为 null = 全局）

```java
    /** v2 单卡资源收支/手牌表现/条件触发率行。act=null 表示全局（滞留/弃置/条件仅全局显示）。 */
    private static void appendCardGroupStats(StringBuilder sb, List<CardInstance> list, Integer act) {
        long spent = 0, gained = 0, drawn = 0;
        long dwellSum = 0, discarded = 0;
        int condTotal = 0, condMet = 0;
        long plays = 0;
        for (CardInstance i : list) {
            plays += (act == null) ? totalPlays(i) : i.plays[act];
            spent += (act == null) ? total(i.energySpent) : i.energySpent[act];
            gained += (act == null) ? total(i.energyGained) : i.energyGained[act];
            drawn += (act == null) ? total(i.drawn) : i.drawn[act];
            dwellSum += i.handDwellSum;
            discarded += i.handDiscarded;
            condTotal += i.conditionTotal;
            condMet += i.conditionMet;
        }
        // 资源收支
        sb.append("- **资源收支**: 净能量 ").append(gained - spent)
                .append("（付 ").append(spent).append(" / 得 ").append(gained).append("）");
        sb.append("  净过牌 ").append(plays <= 0 ? "-" : fmt((double) drawn / plays - 1))
                .append("（抽 ").append(drawn).append(" / 打出 ").append(plays).append("）\n");
        // 手牌表现（整局口径）
        sb.append("- **手牌表现**: 平均滞留 ").append(plays <= 0 ? "-" : fmt((double) dwellSum / plays))
                .append(" 回合  打出率 ").append((plays + discarded) <= 0 ? "-" : fmt(100.0 * plays / (plays + discarded))).append("%\n");
        // 条件触发率（仅注册表内的牌）
        if (condTotal > 0) {
            CardConditionRegistry.Condition cond = CardConditionRegistry.lookup(list.get(0).cardID);
            sb.append("- **条件触发率**: ").append(condMet).append("/").append(condTotal)
                    .append("（").append(cond != null ? cond.label() : "?").append("）\n");
        }
    }
```

- [ ] **Step 4: 卡牌分组导出调用**（两处分组节内，现有行之后调用：每幕分组 :1534-1535 `敏捷贡献` 行之后、全局分组 :1576-1577 `敏捷` 行之后）

每幕分组处：
```java
                appendCardGroupStats(sb, list, act);
```
全局分组处：
```java
                appendCardGroupStats(sb, list, null);
```

- [ ] **Step 5: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`


---

### Task 5: 模块 3 战斗节奏（每场明细行 + 分幕聚合节）

**Files:**
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（export() 战斗统计节 :1429-1470 重写 + 新聚合节）

**Interfaces:**
- Consumes: Task 1 Combat 字段（dmgT1/dmgT2/blockGained/blockCleared/overkill/damageDealt/stunTurns/stanceSwitches/dyingSaves）
- Produces: 无新方法；export() 内新增私有辅助 `pct(long num, long den)`（分母 0 返回 "-"）

**公式**（spec §3 模块 3）：
- 启动税占比 = (dmgT1+dmgT2)/damageTaken
- 战损 stddev/max：同幕同类型全部战斗 netHpLoss()
- 溢空格挡率 = blockCleared/blockGained
- Overkill 率 = Σoverkill/ΣdamageDealt

- [ ] **Step 1: export() 战斗统计节重写**（:1429-1470 整段替换；保留原有幕/类型聚合行，加每场明细行，节尾追加分幕聚合行）

```java
        // ---- 战斗统计（按幕；每场明细 + 类型聚合 + 节奏指标）----
        String[] roomNames = { "小怪", "精英", "BOSS" };
        for (int act = 0; act < ACTS; act++) {
            List<Combat> actCombats = new ArrayList<>();
            for (Combat c : combats) {
                if (c.act == act) {
                    actCombats.add(c);
                }
            }
            if (actCombats.isEmpty()) {
                continue;
            }
            double totalTurns = 0;
            for (Combat c : actCombats) {
                totalTurns += c.rounds();
            }
            sb.append("## 战斗统计-第").append(act + 1).append("幕\n\n");
            sb.append("**战斗数**: ").append(actCombats.size())
                    .append("  **平均回合**: ").append(fmt(totalTurns / actCombats.size())).append("\n\n");
            for (int rt = 0; rt < 3; rt++) {
                List<Combat> rc = new ArrayList<>();
                for (Combat c : actCombats) {
                    if (c.roomType == rt) {
                        rc.add(c);
                    }
                }
                if (rc.isEmpty()) {
                    continue;
                }
                double net = 0, taken = 0, turns = 0;
                long blkG = 0, blkC = 0, ok = 0, dealt = 0;
                for (Combat c : rc) {
                    net += c.netHpLoss();
                    taken += c.damageTaken;
                    turns += c.rounds();
                    blkG += c.blockGained;
                    blkC += c.blockCleared;
                    ok += c.overkill;
                    dealt += c.damageDealt;
                }
                sb.append("- **").append(roomNames[rt]).append("**: 场数 ").append(rc.size())
                        .append("  平均净HP损失 ").append(fmt(net / rc.size()))
                        .append("  平均受伤 ").append(fmt(taken / rc.size()))
                        .append("  平均回合 ").append(fmt(turns / rc.size())).append('\n');
                // v2 节奏聚合：启动税均值 / 战损stddev / 最大单场 / 溢空格挡率 / Overkill率
                double startup = 0;
                for (Combat c : rc) {
                    startup += (c.damageTaken <= 0) ? 0
                            : 100.0 * (c.dmgT1 + c.dmgT2) / c.damageTaken;
                }
                sb.append("  - 启动税T1-T2承伤占比 均值 ").append(fmt(startup / rc.size()))
                        .append("%  战损stddev ").append(fmt(stddev(rc)))
                        .append("  最大单场 ").append(maxNetLoss(rc))
                        .append("  溢空格挡率 ").append(pct(blkC, blkG))
                        .append("  Overkill率 ").append(pct(ok, dealt)).append('\n');
            }
            // v2 每场明细行
            sb.append("**每场明细**（启动期=启动税T1-T2承伤占比）\n\n");
            int idx = 0;
            for (Combat c : actCombats) {
                idx++;
                sb.append("- 场").append(idx).append(" ").append(roomNames[c.roomType])
                        .append(" 回合").append(c.rounds())
                        .append(" 净损").append(c.netHpLoss())
                        .append(" 受伤").append(c.damageTaken)
                        .append(" 启动期").append(pct(c.dmgT1 + c.dmgT2, c.damageTaken))
                        .append(" 溢空格挡").append(pct(c.blockCleared, c.blockGained))
                        .append(" Overkill").append(c.overkill)
                        .append(" 眩晕回合").append(c.stunTurns)
                        .append(" 切换身份").append(c.stanceSwitches)
                        .append(" 免死").append(c.dyingSaves).append('\n');
            }
            sb.append('\n');
        }
```

- [ ] **Step 2: 新增三个私有辅助**（ave/fmt :1791-1797 附近）

```java
    /** 百分比（分母 0 → "-"）。 */
    private static String pct(long num, long den) {
        return den <= 0 ? "-" : fmt(100.0 * num / den) + "%";
    }

    /** 战损标准差（总体 stddev，n=1 时为 0）。 */
    private static double stddev(List<Combat> cs) {
        if (cs.size() <= 1) {
            return 0;
        }
        double mean = 0;
        for (Combat c : cs) {
            mean += c.netHpLoss();
        }
        mean /= cs.size();
        double sq = 0;
        for (Combat c : cs) {
            double d = c.netHpLoss() - mean;
            sq += d * d;
        }
        return Math.sqrt(sq / cs.size());
    }

    private static int maxNetLoss(List<Combat> cs) {
        int m = 0;
        for (Combat c : cs) {
            m = Math.max(m, c.netHpLoss());
        }
        return m;
    }
```

- [ ] **Step 3: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`


---

### Task 6: 模块 4 Power 深度账本（重构原 BUFF/DEBUFF 台账节）

**Files:**
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（export() 台账节 :1630-1676 重写）

**Interfaces:**
- Consumes: Task 1/3 PowerLedger 字段（name/stacksAppliedAct/coverageTurns/hitValueSum）+ 旧字段
- Produces: 私有辅助 `powerEquivLine(PowerLedger L, long strengthContribTotal)`

**等效价值公式**（spec §3 模块 4，全部只读已记账数据，不新建钩子）：
- Vulnerable（怪物 debuff）：增伤贡献 ≈ damageDealtToTarget/3（1.5x 中 0.5/1.5）
- Strength（玩家 buff）：增伤贡献 = 全部卡的 strengthContrib 汇总（授予比例分摊法，Task 1 已记）
- Weakened（怪物 debuff）：减战损等效 = blockSaved（Task 2 Step 4 已按 loss/3 精确累计）
- Shackled（玩家 debuff）：输出减少等效 = hitValueSum
- IntangiblePlayer（玩家 buff）：减战损等效 = blockSaved（Task 2 Step 4 已按 output-loss 累计）
- Energized（玩家 buff）：供能等效 = ΣcoverageTurns（激活回合 ×1 能量）
- 抽牌等效：当前无此类 power，列恒 0（YAGNI 不建归属）
- 其余 power：无等效行

- [ ] **Step 1: export() 台账节重写**（:1630-1676 整段替换为；节名改 Power深度账本）

```java
        // ---- Power深度账本（v2：分幕场均层数 / 覆盖率 / 等效价值）----
        if (!ledgers.isEmpty()) {
            List<PowerLedger> list = new ArrayList<>(ledgers.values());
            list.sort((a, b) -> Long.compare(
                    b.damageDuring + b.damageTakenDuring + b.damageDealtToTarget + b.blockSaved,
                    a.damageDuring + a.damageTakenDuring + a.damageDealtToTarget + a.blockSaved));
            // 全局力量贡献汇总（Strength 行等效用）
            long strengthContribTotal = 0;
            for (CardInstance i : instances.values()) {
                strengthContribTotal += total(i.strengthContrib);
            }
            // 每幕玩家回合总数（覆盖率分母）
            int[] actTurns = new int[ACTS];
            for (Combat c : combats) {
                actTurns[c.act] += c.rounds();
            }
            // 每幕战斗场数（场均层数分母）
            int[] actBattles = new int[ACTS];
            for (Combat c : combats) {
                actBattles[c.act]++;
            }
            sb.append("## Power深度账本\n\n");
            for (PowerLedger L : list) {
                if (L.stacksApplied <= 0 && L.damageDuring <= 0 && L.blockDuring <= 0
                        && L.damageTakenDuring <= 0 && L.damageDealtToTarget <= 0 && L.blockSaved <= 0
                        && L.hitValueSum <= 0) {
                    continue;
                }
                sb.append("### **").append(L.name != null ? L.name : L.powerId)
                        .append("** ").append(L.isDebuff ? "（debuff）" : "（buff）").append("\n\n");
                // 分幕场均层数
                StringBuilder actLine = new StringBuilder("- **分幕场均层数**: ");
                for (int a = 0; a < ACTS; a++) {
                    if (a > 0) {
                        actLine.append("  ");
                    }
                    actLine.append(actBattles[a] <= 0 ? "-" : fmt((double) L.stacksAppliedAct[a] / actBattles[a]));
                }
                sb.append(actLine).append("（总授予 ").append(L.stacksApplied).append(" 层）\n");
                // 激活回合覆盖率
                StringBuilder covLine = new StringBuilder("- **激活回合覆盖率**: ");
                for (int a = 0; a < ACTS; a++) {
                    if (a > 0) {
                        covLine.append("  ");
                    }
                    covLine.append(actTurns[a] <= 0 ? "-" : fmt(100.0 * L.coverageTurns[a] / actTurns[a])).append("%");
                }
                sb.append(covLine).append("\n");
                // 期间统计（保留 v1 行）
                List<Map.Entry<String, Long>> gr = new ArrayList<>(L.granters.entrySet());
                gr.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
                if (!gr.isEmpty()) {
                    StringBuilder line = new StringBuilder("- **来源牌**: ");
                    int shown = 0;
                    for (Map.Entry<String, Long> g : gr) {
                        if (shown++ > 0) {
                            line.append(" / ");
                        }
                        line.append(g.getKey()).append("(").append(g.getValue()).append("层)");
                        if (shown >= 3) {
                            break;
                        }
                    }
                    if (gr.size() > 3) {
                        line.append(" / 其他");
                    }
                    sb.append(line).append('\n');
                }
                sb.append("- **期间造成伤害**: ").append(L.damageDuring)
                        .append("  **期间获得格挡**: ").append(L.blockDuring).append('\n');
                if (L.damageTakenDuring > 0) {
                    sb.append("- **期间(玩家)受到伤害**: ").append(L.damageTakenDuring).append('\n');
                }
                if (L.damageDealtToTarget > 0) {
                    sb.append("- **对带此debuff目标造成伤害**: ").append(L.damageDealtToTarget).append('\n');
                }
                if (L.blockSaved > 0) {
                    sb.append("- **减战损等效(约)**: ").append(L.blockSaved).append('\n');
                }
                if (L.hitValueSum > 0) {
                    sb.append("- **命中少打(负力量等效)**: ").append(L.hitValueSum).append('\n');
                }
                // 等效价值（通用公式 + 招牌）
                String equiv = powerEquivLine(L, strengthContribTotal);
                if (equiv != null) {
                    sb.append("- **等效价值**: ").append(equiv).append('\n');
                }
                sb.append('\n');
            }
        }
```

- [ ] **Step 2: 新增 powerEquivLine**（stddev 辅助附近）

```java
    /** Power 等效价值（按 power ID 选公式；返回 null = 无等效行）。 */
    private static String powerEquivLine(PowerLedger L, long strengthContribTotal) {
        switch (L.powerId) {
            case "Vulnerable":
                return L.damageDealtToTarget <= 0 ? null
                        : "增伤贡献 ≈ " + (L.damageDealtToTarget / 3) + "（1.5x 中 1/3）";
            case "Strength":
                return strengthContribTotal <= 0 ? null
                        : "增伤贡献 ≈ " + strengthContribTotal + "（按授予比例分摊到各卡）";
            case "Weakened":
                return L.blockSaved <= 0 ? null
                        : "减战损等效 ≈ " + L.blockSaved + "（25% 少受伤害）";
            case "Shackled":
                return L.hitValueSum <= 0 ? null
                        : "输出减少等效 ≈ " + L.hitValueSum + "（负力量少打）";
            case "IntangiblePlayer":
                return L.blockSaved <= 0 ? null
                        : "减战损等效 ≈ " + L.blockSaved + "（伤害压到 1）";
            case "Energized": {
                long turns = 0;
                for (int t : L.coverageTurns) {
                    turns += t;
                }
                return turns <= 0 ? null : "供能等效 ≈ " + turns + " 能量（激活回合×1）";
            }
            default:
                return null;
        }
    }
```

- [ ] **Step 3: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`


---

### Task 7: 模块 2 卡包综合审计 + 模块 5 DoubleSS 机制收支（含 4 处内联调用点）

**Files:**
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（export() 新增两节 + 归包辅助；诊断节加两个桶）
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/action/common/UpdateFiendStanceDescriptions.java`（:32 `this.isDone = true;` 之前加一行）
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/action/common/UpdateManagerStanceDescriptions.java`（:31 `this.isDone = true;` 之前加一行）
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/power/SinsPower.java`（stackPower :47-56、reducePower :58-67、onInitialApplication :69-71）
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/power/DyingPower.java`（onPlayerDeath :150-177 两条成功路径）

**Interfaces:**
- Consumes: Task 1 全部整局级字段 + Combat 机制字段；`SS.modcore.modcore.cardParentMap`（cardID→包ID）；`AbstractCardEnum.Sins/Manager`
- Produces: 私有辅助 `cardPackageOf(String cardID, AbstractCard sample)`、`dicePackageOf(String orbId)`、`packageNameOf(String packageId)`

**归包规则**（spec §3 模块 2，优先级从上到下）：Sins tag → 七宗罪；cardParentMap 命中 → 对应包；其余 → 本体。

- [ ] **Step 1: 两个 stance action 内联**

UpdateFiendStanceDescriptions.java：早退分支（`:19-22` `if (p.getPower("Double:FiendStance") != null) { this.isDone = true; return; }`）之后、方法末尾 `this.isDone = true;` 之前插入：

```java
        SS.stats.CardStats.onStanceSwitch("Fiend");
```

UpdateManagerStanceDescriptions.java 同理（早退分支 `:19-22` 之后）：

```java
        SS.stats.CardStats.onStanceSwitch("Manager");
```

（早退之后 = 确认进入该身份，一次真实切换恰好 +1；ChangeStanceAction 同时排两个 action 时另一个被早退挡掉，不重复计。）

- [ ] **Step 2: SinsPower 内联三个方法**（import `SS.stats.CardStats`）

stackPower（:47-56）改为：

```java
    public void stackPower(int stackAmount) {
        if (this.lock)
            return;
        int prev = this.amount;
        this.amount += stackAmount;
        this.amount = Math.min(this.amount, max_sin);
        this.amount = Math.max(this.amount, min_sin);
        if (this.amount == max_sin || this.amount == min_sin) {
            this.lock = true;
        }
        CardStats.onKarmaChange(this.amount - prev);
    }
```

reducePower（:58-67）同型（开头记 `int prev = this.amount;`，末行 `CardStats.onKarmaChange(this.amount - prev);`）。

onInitialApplication（:69-71）改为：

```java
    public void onInitialApplication() {
        CardStats.onKarmaApply(this.amount);
    }
```

（已核实 GainVirtueAction 两分支都走 ApplyPowerAction：已存在→stackPower、首次→onInitialApplication（powers.add 之后调用）→ 罪孽/美德全路径覆盖。onKarmaApply 内部调 onKarmaChange(amount)，prev = now-amount = 0，正确。）

- [ ] **Step 3: DyingPower 内联**（import `SS.stats.CardStats`）

在 onPlayerDeath 两条成功路径的 `return false;` 之前各加一行：斗篷保护期路径（:157-162 的 `return false;` at :161 之前）、正常保命路径（:165-173 的 `return false;` at :173 之前）：

```java
            CardStats.onDyingSaved();
```

（:151 ResurrectionPower 的 `return true`（真死）与 :176 `return true` 不加。）


- [ ] **Step 4: 归包辅助方法**（放在导出辅助区，pct/stddev 附近）

```java
    /** 包 ID → 短名。 */
    private static String packageNameOf(String packageId) {
        if (packageId == null) {
            return null;
        }
        if (packageId.contains("HaoPackage")) {
            return "Hao";
        }
        if (packageId.contains("LostPackage")) {
            return "Lost";
        }
        if (packageId.contains("ShockPackage")) {
            return "Shock";
        }
        if (packageId.contains("C6H14Package")) {
            return "C6H14";
        }
        return "本体";
    }

    /** 牌归包：Sins tag → 七宗罪；cardParentMap 命中 → 对应包；其余 → 本体。sample = 该 ID 的一张活牌（读 tag，可 null）。 */
    private static String cardPackageOf(String cardID, AbstractCard sample) {
        if (sample != null && sample.hasTag(AbstractCardEnum.Sins)) {
            return "七宗罪";
        }
        String pkg = SS.modcore.modcore.cardParentMap.get(cardID);
        return pkg != null ? packageNameOf(pkg) : "本体";
    }

    /** 骰子归包：Hao 骰 → Hao，其余 → 本体。 */
    private static String dicePackageOf(String orbId) {
        if (orbId != null && (orbId.equals("Double:AttackHaoDice") || orbId.equals("Double:DefendHaoDice"))) {
            return "Hao";
        }
        return "本体";
    }

    /** 取该 cardID 的一张活牌（读 tag 用；masterDeck 规模小，线性查找可接受）。 */
    private static AbstractCard liveCardOf(String cardID) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null) {
            return null;
        }
        for (AbstractCard mc : AbstractDungeon.player.masterDeck.group) {
            if (mc.cardID.equals(cardID)) {
                return mc;
            }
        }
        return null;
    }
```

- [ ] **Step 5: 卡包综合审计节**（export() 内，全局卡牌汇总之后、骰子统计之前插入）

```java
        // ---- 卡包综合审计（v2 模块 2）----
        {
            String[] pkgs = { "七宗罪", "Hao", "Lost", "Shock", "C6H14", "本体" };
            Combat lastCombat = combats.isEmpty() ? null : combats.get(combats.size() - 1);
            Map<String, Integer> deckByPkg = new HashMap<>();
            int deckTotal = 0;
            if (lastCombat != null) {
                for (UUID u : lastCombat.deckAtStart) {
                    CardInstance ci = instances.get(u);
                    if (ci == null) {
                        continue;
                    }
                    deckByPkg.merge(cardPackageOf(ci.cardID, liveCardOf(ci.cardID)), 1, Integer::sum);
                    deckTotal++;
                }
            }
            Map<String, long[]> agg = new HashMap<>(); // [dmg, block, plays, energyNet, drawn]
            for (CardInstance ci : instances.values()) {
                long[] a = agg.computeIfAbsent(cardPackageOf(ci.cardID, liveCardOf(ci.cardID)),
                        k -> new long[5]);
                a[0] += totalDmg(ci);
                a[1] += totalBlock(ci);
                a[2] += totalPlays(ci);
                a[3] += total(ci.energyGained) - total(ci.energySpent);
                a[4] += total(ci.drawn);
            }
            Map<String, Long> diceByPkg = new HashMap<>();
            for (DiceStat d : diceStats.values()) {
                diceByPkg.merge(dicePackageOf(d.orbId), d.dmg, Long::sum);
            }
            long allCardDmg = 0, allCardBlock = 0, allPlays = 0, allDiceDmg = 0;
            for (long[] a : agg.values()) {
                allCardDmg += a[0];
                allCardBlock += a[1];
                allPlays += a[2];
            }
            for (Long v : diceByPkg.values()) {
                allDiceDmg += v;
            }
            long totalDmgAll = allCardDmg + allDiceDmg;
            sb.append("## 卡包综合审计（全局）\n\n");
            sb.append("> 伤害占比分母 = 卡直伤 + 骰伤害；稀释件 = 打出占比 < 牌库占比×0.5\n\n");
            for (String pkg : pkgs) {
                long[] a = agg.getOrDefault(pkg, new long[5]);
                long dDmg = diceByPkg.getOrDefault(pkg, 0L);
                int deckN = deckByPkg.getOrDefault(pkg, 0);
                double deckShare = deckTotal <= 0 ? 0 : 100.0 * deckN / deckTotal;
                double playShare = allPlays <= 0 ? 0 : 100.0 * a[2] / allPlays;
                StringBuilder line = new StringBuilder("- **").append(pkg).append("**: ");
                line.append("伤害占比 ").append(pct(a[0] + dDmg, totalDmgAll))
                        .append("（卡 ").append(a[0]).append(" + 骰 ").append(dDmg).append("）")
                        .append("  格挡占比 ").append(pct(a[1], allCardBlock))
                        .append("  净能量 ").append(a[3])
                        .append("  净过牌 ").append(a[2] <= 0 ? "-" : fmt((double) a[4] / a[2] - 1))
                        .append("  牌库 ").append(deckN).append("张(").append(fmt(deckShare)).append("%)")
                        .append("  打出 ").append(a[2]).append("次(").append(fmt(playShare)).append("%)");
                if (deckN > 0 && playShare < deckShare * 0.5) {
                    line.append("  ⚠卡组稀释件");
                }
                sb.append(line).append('\n');
            }
            sb.append("- **机制交互**: 骰伤害占全场 ").append(pct(allDiceDmg, totalDmgAll)).append('\n');
        }
```


- [ ] **Step 6: DoubleSS机制收支节**（Power深度账本节之后插入；`roomNames` 在该节之前已定义于战斗统计节，作用域是 export() 方法体内，可直接用）

```java
        // ---- DoubleSS机制收支（v2 模块 5）----
        sb.append("## DoubleSS机制收支（全局）\n\n");
        sb.append("**因果账本（罪孽/美德）**\n\n");
        for (Combat c : combats) {
            if (c.sinPeak > 0 || c.virtuePeak > 0 || c.maxHpStart != c.maxHpEnd) {
                sb.append("- 第").append(c.act + 1).append("幕-").append(roomNames[c.roomType]).append(": 罪孽峰值 ")
                        .append(c.sinPeak).append("  美德峰值 ").append(c.virtuePeak)
                        .append("  最大HP变动 ").append(c.maxHpEnd - c.maxHpStart).append('\n');
            }
        }
        int maxHpStart0 = combats.isEmpty() ? 0 : combats.get(0).maxHpStart;
        int maxHpEndLast = 0;
        for (Combat c : combats) {
            if (c.maxHpEnd > 0) {
                maxHpEndLast = c.maxHpEnd;
            }
        }
        sb.append("- **整局**: 50罪孽触发 ×").append(karmaPlus50Hits)
                .append("  50美德触发 ×").append(karmaMinus50Hits)
                .append("  净最大HP变动 ").append(maxHpEndLast - maxHpStart0).append('\n');
        sb.append("\n**奄息依赖度**\n\n");
        sb.append("- **整局免死**: ").append(dyingSaveTotal).append(" 次");
        if (!dyingSaveRooms.isEmpty()) {
            sb.append("（关卡: ").append(String.join(", ", dyingSaveRooms)).append("）");
        }
        sb.append('\n');
        for (Combat c : combats) {
            if (c.lowDyingTurns > 0 || c.dyingSaves > 0) {
                sb.append("- 第").append(c.act + 1).append("幕-").append(roomNames[c.roomType]).append(": 奄息<=2层回合 ")
                        .append(c.lowDyingTurns).append("  免死 ").append(c.dyingSaves).append('\n');
            }
        }
        sb.append("\n**身份对位契合度**\n\n");
        sb.append("- **平均切换身份次数/场**: ")
                .append(combats.isEmpty() ? "-" : fmt((double) stanceSwitchTotal / combats.size())).append('\n');
        for (Combat c : combats) {
            if (c.fiendPlays > 0 || c.managerPlays > 0) {
                sb.append("- 第").append(c.act + 1).append("幕-").append(roomNames[c.roomType]).append(": ");
                if (c.fiendPlays > 0) {
                    sb.append("恶魔之焰契合 ").append(pct(c.fiendSinPlays, c.fiendPlays))
                            .append("（七宗罪 ").append(c.fiendSinPlays).append("/").append(c.fiendPlays).append("）");
                }
                if (c.managerPlays > 0) {
                    sb.append("  群主契合 ").append(pct(c.managerHaoPlays, c.managerPlays))
                            .append("（群友卡 ").append(c.managerHaoPlays).append("/").append(c.managerPlays).append("）");
                }
                sb.append('\n');
            }
        }
```

- [ ] **Step 7: 诊断节加两个桶**（:1686 `卡牌栈强制弹出` 行之后）

```java
        sb.append("- 无归属能量增加(无栈顶且本回合未出牌): ").append(unattributedEnergyGain).append('\n');
        sb.append("- 无归属抽牌(开局首抽/无栈顶): ").append(unattributedDraws).append('\n');
```

- [ ] **Step 8: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`


---

### Task 8: 导出重构收尾（删按实例节 / ID→牌名 / 快照 v2）

**Files:**
- Modify: `D:/StSmod/DoubleSS/src/main/java/SS/stats/CardStats.java`（export() 删按实例节 :1472-1494 + 节改名 :1509/:1549 + 牌名 :1528/:1570；删 appendCardBlock :1693-1709、displayName :1780-1789；saveSnapshot :365-491、restoreSnapshot :494-642 加 v2）

**Interfaces:**
- Consumes: Task 1-7 全部字段
- Produces: 快照 JSON 加 `"v":2` + 新字段块；旧 v1 快照全字段缺失容错

- [ ] **Step 1: 删除「按实例」导出节**（export() 内 :1472-1494 整段：`// ---- 卡牌统计（按幕、按实例） ----` 起，到该 for 循环的闭合 `}`；结束标志 = 该节末尾 `sb.append("> 敏捷同理。骰子伤害/格挡按 sources 均分到来源牌并计入本行。持恒牌不吃力敏(豁免)。\n\n");` 之后的 `}`。同时删除 appendCardBlock（:1693-1709）与 displayName（:1780-1789）——删节后无调用者）

- [ ] **Step 2: 两个节标题改名**

`## 卡牌汇总-第X幕-按卡ID分组`（:1509）→ `## 卡牌统计-第X幕`
`## 卡牌汇总-全局-按卡ID分组`（:1549）→ `## 卡牌汇总-全局`

- [ ] **Step 3: 卡名显示（ID→牌名）**（两处分组节标题行 :1528 与 :1570，原为 `sb.append("### **").append(baseId).append(...)`）

改为：

```java
                String shownName = list.get(0).name;
                sb.append("### **").append(shownName).append(up ? "(升级)" : "(未升级)").append("**\n\n");
```

（CardInstance.name = c.name（构造器 :119），即游戏内牌名；分组键仍是 cardID+升级态，同 ID 多张已合并一行。）

- [ ] **Step 4: 快照 v2 序列化**

saveSnapshot 的 `JsonObject root = new JsonObject();`（:374）之后加 `root.addProperty("v", 2);`
CardInstance 块（`o.addProperty("ci", ci.copyIndex);` :383 之后）加：

```java
                o.add("eSpent", longArr(ci.energySpent));
                o.add("eGained", longArr(ci.energyGained));
                o.add("drawn", longArr(ci.drawn));
                o.addProperty("dwell", ci.handDwellSum);
                o.addProperty("discarded", ci.handDiscarded);
                o.addProperty("condTotal", ci.conditionTotal);
                o.addProperty("condMet", ci.conditionMet);
```

Combat 块（`o.addProperty("fin", c.finalized);` :410 之后）加：

```java
                o.addProperty("dmgT1", c.dmgT1);
                o.addProperty("dmgT2", c.dmgT2);
                o.addProperty("blkG", c.blockGained);
                o.addProperty("blkC", c.blockCleared);
                o.addProperty("ok", c.overkill);
                o.addProperty("dealt", c.damageDealt);
                o.addProperty("stun", c.stunTurns);
                o.addProperty("stSw", c.stanceSwitches);
                o.addProperty("fTurns", c.fiendTurns);
                o.addProperty("mTurns", c.managerTurns);
                o.addProperty("fSin", c.fiendSinPlays);
                o.addProperty("fP", c.fiendPlays);
                o.addProperty("mHao", c.managerHaoPlays);
                o.addProperty("mP", c.managerPlays);
                o.addProperty("lowDy", c.lowDyingTurns);
                o.addProperty("sinPk", c.sinPeak);
                o.addProperty("virPk", c.virtuePeak);
                o.addProperty("mh0", c.maxHpStart);
                o.addProperty("mh1", c.maxHpEnd);
                o.addProperty("dySv", c.dyingSaves);
```

PowerLedger 块（`o.addProperty("debuff", L.isDebuff);` :429 之后）加：

```java
                if (L.name != null) {
                    o.addProperty("name", L.name);
                }
                o.add("stkAct", intArr(L.stacksAppliedAct));
                o.add("covTurns", intArr(L.coverageTurns));
                o.addProperty("hitVal", L.hitValueSum);
```

整局级（`root.add("dexG", dexG);` :482 之后）加：

```java
            JsonObject v2 = new JsonObject();
            v2.addProperty("kPlus", karmaPlus50Hits);
            v2.addProperty("kMinus", karmaMinus50Hits);
            v2.addProperty("dyTotal", dyingSaveTotal);
            v2.addProperty("stTotal", stanceSwitchTotal);
            JsonArray dyRooms = new JsonArray();
            for (String s : dyingSaveRooms) {
                dyRooms.add(s);
            }
            v2.add("dyRooms", dyRooms);
            v2.addProperty("uEGain", unattributedEnergyGain);
            v2.addProperty("uDraw", unattributedDraws);
            root.add("v2", v2);
```


- [ ] **Step 5: 快照 v2 反序列化（旧 v1 缺失容错）**

restoreSnapshot 的 CardInstance 块（`ci.dexterityContrib = longArr(o.getAsJsonArray("dexC"));` :539 之后）加：

```java
                    if (o.has("eSpent")) {
                        ci.energySpent = longArr(o.getAsJsonArray("eSpent"));
                        ci.energyGained = longArr(o.getAsJsonArray("eGained"));
                        ci.drawn = longArr(o.getAsJsonArray("drawn"));
                        ci.handDwellSum = o.get("dwell").getAsLong();
                        ci.handDiscarded = o.get("discarded").getAsLong();
                        ci.conditionTotal = o.get("condTotal").getAsInt();
                        ci.conditionMet = o.get("condMet").getAsInt();
                    }
```

Combat 块（`c.finalized = o.get("fin").getAsBoolean();` :557 之后）加：

```java
                    if (o.has("dmgT1")) {
                        c.dmgT1 = o.get("dmgT1").getAsLong();
                        c.dmgT2 = o.get("dmgT2").getAsLong();
                        c.blockGained = o.get("blkG").getAsLong();
                        c.blockCleared = o.get("blkC").getAsLong();
                        c.overkill = o.get("ok").getAsLong();
                        c.damageDealt = o.get("dealt").getAsLong();
                        c.stunTurns = o.get("stun").getAsInt();
                        c.stanceSwitches = o.get("stSw").getAsInt();
                        c.fiendTurns = o.get("fTurns").getAsInt();
                        c.managerTurns = o.get("mTurns").getAsInt();
                        c.fiendSinPlays = o.get("fSin").getAsInt();
                        c.fiendPlays = o.get("fP").getAsInt();
                        c.managerHaoPlays = o.get("mHao").getAsInt();
                        c.managerPlays = o.get("mP").getAsInt();
                        c.lowDyingTurns = o.get("lowDy").getAsInt();
                        c.sinPeak = o.get("sinPk").getAsInt();
                        c.virtuePeak = o.get("virPk").getAsInt();
                        c.maxHpStart = o.get("mh0").getAsInt();
                        c.maxHpEnd = o.get("mh1").getAsInt();
                        c.dyingSaves = o.get("dySv").getAsInt();
                    }
```

PowerLedger 块（`L.blockSaved = o.get("saved").getAsLong();` :584 之后）加：

```java
                    if (o.has("stkAct")) {
                        L.name = o.has("name") ? o.get("name").getAsString() : L.powerId;
                        L.stacksAppliedAct = intArr(o.getAsJsonArray("stkAct"));
                        L.coverageTurns = intArr(o.getAsJsonArray("covTurns"));
                        L.hitValueSum = o.get("hitVal").getAsLong();
                    }
```

整局级（dexG 块结束之后、`remapInstancesToDeck();` :636 之前）加：

```java
            JsonObject v2 = root.getAsJsonObject("v2");
            if (v2 != null) {
                karmaPlus50Hits = v2.get("kPlus").getAsLong();
                karmaMinus50Hits = v2.get("kMinus").getAsLong();
                dyingSaveTotal = v2.get("dyTotal").getAsLong();
                stanceSwitchTotal = v2.get("stTotal").getAsInt();
                JsonArray dyRooms = v2.getAsJsonArray("dyRooms");
                if (dyRooms != null) {
                    for (JsonElement de : dyRooms) {
                        dyingSaveRooms.add(de.getAsString());
                    }
                }
                unattributedEnergyGain = v2.get("uEGain").getAsLong();
                unattributedDraws = v2.get("uDraw").getAsLong();
            }
```

（v1 旧快照无 `v2` 块、无各新键 → 全字段保持默认 0，不报错。）

- [ ] **Step 6: 构建门**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean compile 2>&1 | tr -d '\000' | tail -20`
Expected: `BUILD SUCCESS`（删 appendCardBlock/displayName 后若有残留调用，编译器直接报错 → 按报错清干净）

---

### Task 9: 构建部署 + 实机验证

**Files:**
- 无代码改动；产出 = 部署后的 jar + `analysis/*.md` 一份真实统计

- [ ] **Step 1: 打包部署**

Run: `cd /d/StSmod/DoubleSS && mvn -o -B clean package 2>&1 | tr -d '\000' | tail -25`
Expected: `BUILD SUCCESS` + antrun 的 2 条 copy 输出（mods 与 DoubleSS/content 两处）
Verify: `ls -l "/e/SteamLibrary/steamapps/common/SlayTheSpire/mods/DoubleSS.jar" "/e/SteamLibrary/steamapps/common/SlayTheSpire/DoubleSS/content/DoubleSS.jar"` 两文件大小一致且为最新时间戳

- [ ] **Step 2: 启动游戏核对钩子加载**

启动 StS（带 DoubleSS），检查 `E:/SteamLibrary/steamapps/common/SlayTheSpire/sendToDevs/mts_process_launch.log`：
- 无 `Failed to apply patch` / `SpirePatch` 相关异常
- 出现 mod 初始化日志
若报 `AbstractPlayer.draw` 或 `AbstractRoom.endTurn` 相关 patch 失败 → 回到 CombatStatsPatch 检查 `paramtypez` / 方法签名

- [ ] **Step 3: 打一局（验证 spec §8 清单）**

用户操作：DoubleSS 角色（或原版角色各一局）打完整一局（死亡/胜利/放弃任一结局）。
检查最新 `analysis/*.md`：

1. 新节齐全：战斗节奏-分幕聚合 / 卡包综合审计 / Power深度账本 / DoubleSS机制收支；**「按实例」节消失**
2. 所有卡行显示**牌名**（不再是 `Double:Xxx` ID）
3. 巨浪吃易伤 → 净能量 > 0；KernelMapping → 净能量 = +magicNumber − 费用
4. 三圣颂美德不足时打出 → 条件触发率 total+1、met 不变
5. 切换身份一次 → 对应场次「切换身份」+1（来回切两次 = 2）
6. 原版职业一局 → 统计开关关时无文件写出；无异常日志
7. SL 读档后新字段不丢（打一场 → SL → 看后续导出新字段沿用）
8. 诊断节两个新桶数值合理（开局首抽计入无归属抽牌属预期）

- [ ] **Step 4: 记录验证结论**

在 task 列表标记 #26 完成；把验证结果（哪些项通过、哪些偏差）写入 `analysis` 导出旁注或回报用户。已知可接受偏差（spec §9）：抽牌/返能启发式归属、溢空格挡率在 Barricade/Blur/Calipers 下偏高、条件判定用打出瞬间而非 lambda 执行瞬间。

---

## 自检（Self-Review）

**1. Spec 覆盖**：五模块逐条对位——模块 1（Task 4）/ 模块 2（Task 7 Step 4-5）/ 模块 3（Task 5）/ 模块 4（Task 3 + 6）/ 模块 5（Task 7 Step 1-3、6）；硬性要求「删按实例节 + 按 ID 统整 + 牌名」= Task 8 Step 1-3；快照 v2 = Task 8 Step 4-5；钩子表 12 行 → Task 1-3 + 7 全覆盖。

**2. 占位符扫描**：无 TBD/TODO；每个代码步骤都给了完整可粘贴代码块。

**3. 类型/签名一致性**：`onPlay(AbstractCard, AbstractMonster)`（Task 4 Step 2 改签名，Task 2 Step 7 patch 同步 `CardStats.onPlay(c, m)`）；`onMonsterDamage(DamageInfo, AbstractMonster, int)`（Task 2 Step 3 定义、Step 7 patch 同步）；`onPlayEnd/onGainEnergy/onDraw/onRoomEndTurn/onKarmaChange/onKarmaApply/onStanceSwitch/onDyingSaved` 均在 Task 1 Step 8 + Task 2 Step 2 定义且被 patch/内联调用；`appendCardGroupStats(sb, list, act/Integer null)`、`pct/stddev/maxNetLoss/packageNameOf/cardPackageOf/dicePackageOf/liveCardOf/powerEquivLine` 均在首次使用前定义；Combat/CardInstance/PowerLedger 字段名在 Task 1 定义后各任务只读引用，未再新增别名。

**4. 已知修正（计划已内嵌）**：回合结束钩点由 EndTurnAction 改为 AbstractRoom.endTurn（时序证据）；抽牌用 paramtypez 锁定 draw(int)；眩晕不做 HP 折算改报 stunTurns；ledgerBlockSaved 探测改 atDamageReceive；onPlay 透传目标 m 供巨浪条件判定。
