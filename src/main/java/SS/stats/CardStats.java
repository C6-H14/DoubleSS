package SS.stats;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;

import SS.Dice.AbstractDice;
import SS.path.AbstractCardEnum;
import SS.patches.DamageAllEnemiesDiceSource;
import SS.patches.DamageInfoDiceSource;
import SS.patches.GainBlockDiceSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 战斗统计系统（卡牌平衡分析）—— 核心数据模型与记录入口。
 *
 * <h3>设计要点</h3>
 *
 * <b>按实例追踪</b>：战斗初始化时 drawPile 由 masterDeck 经 makeSameInstanceOf() 生成
 * （保留 card.uuid），因此战斗中打出的任何牌都能按 uuid 回溯到卡组实例；
 * Echo/复制/双击等产生的同 uuid 副本自动并入同一实例（语义上就是"同一张牌"）。
 *
 * <b>卡牌栈（归属根基）</b>：useCard 只是入队，伤害/格挡几帧后才结算。
 * 出牌时压入带"水位"（当前动作队列长度）的帧，队列长度回落到水位即弹帧。
 * 队列实为 FIFO（addToBottom=尾部 add、执行从头部 remove(0)，已对照 GameActionManager 源码核实），
 * 关键在于：所有真实出牌都在 actions 队列为空时发生——
 * 手牌点击仅在 phase==WAITING_ON_USER（队列已空）时允许；cardQueue/limbo/本 mod
 * PlayCardAction 路径都由 getNextAction 在队列为空时才处理（GameActionManager:185-196）。
 * 因此水位恒为 0，帧会在该牌全部 action（含最后的 UseCardAction）执行完毕、
 * 队列彻底清空后才弹出，伤害/格挡结算时帧必在栈顶。
 * 所有真实出牌最终都汇聚到 AbstractPlayer.useCard（GameActionManager:296），一个钩子全覆盖。
 *
 * <b>归属（第二批）</b>：
 * - 卡牌直接伤害/格挡：结算时归属栈顶牌；帧上快照了出牌瞬间的力量/敏捷，
 *   按 min(快照, 结算值) 把力敏加成拆出来，按累计授予比例分摊给授予牌（持恒牌豁免）。
 * - 骰子伤害/格挡：骰子在充能瞬间被标注 sources（卡牌打出→栈顶牌；power 产骰→
 *   施加该 power 的牌列表；遗物产骰→空→无归属桶），结算时经三个 SpireField
 *   （DamageInfo/DamageAllEnemiesAction/GainBlockAction 挂骰子引用）回读，多来源均分。
 *   骰子数值一经生成不再改变，游戏也不对 DICE 型伤害加力量、不在 addBlock 里加敏捷，
 *   故骰子不归属力敏贡献（与游戏实际行为一致，不凭空造数）。
 *
 * <b>战斗/回合</b>：GameActionManager.turn 不随战斗重置（仅类加载时清零一次），
 * 故战斗开始记基线，每帧轮询最大值；回合数 = maxTurn − 基线 + 1
 * （正常击杀最后一回合未走 endTurn 流程、毒杀发生在回合开始，两种时序均验证成立）。
 */
public class CardStats {
    /** 游戏固定 4 幕。运行时 actNum 为 1 起始（dungeonTransitionSetup 开局 ++actNum）。 */
    public static final int ACTS = 4;

    // ==================== 数据模型 ====================

    /** 卡组中的一张牌实例（按 uuid 唯一）。数值按幕分桶。 */
    public static class CardInstance {
        /** 非 final：SL 读档重建卡组会重新生成 uuid，恢复快照时重映射到当前牌。 */
        public UUID uuid;
        public String cardID;
        public String name;
        /**
         * 卡包归属（v2 模块 2）。建实例时用活牌一次性算好并缓存——导出时按 ID
         * 回查 masterDeck 会因「牌已离开卡组/战斗内临时生成」而查不到，导致
         * 七宗罪整包被静默并入本体。缓存值在实例生命周期内不再变。
         */
        public String packageTag;
        /** 本局内是否曾经升级过（用于 升级/未升级 分组统计）。 */
        public boolean upgradedEver;
        public int[] plays = new int[ACTS];
        public int[] playsSingle = new int[ACTS];
        public int[] playsMulti = new int[ACTS];
        public long[] dmg = new long[ACTS];
        public long[] dmgSingle = new long[ACTS];
        public long[] dmgMulti = new long[ACTS];
        public long[] block = new long[ACTS];
        /** 直接授予的力量/敏捷层数（本牌作为能力牌授予）。 */
        public long[] strengthGrant = new long[ACTS];
        public long[] dexterityGrant = new long[ACTS];
        /** 分摊到本牌的力量/敏捷伤害/格挡贡献（授予牌按比例领取）。 */
        public long[] strengthContrib = new long[ACTS];
        public long[] dexterityContrib = new long[ACTS];
        /** 同 ID 实例序号（导出时区分重名实例）。 */
        public int copyIndex;

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

        public CardInstance() {
        }

        public CardInstance(AbstractCard c) {
            this.uuid = c.uuid;
            this.cardID = c.cardID;
            this.name = c.name;
            this.upgradedEver = c.timesUpgraded > 0;
        }
    }

    /** 一场战斗。 */
    public static class Combat {
        public int act;
        /** 0=小怪 1=精英 2=BOSS */
        public int roomType;
        public int enemyCount;
        public int hpStart;
        public int hpEnd = -1;
        public int damageTaken;
        public int turnBaseline;
        public int maxTurn;
        /** 战斗内每实例打出次数（uuid → plays）。非 final：读档 uuid 重映射时整体替换。 */
        public Map<UUID, Integer> playsByCard = new HashMap<>();
        /** 战斗开始时卡组实例快照（uuid 集合）。 */
        public final Set<UUID> deckAtStart = new HashSet<>();
        public boolean finalized = false;

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

        /** 结束时的实际回合数。 */
        public int rounds() {
            return Math.max(1, maxTurn - turnBaseline + 1);
        }

        /** 净 HP 损失（正数 = 掉血；治疗会抵消）。 */
        public int netHpLoss() {
            return hpEnd < 0 ? hpStart : hpStart - hpEnd;
        }
    }

    /** 力量/敏捷/buff 台账（按 power ID 累计，整局）。 */
    public static class PowerLedger {
        public final String powerId;
        public long stacksApplied = 0;        // 施加总层数（正数）
        public long damageDuring = 0;         // 该 buff 生效期间玩家造成的伤害
        public long blockDuring = 0;          // 该 buff 生效期间玩家获得的格挡
        public long damageTakenDuring = 0;    // 该 debuff 生效期间玩家受到的伤害
        public long damageDealtToTarget = 0;  // 对带此 debuff 的目标造成的伤害
        /** 防御性 buff（如怪物身上的虚弱）大约替玩家挡掉的伤害（格挡作用）。 */
        public long blockSaved = 0;
        /** 来源牌 cardID → 授予层数。 */
        public final Map<String, Long> granters = new HashMap<>();
        public boolean isDebuff;

        /** 显示名（onPowerApplied 建条目时取 power.name；快照恢复缺失时保持 powerId）。 */
        public String name = null;
        /** 每幕授予层数（stacksApplied 保留为总和）。 */
        public int[] stacksAppliedAct = new int[ACTS];
        /** 每幕激活回合数（新玩家回合采样：buff 查玩家 / debuff 查任一存活怪物）。 */
        public int[] coverageTurns = new int[ACTS];
        /** 持有期间玩家每次命中少打的力量值累加（Shackled 负力量等效）。 */
        public long hitValueSum = 0;

        public PowerLedger(String powerId, boolean isDebuff) {
            this.powerId = powerId;
            this.isDebuff = isDebuff;
            this.name = powerId;
        }
    }

    /** 骰子统计（orbId × act 聚合；骰子激发后即被销毁，无法按实例追踪）。 */
    public static class DiceStat {
        public final String orbId;
        public final String name;
        public final int act;
        /** 本幕产出的骰子个数（channelOrb 时计；满槽经 ChannelAction 入队的也走 channelOrb）。 */
        public long produced = 0;
        public long dmg = 0;
        public long block = 0;
        /** 来源牌显示名 → 分到的伤害+格挡点数。 */
        public final Map<String, Long> sourceShare = new HashMap<>();

        public DiceStat(String orbId, String name, int act) {
            this.orbId = orbId;
            this.name = name;
            this.act = act;
        }
    }

    // ==================== 局级状态 ====================

    /** 导出开关（SpireConfig("Double","Common").StatsExport，modcore 开局读取）。 */
    public static boolean enabled = false;

    private static final Map<UUID, CardInstance> instances = new HashMap<>();
    private static final ArrayList<Combat> combats = new ArrayList<>();
    private static Combat currentCombat = null;

    /** 诊断计数。 */
    private static long unattributedDamage = 0;      // 栈空且非骰子的伤害
    private static long unattributedDiceDamage = 0;  // 骰子 sources 为空的伤害
    private static long unattributedBlock = 0;
    private static long unattributedStrength = 0;    // 持恒牌豁免/无授予记录而未能分摊的贡献
    private static long unattributedDexterity = 0;
    private static int untrackedDamageEvents = 0;
    private static int forcedPops = 0;

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

    // 卡牌栈帧
    private static class Frame {
        final AbstractCard card;
        /**
         * 出牌前动作队列长度（水位）。该牌的全部 action 都在水位之后入队，
         * 队列长度回落到水位即该牌事务全部结束 → 弹帧。
         * 时序依据（已对照 GameActionManager 源码核实）：useCard 末尾总以
         * addToBottom(UseCardAction) 收尾，故该牌最后执行的一定是 UseCardAction；
         * 牌自身 action 造成伤害/格挡时 UseCardAction 仍在队列中，帧不会提前弹出。
         * 嵌套出牌（limbo/cardQueue 路径）仅在队列为空时执行，水位=0 同样成立。
         */
        final int watermark;
        /** 出牌瞬间快照：用于结算时拆出力敏贡献（持恒牌结算时豁免）。 */
        final int strengthAtPlay;
        final int dexterityAtPlay;
        float age;

        Frame(AbstractCard card, int watermark, int strengthAtPlay, int dexterityAtPlay) {
            this.card = card;
            this.watermark = watermark;
            this.strengthAtPlay = strengthAtPlay;
            this.dexterityAtPlay = dexterityAtPlay;
        }
    }

    private static final ArrayDeque<Frame> stack = new ArrayDeque<>();

    // ---- 骰子来源 ----
    /** power ID → 施加该 power 的牌（power 产骰时回溯 source 用；一次授予一条记录）。 */
    private static final Map<String, ArrayList<AbstractCard>> powerGranters = new HashMap<>();
    /** 静态上下文：正在结算的骰子 AOE 伤害 / 骰子格挡归属快照（AOE/格挡 action update 前后钩夹住）。 */
    private static DiceAttribution aoeDice = null;
    private static DiceAttribution blockDice = null;

    // ---- ApplyPowerAction 台账（update() 首帧去重 + 反射读 private powerToApply）----
    /** 已记账的 power 实例（按引用去重）：ApplyPowerAction.update() 每帧执行，
     *  同一实例只在首次见到时记一笔（新授/叠层都是新实例，天然各记一笔）。 */
    private static final Set<AbstractPower> recordedPowers =
            Collections.newSetFromMap(new IdentityHashMap<>());
    /** 缓存的 ApplyPowerAction#powerToApply 反射字段（private，仅读）。 */
    private static Field powerToApplyField = null;
    private static boolean powerToApplyFieldTried = false;

    // ---- 力敏授予比例（整局累计，uuid → 授予总层数）----
    private static final Map<UUID, Integer> strengthGranters = new HashMap<>();
    private static final Map<UUID, Integer> dexterityGranters = new HashMap<>();

    // ---- 台账 / 骰子统计 ----
    private static final Map<String, PowerLedger> ledgers = new HashMap<>();
    private static final Map<String, DiceStat> diceStats = new HashMap<>();
    /**
     * 已计过"产出"的骰子实例（引用去重）。满槽时 channelOrb 先入队 ChannelAction、
     * 同一骰子实例进槽时又触发一次 Postfix，不去重会把一个骰子计成 2 个产出。
     */
    private static final Set<AbstractOrb> channeledDice =
            Collections.newSetFromMap(new IdentityHashMap<>());

    // ==================== 生命周期 ====================

    /**
     * 局内数据重置（modcore.receiveStartGame 调用）。
     *
     * 关键时序：这版游戏"放弃游戏"= 置死亡屏 + 同帧 startOver() 开新一局
     * （ConfirmPopup → player.isDead=true; deathScreen=new DeathScreen() → startOver），
     * startOver 触发 BaseMod StartGame 事件 → 本方法。若不在这里先导出，
     * 下面 clear() 会把整局数据清掉，导出就只剩空文件（19:32:17/19:32:18 两个
     * 623 字节文件的根因）。死亡屏对象永不清空（全代码库无 deathScreen=null），
     * 用引用比较做"每局一次"去重：本局已导出过的死亡屏不会在下一局再次触发。
     */
    public static void reset() {
        exportIfTerminal();
        clear();
        clearSnapshotFile(); // 新开局：上一局的固化快照作废
    }

    /**
     * SL 读档（modcore.receiveStartGame 在 loadingSave 时调用，替代 reset 的 clear）。
     *
     * 为什么必须落盘恢复：读档重建卡组会重新生成 uuid（CardSave 只有 id/upgrades/misc），
     * 跨进程读档时内存态更是整个没了。快照 = 每场战斗胜利结束时的累计态（见
     * saveSnapshot）；恢复它即"已打完的战斗全部固化数据"。当前进行中的战斗（SL 前
     * 打的半截）其贡献被回滚丢弃——游戏会把那场战斗重开（用户确认的行为），
     * 重开时 onCombatStart 重新记录，正合"本房间数据临时存储、SL 重置临时存储"的口径。
     *
     * 牌实例重映射：快照里的 CardInstance 按旧 uuid 存，恢复后重挂到当前 deck 上
     * 同 cardID 的牌（按 deck 顺序一一对应；卡组构成与 SL 前一致）。
     */
    public static void onLoadSave() {
        clear(); // 丢内存态：含未固化的当前战斗临时数据（回滚）
        if (restoreSnapshot()) {
            System.out.println("[DoubleSS] 读档恢复战斗统计快照成功");
        } else {
            System.out.println("[DoubleSS] 读档时未找到统计快照（本局未打过完整战斗或跨进程前未部署），从空开始");
        }
    }

    /**
     * 终局导出检查（死亡/胜利）。
     * 每帧入口（update）与 reset 入口共用；以"死亡/胜利屏对象引用"为去重键，
     * 同一局只导一次；新局的 null/新对象不触发。
     */
    private static void exportIfTerminal() {
        Object terminal = null;
        if (AbstractDungeon.deathScreen != null) {
            terminal = AbstractDungeon.deathScreen;
        } else if (AbstractDungeon.victoryScreen != null) {
            terminal = AbstractDungeon.victoryScreen;
        }
        if (terminal == null || terminal == exportedForScreen) {
            return;
        }
        exportedForScreen = terminal;
        export();
    }

    /** 已导出时对应的死亡/胜利屏对象（引用去重，防同一局重复导出）。 */
    private static Object exportedForScreen = null;

    private static void clear() {
        instances.clear();
        combats.clear();
        currentCombat = null;
        stack.clear();
        unattributedDamage = 0;
        unattributedDiceDamage = 0;
        unattributedBlock = 0;
        unattributedStrength = 0;
        unattributedDexterity = 0;
        untrackedDamageEvents = 0;
        forcedPops = 0;
        powerGranters.clear();
        aoeDice = null;
        blockDice = null;
        recordedPowers.clear();
        strengthGranters.clear();
        dexterityGranters.clear();
        ledgers.clear();
        diceStats.clear();
        channeledDice.clear();
        karmaPlus50Hits = 0;
        karmaMinus50Hits = 0;
        dyingSaveTotal = 0;
        stanceSwitchTotal = 0;
        dyingSaveRooms.clear();
        unattributedEnergyGain = 0;
        unattributedDraws = 0;
        lastPlayedCard = null;
    }

    /** 当前幕（0 起始，越界保护）。 */
    private static int currentAct() {
        return Math.min(Math.max(AbstractDungeon.actNum - 1, 0), ACTS - 1);
    }

    // ---- SL 固化快照（"已打完的战斗"累计态落盘，读档时恢复）----

    /** 快照文件：analysis 目录下，不匹配清理正则（无时间戳前缀），不受 KEEP_FILES 影响。 */
    private static final String SNAPSHOT_NAME = "_cardstats_state.json";

    /**
     * 战斗胜利结束后固化：把当前全部累计态（实例/战斗/台账/骰子/诊断/力敏授予比例）
     * 序列化为 JSON 写盘。这是"temp 合并进主档"——战斗内的临时数据随战斗结束并入
     * 固化数据；下次 SL 读档时从它恢复。写失败只记日志，不影响游戏与导出。
     */
    public static void saveSnapshot() {
        if (!enabled) {
            return;
        }
        try {
            File dir = new File(Gdx.files.local("analysis").path());
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            JsonObject root = new JsonObject();
            root.addProperty("v", 2);

            JsonObject inst = new JsonObject();
            for (CardInstance ci : instances.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", ci.uuid.toString());
                o.addProperty("id", ci.cardID);
                o.addProperty("name", ci.name);
                o.addProperty("up", ci.upgradedEver);
                o.addProperty("ci", ci.copyIndex);
                o.add("eSpent", longArr(ci.energySpent));
                o.add("eGained", longArr(ci.energyGained));
                o.add("drawn", longArr(ci.drawn));
                o.addProperty("dwell", ci.handDwellSum);
                o.addProperty("discarded", ci.handDiscarded);
                o.addProperty("condTotal", ci.conditionTotal);
                o.addProperty("condMet", ci.conditionMet);
                if (ci.packageTag != null) {
                    o.addProperty("pkg", ci.packageTag);
                }
                o.add("plays", intArr(ci.plays));
                o.add("pSingle", intArr(ci.playsSingle));
                o.add("pMulti", intArr(ci.playsMulti));
                o.add("dmg", longArr(ci.dmg));
                o.add("dmgSingle", longArr(ci.dmgSingle));
                o.add("dmgMulti", longArr(ci.dmgMulti));
                o.add("block", longArr(ci.block));
                o.add("strG", longArr(ci.strengthGrant));
                o.add("dexG", longArr(ci.dexterityGrant));
                o.add("strC", longArr(ci.strengthContrib));
                o.add("dexC", longArr(ci.dexterityContrib));
                inst.add(ci.uuid.toString(), o);
            }
            root.add("inst", inst);

            JsonArray combs = new JsonArray();
            for (Combat c : combats) {
                JsonObject o = new JsonObject();
                o.addProperty("act", c.act);
                o.addProperty("rt", c.roomType);
                o.addProperty("en", c.enemyCount);
                o.addProperty("hp0", c.hpStart);
                o.addProperty("hp1", c.hpEnd);
                o.addProperty("dmgT", c.damageTaken);
                o.addProperty("base", c.turnBaseline);
                o.addProperty("max", c.maxTurn);
                o.addProperty("fin", c.finalized);
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
                JsonObject pb = new JsonObject();
                for (Map.Entry<UUID, Integer> e : c.playsByCard.entrySet()) {
                    pb.addProperty(e.getKey().toString(), e.getValue());
                }
                o.add("pb", pb);
                JsonArray dk = new JsonArray();
                for (UUID u : c.deckAtStart) {
                    dk.add(u.toString());
                }
                o.add("deck", dk);
                combs.add(o);
            }
            root.add("combs", combs);

            JsonObject leds = new JsonObject();
            for (PowerLedger L : ledgers.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", L.powerId);
                o.addProperty("debuff", L.isDebuff);
                if (L.name != null) {
                    o.addProperty("name", L.name);
                }
                o.add("stkAct", intArr(L.stacksAppliedAct));
                o.add("covTurns", intArr(L.coverageTurns));
                o.addProperty("hitVal", L.hitValueSum);
                o.addProperty("stacks", L.stacksApplied);
                o.addProperty("dmg", L.damageDuring);
                o.addProperty("blk", L.blockDuring);
                o.addProperty("dmgT", L.damageTakenDuring);
                o.addProperty("dealt", L.damageDealtToTarget);
                o.addProperty("saved", L.blockSaved);
                JsonObject gr = new JsonObject();
                for (Map.Entry<String, Long> e : L.granters.entrySet()) {
                    gr.addProperty(e.getKey(), e.getValue());
                }
                o.add("gr", gr);
                leds.add(L.powerId, o);
            }
            root.add("leds", leds);

            JsonObject dcs = new JsonObject();
            for (DiceStat d : diceStats.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", d.orbId);
                o.addProperty("name", d.name);
                o.addProperty("act", d.act);
                o.addProperty("produced", d.produced);
                o.addProperty("dmg", d.dmg);
                o.addProperty("blk", d.block);
                JsonObject ss = new JsonObject();
                for (Map.Entry<String, Long> e : d.sourceShare.entrySet()) {
                    ss.addProperty(e.getKey(), e.getValue());
                }
                o.add("share", ss);
                dcs.add(d.orbId + "#" + d.act, o);
            }
            root.add("dice", dcs);

            JsonObject diag = new JsonObject();
            diag.addProperty("uDam", unattributedDamage);
            diag.addProperty("uDice", unattributedDiceDamage);
            diag.addProperty("uBlk", unattributedBlock);
            diag.addProperty("uStr", unattributedStrength);
            diag.addProperty("uDex", unattributedDexterity);
            diag.addProperty("uEv", untrackedDamageEvents);
            diag.addProperty("forced", forcedPops);
            root.add("diag", diag);

            JsonObject strG = new JsonObject();
            for (Map.Entry<UUID, Integer> e : strengthGranters.entrySet()) {
                strG.addProperty(e.getKey().toString(), e.getValue());
            }
            root.add("strG", strG);
            JsonObject dexG = new JsonObject();
            for (Map.Entry<UUID, Integer> e : dexterityGranters.entrySet()) {
                dexG.addProperty(e.getKey().toString(), e.getValue());
            }
            root.add("dexG", dexG);

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

            try (BufferedWriter w = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(new File(dir, SNAPSHOT_NAME)), StandardCharsets.UTF_8))) {
                w.write(root.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 读档时从快照恢复累计态并做牌实例 uuid 重映射。成功返回 true。 */
    private static boolean restoreSnapshot() {
        try {
            File f = new File(Gdx.files.local("analysis").path(), SNAPSHOT_NAME);
            if (!f.exists()) {
                return false;
            }
            String text;
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = r.read()) != -1) {
                    sb.append((char) ch);
                }
                text = sb.toString();
            }
            if (text.isEmpty()) {
                return false;
            }
            // 不用 JsonParser.parseString（Gson 2.8.6+ 才有），老 API 全版本兼容
            JsonObject root = new com.google.gson.JsonParser().parse(text).getAsJsonObject();
            if (root == null) {
                return false;
            }

            JsonObject inst = root.getAsJsonObject("inst");
            if (inst != null) {
                for (Map.Entry<String, JsonElement> en : inst.entrySet()) {
                    JsonObject o = en.getValue().getAsJsonObject();
                    CardInstance ci = new CardInstance();
                    ci.uuid = UUID.fromString(o.get("uuid").getAsString());
                    ci.cardID = o.get("id").getAsString();
                    ci.name = o.get("name").getAsString();
                    ci.upgradedEver = o.get("up").getAsBoolean();
                    ci.copyIndex = o.get("ci").getAsInt();
                    ci.plays = intArr(o.getAsJsonArray("plays"));
                    ci.playsSingle = intArr(o.getAsJsonArray("pSingle"));
                    ci.playsMulti = intArr(o.getAsJsonArray("pMulti"));
                    ci.dmg = longArr(o.getAsJsonArray("dmg"));
                    ci.dmgSingle = longArr(o.getAsJsonArray("dmgSingle"));
                    ci.dmgMulti = longArr(o.getAsJsonArray("dmgMulti"));
                    ci.block = longArr(o.getAsJsonArray("block"));
                    ci.strengthGrant = longArr(o.getAsJsonArray("strG"));
                    ci.dexterityGrant = longArr(o.getAsJsonArray("dexG"));
                    ci.strengthContrib = longArr(o.getAsJsonArray("strC"));
                    ci.dexterityContrib = longArr(o.getAsJsonArray("dexC"));
                    if (o.has("eSpent")) {
                        ci.energySpent = longArr(o.getAsJsonArray("eSpent"));
                        ci.energyGained = longArr(o.getAsJsonArray("eGained"));
                        ci.drawn = longArr(o.getAsJsonArray("drawn"));
                        ci.handDwellSum = o.get("dwell").getAsLong();
                        ci.handDiscarded = o.get("discarded").getAsLong();
                        ci.conditionTotal = o.get("condTotal").getAsInt();
                        ci.conditionMet = o.get("condMet").getAsInt();
                        if (o.has("pkg")) {
                            ci.packageTag = o.get("pkg").getAsString();
                        }
                    }
                    instances.put(ci.uuid, ci);
                }
            }

            JsonArray combs = root.getAsJsonArray("combs");
            if (combs != null) {
                for (JsonElement ce : combs) {
                    JsonObject o = ce.getAsJsonObject();
                    Combat c = new Combat();
                    c.act = o.get("act").getAsInt();
                    c.roomType = o.get("rt").getAsInt();
                    c.enemyCount = o.get("en").getAsInt();
                    c.hpStart = o.get("hp0").getAsInt();
                    c.hpEnd = o.get("hp1").getAsInt();
                    c.damageTaken = o.get("dmgT").getAsInt();
                    c.turnBaseline = o.get("base").getAsInt();
                    c.maxTurn = o.get("max").getAsInt();
                    c.finalized = o.get("fin").getAsBoolean();
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
                    JsonObject pb = o.getAsJsonObject("pb");
                    if (pb != null) {
                        for (Map.Entry<String, JsonElement> e : pb.entrySet()) {
                            c.playsByCard.put(UUID.fromString(e.getKey()), e.getValue().getAsInt());
                        }
                    }
                    JsonArray dk = o.getAsJsonArray("deck");
                    if (dk != null) {
                        for (JsonElement de : dk) {
                            c.deckAtStart.add(UUID.fromString(de.getAsString()));
                        }
                    }
                    combats.add(c);
                }
            }

            JsonObject leds = root.getAsJsonObject("leds");
            if (leds != null) {
                for (Map.Entry<String, JsonElement> en : leds.entrySet()) {
                    JsonObject o = en.getValue().getAsJsonObject();
                    PowerLedger L = new PowerLedger(o.get("id").getAsString(), o.get("debuff").getAsBoolean());
                    L.stacksApplied = o.get("stacks").getAsLong();
                    L.damageDuring = o.get("dmg").getAsLong();
                    L.blockDuring = o.get("blk").getAsLong();
                    L.damageTakenDuring = o.get("dmgT").getAsLong();
                    L.damageDealtToTarget = o.get("dealt").getAsLong();
                    L.blockSaved = o.get("saved").getAsLong();
                    if (o.has("stkAct")) {
                        L.name = o.has("name") ? o.get("name").getAsString() : L.powerId;
                        L.stacksAppliedAct = intArr(o.getAsJsonArray("stkAct"));
                        L.coverageTurns = intArr(o.getAsJsonArray("covTurns"));
                        L.hitValueSum = o.get("hitVal").getAsLong();
                    }
                    JsonObject gr = o.getAsJsonObject("gr");
                    if (gr != null) {
                        for (Map.Entry<String, JsonElement> e : gr.entrySet()) {
                            L.granters.put(e.getKey(), e.getValue().getAsLong());
                        }
                    }
                    ledgers.put(L.powerId, L);
                }
            }

            JsonObject dcs = root.getAsJsonObject("dice");
            if (dcs != null) {
                for (Map.Entry<String, JsonElement> en : dcs.entrySet()) {
                    JsonObject o = en.getValue().getAsJsonObject();
                    DiceStat d = new DiceStat(o.get("id").getAsString(), o.get("name").getAsString(), o.get("act").getAsInt());
                    d.produced = o.get("produced").getAsLong();
                    d.dmg = o.get("dmg").getAsLong();
                    d.block = o.get("blk").getAsLong();
                    JsonObject ss = o.getAsJsonObject("share");
                    if (ss != null) {
                        for (Map.Entry<String, JsonElement> e : ss.entrySet()) {
                            d.sourceShare.put(e.getKey(), e.getValue().getAsLong());
                        }
                    }
                    diceStats.put(o.get("id").getAsString() + "#" + o.get("act").getAsInt(), d);
                }
            }

            JsonObject diag = root.getAsJsonObject("diag");
            if (diag != null) {
                unattributedDamage = diag.get("uDam").getAsLong();
                unattributedDiceDamage = diag.get("uDice").getAsLong();
                unattributedBlock = diag.get("uBlk").getAsLong();
                unattributedStrength = diag.get("uStr").getAsLong();
                unattributedDexterity = diag.get("uDex").getAsLong();
                untrackedDamageEvents = diag.get("uEv").getAsInt();
                forcedPops = diag.get("forced").getAsInt();
            }
            JsonObject strG = root.getAsJsonObject("strG");
            if (strG != null) {
                for (Map.Entry<String, JsonElement> e : strG.entrySet()) {
                    strengthGranters.put(UUID.fromString(e.getKey()), e.getValue().getAsInt());
                }
            }
            JsonObject dexG = root.getAsJsonObject("dexG");
            if (dexG != null) {
                for (Map.Entry<String, JsonElement> e : dexG.entrySet()) {
                    dexterityGranters.put(UUID.fromString(e.getKey()), e.getValue().getAsInt());
                }
            }

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

            remapInstancesToDeck();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 删除快照文件（新开局/导出完成后调用）。 */
    private static void clearSnapshotFile() {
        try {
            File f = new File(Gdx.files.local("analysis").path(), SNAPSHOT_NAME);
            if (f.exists() && !f.delete()) {
                System.out.println("[DoubleSS] 删除统计快照失败: " + f.getAbsolutePath());
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 读档后把快照里的牌实例重映射到当前 deck：读档重建卡组生成新 uuid，
     * 按 cardID 把旧实例（含数据）迁到当前牌上，并同步 playsByCard / 授予比例表的键。
     * 迁移策略：快照实例按 cardID 归组，当前 deck 里同 cardID 的牌按出现顺序依次
     * 领走一个实例（卡组构成与 SL 前一致，顺序稳定）。
     */
    private static void remapInstancesToDeck() {
        com.megacrit.cardcrawl.characters.AbstractPlayer p = AbstractDungeon.player;
        if (p == null || p.masterDeck == null) {
            return;
        }
        // cardID → 快照实例队列（保序）
        Map<String, java.util.ArrayDeque<CardInstance>> byId = new HashMap<>();
        for (CardInstance ci : instances.values()) {
            byId.computeIfAbsent(ci.cardID, k -> new java.util.ArrayDeque<>()).add(ci);
        }
        Map<UUID, UUID> oldToNew = new HashMap<>();
        for (AbstractCard card : p.masterDeck.group) {
            java.util.ArrayDeque<CardInstance> q = byId.get(card.cardID);
            if (q != null && !q.isEmpty()) {
                CardInstance ci = q.poll();
                if (ci.uuid != card.uuid) {
                    oldToNew.put(ci.uuid, card.uuid);
                    instances.remove(ci.uuid);
                    ci.uuid = card.uuid;
                    instances.put(ci.uuid, ci);
                }
            }
        }
        if (oldToNew.isEmpty()) {
            return;
        }
        // 同步 Combat.playsByCard 与授予比例表的 uuid 键（授予表是 final，原地重建内容）
        for (Combat c : combats) {
            Map<UUID, Integer> nb = new HashMap<>();
            for (Map.Entry<UUID, Integer> e : c.playsByCard.entrySet()) {
                nb.merge(oldToNew.getOrDefault(e.getKey(), e.getKey()), e.getValue(), Integer::sum);
            }
            c.playsByCard.clear();
            c.playsByCard.putAll(nb);
        }
        remapGrantersInPlace(strengthGranters, oldToNew);
        remapGrantersInPlace(dexterityGranters, oldToNew);
    }

    /** 授予比例表（final 字段）的 uuid 键重映射：旧键迁移到新键，同键合并。 */
    private static void remapGrantersInPlace(Map<UUID, Integer> src, Map<UUID, UUID> oldToNew) {
        Map<UUID, Integer> out = new HashMap<>();
        for (Map.Entry<UUID, Integer> e : src.entrySet()) {
            out.merge(oldToNew.getOrDefault(e.getKey(), e.getKey()), e.getValue(), Integer::sum);
        }
        src.clear();
        src.putAll(out);
    }

    private static JsonArray longArr(long[] arr) {
        JsonArray a = new JsonArray();
        for (long v : arr) {
            a.add(v);
        }
        return a;
    }

    private static JsonArray intArr(int[] arr) {
        JsonArray a = new JsonArray();
        for (int v : arr) {
            a.add(v);
        }
        return a;
    }

    private static int[] intArr(JsonArray a) {
        int[] out = new int[ACTS];
        if (a == null) {
            return out;
        }
        for (int i = 0; i < ACTS && i < a.size(); i++) {
            out[i] = a.get(i).getAsInt();
        }
        return out;
    }

    private static long[] longArr(JsonArray a) {
        long[] out = new long[ACTS];
        if (a == null) {
            return out;
        }
        for (int i = 0; i < ACTS && i < a.size(); i++) {
            out[i] = a.get(i).getAsLong();
        }
        return out;
    }

    /** 战斗开始（modcore.receiveOnBattleStart 调用；小怪/精英/BOSS 三类房间都会到这里）。 */
    public static void onCombatStart(AbstractRoom room) {
        if (!enabled) {
            return;
        }
        // 战斗开始时栈必须为空（上一场的牌事务应在场间清空）；若非空说明泄漏，强制清理并记诊断
        if (!stack.isEmpty()) {
            forcedPops += stack.size();
            stack.clear();
        }
        aoeDice = null;
        blockDice = null;
        // power 不跨战斗存在：清掉授予映射与力敏授予比例，避免旧战斗的牌稀释本场的分摊
        powerGranters.clear();
        strengthGranters.clear();
        dexterityGranters.clear();
        clearSoulWeights(); // 魂火不跨战斗存活，贡献权重随战斗重置
        soulWatermark = -1; // 魂火事务窗口一并复位
        Combat c = new Combat();
        // 本版本 actNum 是 1 起始（dungeonTransitionSetup 开局 ++actNum），索引用 actNum-1
        c.act = Math.min(Math.max(AbstractDungeon.actNum - 1, 0), ACTS - 1);
        c.roomType = room instanceof MonsterRoomBoss ? 2 : room instanceof MonsterRoomElite ? 1 : 0;
        c.enemyCount = (room.monsters != null && room.monsters.monsters != null) ? room.monsters.monsters.size() : 0;
        c.hpStart = AbstractDungeon.player.currentHealth;
        c.maxHpStart = AbstractDungeon.player.maxHealth;
        c.turnBaseline = GameActionManager.turn;
        c.maxTurn = c.turnBaseline;
        if (AbstractDungeon.player.masterDeck != null) {
            for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
                c.deckAtStart.add(card.uuid);
            }
        }
        combats.add(c);
        currentCombat = c;
    }

    /** 战斗胜利结束（AbstractRoom.endBattle Postfix）。死亡路径不走这里，导出时收尾。 */
    public static void onCombatWin() {
        if (currentCombat != null) {
            finalizeCombat(currentCombat, AbstractDungeon.player.currentHealth);
        }
        currentCombat = null;
        saveSnapshot(); // 固化：本场战斗数据并入快照，SL 读档可恢复到此
    }

    private static void finalizeCombat(Combat c, int hpEnd) {
        if (c.finalized) {
            return;
        }
        c.hpEnd = hpEnd;
        c.maxHpEnd = AbstractDungeon.player.maxHealth; // endBattle 体内先调 player.onVictory（罪孽结算 maxHp 变动）再返回，Postfix 时已生效
        c.finalized = true;
    }

    // ==================== 事件入口（由 patch 调用） ====================

    /** 出牌（AbstractPlayer.useCard Prefix，入栈 + 计数）。m = useCard 的目标（条件牌判定用，可为 null）。 */
    public static void onPlay(AbstractCard c, AbstractMonster m) {
        if (!enabled || currentCombat == null) {
            return;
        }
        CardInstance ins = getOrCreate(c);
        int act = currentCombat.act;
        ins.plays[act]++;
        if (currentCombat.enemyCount > 1) {
            ins.playsMulti[act]++;
        } else {
            ins.playsSingle[act]++;
        }
        if (c.timesUpgraded > 0) {
            ins.upgradedEver = true;
        }
        int prev = currentCombat.playsByCard.getOrDefault(c.uuid, 0);
        currentCombat.playsByCard.put(c.uuid, prev + 1);
        // 快照出牌瞬间的力敏（结算时的贡献以快照为准，避免同牌"先加力再打伤害"的时序误差）
        int str = 0, dex = 0;
        AbstractPower sp = AbstractDungeon.player.getPower("Strength");
        if (sp != null) {
            str = sp.amount;
        }
        AbstractPower dp = AbstractDungeon.player.getPower("Dexterity");
        if (dp != null) {
            dex = dp.amount;
        }
        // 水位在 useCard 方法体执行前捕获：该牌的全部 action 都压在其后
        stack.push(new Frame(c, AbstractDungeon.actionManager.actions.size(), str, dex));
        // v2：滞留结算（打出即清零）+ 本回合最后打出牌 + 身份对位计数
        ins.handDwellSum += ins.currentDwell;
        ins.currentDwell = 0;
        // v2：条件牌触发率（注册表外的牌不计数；判定用 useCard 的目标 m，与牌 use() 门槛同刻）
        CardConditionRegistry.Condition cond = CardConditionRegistry.lookup(c.cardID);
        if (cond != null) {
            ins.conditionTotal++;
            if (cond.met(c, m)) {
                ins.conditionMet++;
            }
        }
        lastPlayedCard = c;
        AbstractPlayer pl = AbstractDungeon.player;
        if (pl.hasPower("Double:FiendStance")) {
            currentCombat.fiendPlays++;
            if (c.hasTag(AbstractCardEnum.Sins)) {
                currentCombat.fiendSinPlays++;
            }
        } else if (pl.hasPower("Double:ManagerStance")) {
            currentCombat.managerPlays++;
            // 与卡包审计节同源：都走 cardPackageOf（Sins tag 优先，再查 cardParentMap）。
            // 原来这里硬编码 equals("Double:HaoPackage")、审计节用 contains——两节口径必须一致，
            // 否则同一张卡会在审计节算「Hao」、在群友卡占比里不算。cardPackageOf 用 contains 系列匹配，
            // 对 HaoPackage / HaoPackage_v / _c / _e 都成立。
            boolean hao = "Hao".equals(cardPackageOf(c.cardID, c));
            if (c.hasTag(AbstractCardEnum.Manager) || hao) {
                currentCombat.managerHaoPlays++;
            }
        }
    }

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

    /** 玩家对怪物造成伤害（AbstractMonster.damage Postfix，info.owner==player）。 */
    public static void onMonsterDamage(DamageInfo info, AbstractMonster target, int hpBefore) {
        if (!enabled) {
            return;
        }
        if (currentCombat == null) {
            untrackedDamageEvents++;
            return;
        }
        int act = currentCombat.act;
        int amount = Math.max(0, info.output);
        if (amount <= 0) {
            return;
        }
        // v2：本场总伤害（含骰子，先于归属分支累计）+ 溢出伤害（超出怪物伤害前剩余 HP）
        currentCombat.damageDealt += amount;
        currentCombat.overkill += Math.max(0, amount - hpBefore);

        // 1) 骰子伤害：SpireField（单目标）或 AOE 静态上下文（HitAll / NextTurnDamagePower 延迟 AOE）
        DiceAttribution att = DamageInfoDiceSource.diceRef.get(info);
        if (att == null) {
            att = aoeDice;
        }
        if (att != null) {
            attributeDice(att, amount, true, act);
            return;
        }

        // 1.5) 魂火跟班伤害：按各唤魂牌的魂火值贡献权重均分（用户定口径）。
        //      置于骰子之后、卡牌栈之前——魂火打出的攻击骰已由上面骰子分支消化，
        //      其余（魂火直接打的卡）走这里。栈顶此时是魂火自己的牌，不是玩家的牌。
        if (inSoulDamage()) {
            attributeSoul(amount, true, act);
            return;
        }

        // 2) 卡牌直接伤害：栈顶牌
        Frame top = stack.peek();
        if (top != null) {
            CardInstance ins = getOrCreate(top.card);
            ins.dmg[act] += amount;
            if (currentCombat.enemyCount > 1) {
                ins.dmgMulti[act] += amount;
            } else {
                ins.dmgSingle[act] += amount;
            }
            // 3) 力量贡献：只有 NORMAL 型吃力量（StrengthPower.atDamageGive 判 NORMAL），
            //    DICE/THORNS/DELAY 型游戏不加力量 → 不归属（不凭空造数）
            if (info.type == DamageInfo.DamageType.NORMAL && top.strengthAtPlay > 0) {
                long contrib = Math.min(top.strengthAtPlay, amount);
                if (contrib > 0) {
                    splitContribution(strengthGranters, contrib, act, true, top.card);
                }
            }
            // v2：玩家持有 Shackled（负力量）时每次命中少打的力量值
            AbstractPower sh = AbstractDungeon.player.getPower("Shackled");
            if (sh != null && sh.amount < 0) {
                PowerLedger L = ledgers.get("Shackled");
                if (L != null) {
                    L.hitValueSum += Math.min(-sh.amount, amount);
                }
            }
            // buff 台账：该 buff 生效期间玩家造成伤害 + 怪物防御性 buff 的格挡作用
            ledgerDamageDealt(amount, target);
        } else {
            unattributedDamage += amount;
        }
    }

    /** 玩家 HP 实际损失（AbstractPlayer.damage Prefix/Postfix 前后差）。 */
    public static void onPlayerHpLoss(int loss, DamageInfo info) {
        if (!enabled) {
            return;
        }
        if (loss <= 0) {
            return;
        }
        if (currentCombat == null) {
            untrackedDamageEvents++;
            return;
        }
        currentCombat.damageTaken += loss;
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
        // debuff 台账：该 debuff 生效期间玩家受到伤害（仅敌人来源）
        if (info != null && info.owner != null && info.owner != AbstractDungeon.player) {
            AbstractCreature p = AbstractDungeon.player;
            for (AbstractPower pw : p.powers) {
                if (pw.type == AbstractPower.PowerType.DEBUFF && pw.ID != null) {
                    PowerLedger L = ledgers.get(pw.ID);
                    if (L != null) {
                        L.damageTakenDuring += loss;
                    }
                }
            }
        }
    }

    /** 玩家获得格挡（AbstractCreature.addBlock Postfix，target==player）。 */
    public static void onPlayerBlock(int amount) {
        if (!enabled || amount <= 0 || currentCombat == null) {
            return;
        }
        int act = currentCombat.act;
        currentCombat.blockGained += amount;

        // 1) 骰子格挡：GainBlockAction 上的静态上下文
        DiceAttribution att = blockDice;
        if (att != null) {
            attributeDice(att, amount, false, act);
            return;
        }

        // 1.5) 魂火打出的牌若给玩家加格挡（blkAct → GainBlockAction(player)），
        //      归到唤魂牌头上（用户口径：魂火的伤害与格挡都按魂火值加权均分）。
        if (inSoulDamage()) {
            attributeSoul(amount, false, act);
            return;
        }

        // 2) 卡牌直接格挡：栈顶牌
        //    本版本敏捷在出牌烘焙时生效（applyPowersToBlock → card.block），
        //    addBlock 本身不加敏捷 → 贡献 = min(出牌时敏捷, 结算格挡)
        Frame top = stack.peek();
        if (top != null) {
            CardInstance ins = getOrCreate(top.card);
            ins.block[act] += amount;
            if (top.dexterityAtPlay > 0) {
                long contrib = Math.min(top.dexterityAtPlay, amount);
                if (contrib > 0) {
                    splitContribution(dexterityGranters, contrib, act, false, top.card);
                }
            }
            // buff 台账：该 buff 生效期间玩家获得格挡
            for (AbstractPower pw : AbstractDungeon.player.powers) {
                if (pw.type == AbstractPower.PowerType.BUFF && pw.ID != null) {
                    PowerLedger L = ledgers.get(pw.ID);
                    if (L != null) {
                        L.blockDuring += amount;
                    }
                }
            }
        } else {
            unattributedBlock += amount;
        }
    }

    /**
     * 充能完成（AbstractPlayer/AbstractSSCharacter.channelOrb Postfix）。
     * 卡牌打出充能时栈顶 = 充能牌 → 标注为 source；
     * power 产骰已在 power 内预先标注（sources 非空则不动）；
     * 遗物/环境产骰栈为空 → sources 保持空 → 结算时进无归属桶。
     */
    public static void onOrbChanneled(com.megacrit.cardcrawl.orbs.AbstractOrb orb) {
        if (!enabled || orb == null) {
            return;
        }
        if (orb instanceof AbstractDice) {
            AbstractDice d = (AbstractDice) orb;
            if (d.sources.isEmpty()) {
                AbstractCard top = stackTopCard();
                if (top != null) {
                    d.sources.add(top);
                }
            }
            // 产出计数：真正进槽才算（player.orbs 已包含本骰子）。满槽路径里
            // 首次 channelOrb 只入队 ChannelAction（未进槽）、同一实例进槽时
            // 第二次 Postfix 才满足 contains → IdentityHashMap 去重双重保险。
            if (AbstractDungeon.player != null
                    && AbstractDungeon.player.orbs.contains(orb)
                    && channeledDice.add(orb)) {
                DiceStat st = diceStats.get(orb.ID + "#" + currentAct());
                if (st == null) {
                    st = new DiceStat(orb.ID, orb.name, currentAct());
                    diceStats.put(orb.ID + "#" + currentAct(), st);
                }
                st.produced++;
            }
        }
    }

    /**
     * power 施加记账入口（CombatStatsPatch 在 ApplyPowerAction.update() Postfix 调用）。
     * 反射读 private powerToApply + 按 power 实例去重（同一 action 每帧 update 都会来，只记首帧）。
     * 同时读 ApplyPowerDiceSource.diceRef：骰子激发施加的 power 把打出牌精确挂到本 power 上
     * （回合末激发时卡牌栈已空，栈顶归属失效，必须走骰子 sources）。
     */
    public static void onPowerAppliedFrame(Object applyPowerAction) {
        if (!enabled || applyPowerAction == null) {
            return;
        }
        AbstractPower power = readPowerToApply(applyPowerAction);
        if (power == null || power.ID == null) {
            return;
        }
        // 同一 action 实例的多次 update 只记一笔（新授/叠层各是新实例，天然各记一笔）
        if (!recordedPowers.add(power)) {
            return;
        }
        AbstractDice dice = null;
        try {
            dice = SS.patches.ApplyPowerDiceSource.diceRef.get(applyPowerAction);
        } catch (Exception ignored) {
        }
        onPowerApplied(power, dice);
    }

    /** 反射读 ApplyPowerAction#powerToApply（private 字段，字段句柄一次性缓存）。 */
    private static AbstractPower readPowerToApply(Object action) {
        try {
            if (!powerToApplyFieldTried) {
                powerToApplyFieldTried = true;
                try {
                    Field f = com.megacrit.cardcrawl.actions.common.ApplyPowerAction.class
                            .getDeclaredField("powerToApply");
                    f.setAccessible(true);
                    powerToApplyField = f;
                } catch (NoSuchFieldException e) {
                    powerToApplyField = null; // 字段改名/消失 → 统计跳过，绝不影响游戏
                }
            }
            if (powerToApplyField != null) {
                return (AbstractPower) powerToApplyField.get(action);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 记账主体。记录三件事：power→授予牌映射（骰子回溯用）、力敏直接授予（比例分摊用）、
     * buff/debuff 台账。授予比例表与 power→牌 映射在每场战斗开始时清零（onCombatStart），
     * 把"旧牌过期后稀释分摊"的误差限制在单场战斗内。
     *
     * @param dice 骰子激发施加本 power 时的骰子引用（null = 卡牌/环境直接施加）。
     *             授予归属优先取 dice.sources（回合末激发时卡牌栈已空，栈顶归属失效）。
     */
    private static void onPowerApplied(AbstractPower power, AbstractDice dice) {
        boolean playerOwned = power.owner == AbstractDungeon.player;
        List<AbstractCard> grantCardList = null;
        if (dice != null && dice.sources != null && !dice.sources.isEmpty()) {
            grantCardList = dice.sources;
        } else {
            AbstractCard top = stackTopCard();
            if (top != null) {
                grantCardList = java.util.Collections.singletonList(top);
            }
        }

        // 1) power → 授予牌映射（power 产骰时回溯 source；只记玩家自身 buff/debuff）
        //    多张能力牌叠加同一 power 时列表有多条，骰子 source 按列表均分
        if (playerOwned && grantCardList != null) {
            ArrayList<AbstractCard> list = powerGranters.computeIfAbsent(power.ID, k -> new ArrayList<>());
            list.addAll(grantCardList);
        }

        // 2) 力敏直接授予（只有正层数参与比例分摊）
        if (playerOwned && grantCardList != null && power.amount > 0) {
            int act = currentCombat != null ? currentCombat.act : 0;
            for (AbstractCard top : grantCardList) {
                CardInstance ins = getOrCreate(top);
                if (power instanceof StrengthPower) {
                    ins.strengthGrant[act] += power.amount / grantCardList.size();
                    strengthGranters.merge(top.uuid, power.amount / grantCardList.size(), Integer::sum);
                } else if (power instanceof DexterityPower) {
                    ins.dexterityGrant[act] += power.amount / grantCardList.size();
                    dexterityGranters.merge(top.uuid, power.amount / grantCardList.size(), Integer::sum);
                }
            }
        }

        // 3) 台账：玩家自身 buff/debuff（期间伤害/格挡）+ 怪物身上的 debuff
        //    （对带此 debuff 目标造成的伤害，如流血/易伤）
        boolean monsterDebuff = !playerOwned && power.type == AbstractPower.PowerType.DEBUFF;
        int ledgerAct = currentCombat != null ? currentCombat.act : 0;
        if (playerOwned || monsterDebuff) {
            boolean debuff = power.type == AbstractPower.PowerType.DEBUFF;
            PowerLedger L = ledgers.get(power.ID);
            if (L == null) {
                L = new PowerLedger(power.ID, debuff);
                if (power.name != null && !power.name.isEmpty()) {
                    L.name = power.name;
                }
                ledgers.put(power.ID, L);
            }
            if (power.amount > 0) {
                L.stacksApplied += power.amount;
                L.stacksAppliedAct[ledgerAct] += power.amount;
                if (grantCardList != null) {
                    for (AbstractCard top : grantCardList) {
                        L.granters.merge(top.cardID, (long) power.amount / grantCardList.size(), Long::sum);
                    }
                }
            }
        }
    }

    /**
     * power 产骰时标注 source：返回施加该 power 的牌列表（可能多张能力牌叠加，
     * 结算时均分）；找不到记录（环境/开局/遗物授予）返回 null（sources 保持空 →
     * 无归属桶）。power 内调用：
     * <pre>ArrayList<AbstractCard> src = CardStats.powerSourcesFor(this.ID);
     * if (src != null) { dice.sources.addAll(src); }</pre>
     */
    public static ArrayList<AbstractCard> powerSourcesFor(String powerId) {
        if (!enabled || powerId == null) {
            return null;
        }
        ArrayList<AbstractCard> list = powerGranters.get(powerId);
        return (list != null && !list.isEmpty()) ? list : null;
    }

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

    /** 骰子 AOE 伤害结算上下文（DamageAllEnemiesAction.update 前后钩）。 */
    public static void setAoeDice(DiceAttribution d) {
        aoeDice = d;
    }

    /** 骰子格挡结算上下文（GainBlockAction.update 前后钩）。 */
    public static void setBlockDice(DiceAttribution d) {
        blockDice = d;
    }

    // ==================== 每帧驱动（modcore.receivePostUpdate 调用） ====================

    public static void update() {
        if (!enabled) {
            return;
        }
        // Quick Restart / save-load briefly leaves the dungeon without a current
        // map node. AbstractDungeon.getCurrRoom() dereferences that node, so no
        // per-combat statistic may query the room during this transition.
        if (AbstractDungeon.currMapNode == null) {
            return;
        }
        GameActionManager am = AbstractDungeon.actionManager;
        if (am == null) {
            return;
        }
        // 回合计数 + v2 新回合采样（turn 跨战斗累积，maxTurn 初值=基线，第一次超越恰为 T1）
        if (currentCombat != null && GameActionManager.turn > currentCombat.maxTurn) {
            currentCombat.maxTurn = GameActionManager.turn;
            sampleNewTurn(currentCombat, GameActionManager.turn - currentCombat.turnBaseline);
        }
        // v2 眩晕边沿检测：怪物意图转为 STUN 时计 1（该回合跳过行动）；转回其他意图时移出集合，
        // 允许再次眩晕再计。已核实四例眩晕怪（Lagavulin/BronzeAutomaton/Byrd/ShelledParasite）
        // 的 STUN 意图回合都真实执行了 STUNNED 文本+RollMoveAction，意图被新意图替换 → 每跳过一回合恰好计一次。
        if (currentCombat != null) {
            AbstractRoom cr = AbstractDungeon.getCurrRoom();
            if (cr != null && !cr.isBattleOver && cr.monsters != null && cr.monsters.monsters != null) {
                com.megacrit.cardcrawl.monsters.MonsterGroup g = cr.monsters;
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
        }
        // 魂火事务窗口：队列回落到水位即结束（与下面的玩家卡牌栈同构）
        tickSoulWindow();
        // 卡牌栈排水：队列长度回落到水位 = 该牌事务全部结束
        if (!stack.isEmpty()) {
            while (!stack.isEmpty() && stack.peek().watermark >= am.actions.size()) {
                stack.pop();
            }
            // 兜底：帧滞留过久（理论不应发生）强制弹出，记诊断
            for (Frame f : stack) {
                f.age += Gdx.graphics.getDeltaTime();
            }
            Frame top = stack.peek();
            if (top != null && top.age > 20f) {
                stack.pop();
                forcedPops++;
            }
        }
        // 终局导出：死亡或胜利（按屏对象引用去重，每局一次）
        exportIfTerminal();
    }

    /**
     * 新玩家回合采样（每回合一次）：Power 激活覆盖 / 奄息危险 / 身份回合。
     * 在回合开始时采样——此时玩家回合内将生效的 buff 已就位（applyStartOfTurnPowers 已执行）。
     */
    private static void sampleNewTurn(Combat c, int turnNo) {
        if (turnNo < 1) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            return;
        }
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
                com.megacrit.cardcrawl.monsters.MonsterGroup g =
                        AbstractDungeon.getCurrRoom() != null ? AbstractDungeon.getCurrRoom().monsters : null;
                if (g != null && g.monsters != null) {
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

    // ==================== 归属工具 ====================

    /** 栈顶牌（无牌时为 null）。 */
    public static AbstractCard stackTopCard() {
        Frame top = stack.peek();
        return top == null ? null : top.card;
    }

    /**
     * 激发归属覆盖牌（默认 null = 不覆盖，走骰子自身的 sources）。
     *
     * 用于「额外激发场上已有骰子」的牌（Seething/Blitzkrieg）：这些骰子的 sources
     * 指向当初产它们的牌，但本次激发的伤害是因激发牌而生的，应记在激发牌头上。
     * 由 EvokeAllDiceAction 在激发前设置、激发后清除（try/finally），
     * DiceAttribution.of 读取。只影响统计归属，不改游戏逻辑。
     */
    private static AbstractCard evokeAttribution = null;

    /** 供 DiceAttribution.of 读取本次激发的归属牌。 */
    public static AbstractCard currentEvoker() {
        return enabled ? evokeAttribution : null;
    }

    /** 设置/清除激发归属牌（null = 清除）。 */
    public static void setEvokeAttribution(AbstractCard c) {
        evokeAttribution = c;
    }

    /**
     * 魂火跟班（SoulAlly）的贡献权重台账：贡献牌 → 该牌累计提供的魂火值。
     *
     * 用户定口径：唤魂 X 的牌，打出时若场上无魂火则召唤 1 只（记为贡献 1 点魂火值），
     * 否则给每只魂火 +X 点魂火值（记为贡献 X 点）。魂火后续造成的伤害/格挡，
     * 按各牌的累计贡献权重**加权均分**。
     *
     * 权重在 EvokeSoulAction 里累加（那是唯一的唤魂入口）；结算在
     * attributeSoul() 里按权重分。只统计伤害与格挡（用户选定范围）。
     */
    private static final Map<AbstractCard, Integer> soulWeights = new java.util.LinkedHashMap<>();

    /** 记一次唤魂贡献（X 点魂火值）。由 EvokeSoulAction 调用。 */
    public static void onSoulContribute(AbstractCard card, int amount) {
        if (!enabled || card == null || amount <= 0) {
            return;
        }
        soulWeights.merge(card, amount, Integer::sum);
    }

    /**
     * 魂火产出的伤害/格挡结算：按 soulWeights 加权均分给各贡献牌。
     * 无贡献记录（魂火来自遗物/环境）→ 进无归属桶。
     */
    private static void attributeSoul(int amount, boolean isDamage, int act) {
        int totalW = 0;
        for (int w : soulWeights.values()) {
            totalW += w;
        }
        if (totalW <= 0) {
            if (isDamage) {
                unattributedSoulDamage += amount;
            } else {
                unattributedSoulBlock += amount;
            }
            return;
        }
        int idx = 0, assigned = 0, total = soulWeights.size();
        for (Map.Entry<AbstractCard, Integer> e : soulWeights.entrySet()) {
            idx++;
            // 最后一笔收尾，避免整数除法的余数丢失
            long share = (idx == total) ? (amount - assigned)
                    : (long) amount * e.getValue() / totalW;
            assigned += share;
            if (share <= 0) {
                continue;
            }
            CardInstance ins = getOrCreate(e.getKey());
            if (isDamage) {
                ins.dmg[act] += share;
                if (currentCombat.enemyCount > 1) {
                    ins.dmgMulti[act] += share;
                } else {
                    ins.dmgSingle[act] += share;
                }
            } else {
                ins.block[act] += share;
            }
        }
    }

    /** 清空魂火权重台账（战斗开始时调用——魂火不跨战斗存活）。 */
    private static void clearSoulWeights() {
        soulWeights.clear();
    }

    /**
     * 魂火牌事务的队列水位（-1 = 无进行中的魂火牌事务）。
     *
     * 不能用「begin/end 包住 card.use()」的布尔窗口：AllyPlayCardAction 里的
     * card.use() 只负责**入队** action（如 dmgAct → addToBot(DamageAction)），
     * 伤害真正结算在几帧之后，那时 use() 早已返回、窗口已关 → 归不到魂火头上。
     * 故改用与玩家卡牌栈同构的水位机制：记下入队前的队列长度，等队列回落到
     * 该长度即事务结束，由 update() 每帧检查后清除。
     */
    private static int soulWatermark = -1;

    /** 标记魂火牌事务开始（AllyPlayCardAction 在 card.use() 前调用）。 */
    public static void beginSoulDamage() {
        if (!enabled) {
            return;
        }
        GameActionManager am = AbstractDungeon.actionManager;
        soulWatermark = (am == null) ? 0 : am.actions.size();
    }

    /** 本轮伤害是否算在魂火头上（事务进行中且非模拟）。 */
    public static boolean inSoulDamage() {
        return enabled && soulWatermark >= 0;
    }

    /** 每帧检查：队列回落到水位 → 魂火事务结束。 */
    private static void tickSoulWindow() {
        if (soulWatermark < 0) {
            return;
        }
        GameActionManager am = AbstractDungeon.actionManager;
        if (am == null || am.actions.size() <= soulWatermark) {
            soulWatermark = -1;
        }
    }

    /** 诊断桶：无归属的魂火伤害/格挡（魂火来源无贡献记录时）。 */
    private static long unattributedSoulDamage = 0;
    private static long unattributedSoulBlock = 0;

    /** 魂火跟班获得格挡（AbstractCreature.addBlock Postfix，__instance 是 SoulAlly）。 */
    public static void onSoulBlock(int amount) {
        if (!enabled || amount <= 0 || currentCombat == null) {
            return;
        }
        currentCombat.blockGained += amount;
        attributeSoul(amount, false, currentCombat.act);
    }

    private static CardInstance getOrCreate(AbstractCard c) {
        CardInstance ins = instances.get(c.uuid);
        if (ins == null) {
            ins = new CardInstance(c);
            // 同 ID 实例编号
            int n = 0;
            for (CardInstance other : instances.values()) {
                if (other.cardID.equals(c.cardID)) {
                    n = Math.max(n, other.copyIndex + 1);
                }
            }
            ins.copyIndex = n;
            // v2 模块 2：此刻手里有活牌，当场定包并缓存（导出时按 ID 回查会失败）
            ins.packageTag = cardPackageOf(c.cardID, c);
            instances.put(c.uuid, ins);
        }
        return ins;
    }

    /**
     * 骰子伤害/格挡归属：记入骰子统计行，并按 sources 均分给来源牌。
     * sources 为空（遗物/环境产骰）→ 无归属桶（用户已确认遗物不归属）。
     */
    private static void attributeDice(DiceAttribution att, int amount, boolean isDamage, int act) {
        DiceStat st = diceStats.get(att.diceId + "#" + act);
        if (st == null) {
            st = new DiceStat(att.diceId, att.diceName, act);
            diceStats.put(att.diceId + "#" + act, st);
        }
        List<AbstractCard> srcs = att.sources;
        if (srcs == null || srcs.isEmpty()) {
            if (isDamage) {
                st.dmg += amount;
                unattributedDiceDamage += amount;
            } else {
                st.block += amount;
                unattributedBlock += amount;
            }
            return;
        }
        long per = amount / srcs.size();
        int rem = amount % srcs.size();
        boolean multi = currentCombat.enemyCount > 1;
        for (int i = 0; i < srcs.size(); i++) {
            AbstractCard c = srcs.get(i);
            if (c == null) {
                continue;
            }
            long share = per + (i < rem ? 1 : 0);
            if (share <= 0) {
                continue;
            }
            CardInstance ins = getOrCreate(c);
            if (isDamage) {
                st.dmg += share;
                ins.dmg[act] += share;
                if (multi) {
                    ins.dmgMulti[act] += share;
                } else {
                    ins.dmgSingle[act] += share;
                }
            } else {
                st.block += share;
                ins.block[act] += share;
            }
            // 来源构成按牌名聚合（同 ID 多张合并为一行，与卡牌统计节口径一致）
            st.sourceShare.merge(ins.name, share, Long::sum);
        }
    }

    /**
     * 力敏贡献按累计授予比例分摊给各授予牌。
     *
     * @param granters   授予比例表（uuid → 授予总层数）
     * @param contrib    本次事件中被拆出的力敏加成值
     * @param act        当前幕
     * @param isStrength true=力量（伤害） false=敏捷（格挡）
     * @param sourceCard 产生该伤害/格挡的牌（持恒牌 → 豁免，整份进诊断桶）
     */
    private static void splitContribution(Map<UUID, Integer> granters, long contrib, int act, boolean isStrength,
            AbstractCard sourceCard) {
        // 持恒牌豁免：该 mod 的规则里持恒牌不吃力敏加成
        if (sourceCard != null && sourceCard.tags.contains(AbstractCardEnum.Permanent)) {
            if (isStrength) {
                unattributedStrength += contrib;
            } else {
                unattributedDexterity += contrib;
            }
            return;
        }
        if (granters.isEmpty()) {
            // 力量/敏捷存在但找不到授予记录（环境/遗物授予等）→ 诊断桶
            if (isStrength) {
                unattributedStrength += contrib;
            } else {
                unattributedDexterity += contrib;
            }
            return;
        }
        long totalGrant = 0;
        for (int g : granters.values()) {
            totalGrant += Math.max(0, g);
        }
        if (totalGrant <= 0) {
            if (isStrength) {
                unattributedStrength += contrib;
            } else {
                unattributedDexterity += contrib;
            }
            return;
        }
        long assigned = 0;
        UUID biggest = null;
        int biggestGrant = -1;
        for (Map.Entry<UUID, Integer> e : granters.entrySet()) {
            if (e.getValue() <= 0) {
                continue;
            }
            long share = contrib * e.getValue() / totalGrant;
            assigned += share;
            CardInstance ins = getOrCreateByUuid(e.getKey());
            if (ins != null) {
                if (isStrength) {
                    ins.strengthContrib[act] += share;
                } else {
                    ins.dexterityContrib[act] += share;
                }
            }
            if (e.getValue() > biggestGrant) {
                biggestGrant = e.getValue();
                biggest = e.getKey();
            }
        }
        // 余数（整数除法损耗）归最大授予者，保证加总 = contrib
        long leftover = contrib - assigned;
        if (leftover > 0 && biggest != null) {
            CardInstance ins = getOrCreateByUuid(biggest);
            if (ins != null) {
                if (isStrength) {
                    ins.strengthContrib[act] += leftover;
                } else {
                    ins.dexterityContrib[act] += leftover;
                }
            }
        }
    }

    private static CardInstance getOrCreateByUuid(UUID uuid) {
        return instances.get(uuid);
    }

    /** buff 台账：玩家造成伤害（遍历玩家 buff + 受击目标 debuff）。 */
    private static void ledgerDamageDealt(int amount, AbstractMonster target) {
        for (AbstractPower pw : AbstractDungeon.player.powers) {
            if (pw.ID == null) {
                continue;
            }
            PowerLedger L = ledgers.get(pw.ID);
            if (L != null) {
                L.damageDuring += amount;
            }
        }
        if (target != null) {
            for (AbstractPower pw : target.powers) {
                if (pw.type == AbstractPower.PowerType.DEBUFF && pw.ID != null) {
                    PowerLedger L = ledgers.get(pw.ID);
                    if (L != null) {
                        L.damageDealtToTarget += amount;
                    }
                }
            }
            ledgerBlockSaved(amount, target);
        }
    }

    /**
     * 防御性 buff 的格挡作用估算：怪物身上的减伤 debuff（如虚弱 ×0.75、缓速等）
     * 让本次命中少打了多少。用 atDamageReceive(1, NORMAL) 探测每个 buff 的乘法
     * 修正（虚弱 0.75 → 倍率 m=0.75），从结算值反推：未减伤伤害 ≈ output/m，
     * 该 buff 挡掉的 ≈ output*(1/m − 1)。纯乘法关系，精确到本次命中。
     *
     * <p>v2 修正：原用 atDamageGive 探测「目标造成的伤害修正」——怪物身上的 Weakened
     * 会以 0.75 被误判成替玩家挡伤；改 atDamageReceive（目标受到的伤害修正）语义才正确。
     * 本版本怪物防御 debuff 几乎不覆写它，该桶接近 0 属修正而非回归。
     */
    private static void ledgerBlockSaved(int amount, AbstractMonster target) {
        // 先算所有修正 buff 的综合倍率与净修正
        float mult = 1f;
        float add = 0f;
        List<AbstractPower> modifiers = new ArrayList<>();
        for (AbstractPower pw : target.powers) {
            if (pw.ID == null) {
                continue;
            }
            float probe = pw.atDamageReceive(1f, DamageInfo.DamageType.NORMAL);
            if (probe != 1f) {
                modifiers.add(pw);
                if (probe > 0) {
                    mult *= probe;   // 乘法修正（虚弱 0.75 减伤；增伤型同样进倍率）
                } else {
                    add += probe - 1f; // 加法（罕见）
                }
            }
        }
        if (modifiers.isEmpty()) {
            return;
        }
        // 反推未修正伤害：output = (base + add) * mult → base = output/mult − add
        float base = amount;
        if (Math.abs(mult) > 0.01f) {
            base = amount / mult - add;
        }
        if (base <= 0) {
            return;
        }
        // 按"该 buff 单独贡献的修正比例"分摊挡掉的伤害
        for (AbstractPower pw : modifiers) {
            PowerLedger L = ledgers.get(pw.ID);
            if (L == null) {
                continue;
            }
            float probe = pw.atDamageReceive(1f, DamageInfo.DamageType.NORMAL);
            float saved;
            if (probe > 0 && probe < 1f) {
                saved = base * (1f - probe); // 乘法减伤：base × (1 − 0.75)
            } else {
                saved = 0; // 增伤/加法型：不产生格挡作用
            }
            if (saved > 0) {
                L.blockSaved += (long) saved;
            }
        }
    }

    // ==================== 导出 ====================

    public static List<Combat> combats() {
        return combats;
    }

    public static Map<UUID, CardInstance> instances() {
        return instances;
    }

    public static Combat current() {
        return currentCombat;
    }

    private static void export() {
        // 死亡/异常结束时未收尾的战斗（如死亡场）在此补齐
        if (currentCombat != null) {
            finalizeCombat(currentCombat, AbstractDungeon.player.currentHealth);
            currentCombat = null;
        }
        // 诊断：导出时栈内残留
        if (!stack.isEmpty()) {
            forcedPops += stack.size();
            stack.clear();
        }

        boolean victory = AbstractDungeon.victoryScreen != null;
        StringBuilder sb = new StringBuilder();
        sb.append("# DoubleSS 战斗统计\n\n");
        sb.append("**生成时间**: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n\n");

        // ---- 运行信息 ----
        sb.append("## 运行信息\n\n");
        sb.append("- **角色**: ").append(AbstractDungeon.player.name).append('\n');
        sb.append("- **飞升**: A").append(AbstractDungeon.ascensionLevel).append('\n');
        sb.append("- **结果**: ").append(victory ? "胜利" : "死亡").append('\n');
        // 本版本 actNum 为 1 起始（第一幕=1），直接显示
        sb.append("- **到达**: 第").append(AbstractDungeon.actNum).append("幕 第").append(AbstractDungeon.floorNum).append("层\n");
        sb.append("- **统计开关**: ").append(enabled ? "开" : "关").append("\n\n");

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

        // ---- 卡牌汇总（按 卡ID × 升级/未升级，每幕） ----
        for (int actRaw = 0; actRaw < ACTS; actRaw++) {
            final int act = actRaw;
            Map<String, List<CardInstance>> groups = new HashMap<>();
            for (CardInstance ins : instances.values()) {
                if (activeInAct(ins, act)) {
                    String key = ins.cardID + (ins.upgradedEver ? "#up" : "#base");
                    groups.computeIfAbsent(key, k -> new ArrayList<>()).add(ins);
                }
            }
            if (groups.isEmpty()) {
                continue;
            }
            sb.append("## 卡牌统计-第").append(act + 1).append("幕\n\n");
            List<Map.Entry<String, List<CardInstance>>> entries = new ArrayList<>(groups.entrySet());
            entries.sort((a, b) -> Long.compare(
                    b.getValue().stream().mapToLong(i -> i.dmg[act]).sum(),
                    a.getValue().stream().mapToLong(i -> i.dmg[act]).sum()));
            for (Map.Entry<String, List<CardInstance>> e : entries) {
                List<CardInstance> list = e.getValue();
                long sumPlays = 0, sumDmg = 0, sumBlock = 0, sumStrC = 0, sumDexC = 0;
                double ppr = 0;
                for (CardInstance ins : list) {
                    sumPlays += ins.plays[act];
                    sumDmg += ins.dmg[act];
                    sumBlock += ins.block[act];
                    sumStrC += ins.strengthContrib[act];
                    sumDexC += ins.dexterityContrib[act];
                    ppr += playsPerRound(ins, act);
                }
                boolean up = e.getKey().endsWith("#up");
                String shownName = list.get(0).name;
                sb.append("### **").append(shownName).append(up ? "(升级)" : "(未升级)").append("**\n\n");
                sb.append("- **张数**: ").append(list.size())
                        .append("  **总打出**: ").append(sumPlays)
                        .append("  **均伤/次**: ").append(ave(sumDmg, sumPlays))
                        .append("  **均格挡/次**: ").append(ave(sumBlock, sumPlays))
                        .append("  **平均每轮打出**: ").append(fmt(ppr / list.size())).append('\n');
                sb.append("- **力量贡献**: ").append(sumStrC)
                        .append("  **敏捷贡献**: ").append(sumDexC).append("\n\n");
                appendCardGroupStats(sb, list, act);
            }
        }

        // ---- 卡牌汇总（按 卡ID × 升级/未升级，全局） ----
        {
            Map<String, List<CardInstance>> groups = new HashMap<>();
            for (CardInstance ins : instances.values()) {
                if (hasAnyStat(ins)) {
                    String key = ins.cardID + (ins.upgradedEver ? "#up" : "#base");
                    groups.computeIfAbsent(key, k -> new ArrayList<>()).add(ins);
                }
            }
            if (!groups.isEmpty()) {
                sb.append("## 卡牌汇总-全局\n\n");
                List<Map.Entry<String, List<CardInstance>>> entries = new ArrayList<>(groups.entrySet());
                entries.sort((a, b) -> Long.compare(
                        b.getValue().stream().mapToLong(i -> totalDmg(i)).sum(),
                        a.getValue().stream().mapToLong(i -> totalDmg(i)).sum()));
                for (Map.Entry<String, List<CardInstance>> e : entries) {
                    List<CardInstance> list = e.getValue();
                    long sumPlays = 0, sumDmg = 0, sumBlock = 0, sumStrC = 0, sumDexC = 0, sumStrG = 0, sumDexG = 0;
                    double ppr = 0;
                    for (CardInstance ins : list) {
                        sumPlays += totalPlays(ins);
                        sumDmg += totalDmg(ins);
                        sumBlock += totalBlock(ins);
                        sumStrC += total(ins.strengthContrib);
                        sumDexC += total(ins.dexterityContrib);
                        sumStrG += total(ins.strengthGrant);
                        sumDexG += total(ins.dexterityGrant);
                        ppr += playsPerRoundAll(ins);
                    }
                    boolean up = e.getKey().endsWith("#up");
                    String shownName = list.get(0).name;
                    sb.append("### **").append(shownName).append(up ? "(升级)" : "(未升级)").append("**\n\n");
                    sb.append("- **张数**: ").append(list.size())
                            .append("  **总打出**: ").append(sumPlays)
                            .append("  **均伤/次**: ").append(ave(sumDmg, sumPlays))
                            .append("  **均格挡/次**: ").append(ave(sumBlock, sumPlays))
                            .append("  **平均每轮打出**: ").append(fmt(ppr / list.size())).append('\n');
                    sb.append("- **力量**: 授予 ").append(sumStrG).append("  贡献 ").append(sumStrC)
                            .append("  **敏捷**: 授予 ").append(sumDexG).append("  贡献 ").append(sumDexC).append("\n\n");
                    appendCardGroupStats(sb, list, null);
                }
            }
        }

        // ---- 卡包综合审计（v2 模块 2）----
        {
            String[] pkgs = { "七宗罪", "Hao", "Lost", "Shock", "C6H14", "本体" };
            // 牌库 = 本局拥有过的牌，按 cardID 去重（整局口径）。
            // 不用末场 deckAtStart：那样后期拿到/已删掉的牌会整个看不见（用户实测反馈）。
            Map<String, Integer> deckByPkg = new HashMap<>();
            int deckTotal = 0;
            java.util.HashSet<String> seenDeckIds = new java.util.HashSet<>();
            for (CardInstance ci : instances.values()) {
                if (seenDeckIds.add(ci.cardID)) {
                    deckByPkg.merge(packageOf(ci), 1, Integer::sum);
                    deckTotal++;
                }
            }
            Map<String, long[]> agg = new HashMap<>(); // [dmg, block, plays, energyNet, drawn]
            for (CardInstance ci : instances.values()) {
                long[] a = agg.computeIfAbsent(packageOf(ci),
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
            long allCardDmg = 0, allCardBlock = 0, allDiceDmg = 0;
            for (long[] a : agg.values()) {
                allCardDmg += a[0];
                allCardBlock += a[1];
            }
            for (Long v : diceByPkg.values()) {
                allDiceDmg += v;
            }
            long totalDmgAll = allCardDmg + allDiceDmg;
            // 整局口径：a[2] 已是整局总打出（见上方 agg 循环），此处只需整局总打出做分母。
            long allPlaysTotal = 0;
            for (long[] a : agg.values()) {
                allPlaysTotal += a[2];
            }
            final double avgPlaysPerCard = deckTotal <= 0 ? 0 : (double) allPlaysTotal / deckTotal;
            sb.append("## 卡包综合审计（全局）\n\n");
            sb.append("> 全表为**整局**口径：牌库 = 本局拥有过的牌（去重），打出 = 整局打出次数。\n");
            sb.append("> 稀释件 = 该包每张牌平均打出次数 < 全场每张牌均值×0.5（两列同量纲）。\n\n");
            for (String pkg : pkgs) {
                long[] a = agg.getOrDefault(pkg, new long[5]);
                long dDmg = diceByPkg.getOrDefault(pkg, 0L);
                int deckN = deckByPkg.getOrDefault(pkg, 0);
                double deckShare = deckTotal <= 0 ? 0 : 100.0 * deckN / deckTotal;
                double playShare = allPlaysTotal <= 0 ? 0 : 100.0 * a[2] / allPlaysTotal;
                StringBuilder line = new StringBuilder("- **").append(pkg).append("**: ");
                line.append("伤害占比 ").append(pct(a[0] + dDmg, totalDmgAll))
                        .append("（卡 ").append(a[0]).append(" + 骰 ").append(dDmg).append("）")
                        .append("  格挡占比 ").append(pct(a[1], allCardBlock))
                        .append("  净能量 ").append(a[3])
                        .append("  净过牌 ").append(a[2] <= 0 ? "-" : fmt((double) a[4] / a[2] - 1))
                        .append("  牌库 ").append(deckN).append("张(").append(fmt(deckShare)).append("%)")
                        .append("  打出 ").append(a[2]).append("次(").append(fmt(playShare)).append("%)");
                // 稀释件：该包「每张牌平均打出次数」比「全场每张牌平均打出次数」。
                // 两个量纲必须都是「每张牌的次数」——直接比两个占比会因包大小不同而失真
                // （包内张数越多、上限越高，小结包会被系统性漏标）。
                double playsPerDeckCard = deckN <= 0 ? 0 : (double) a[2] / deckN;
                double ratio = avgPlaysPerCard <= 0 ? 1 : playsPerDeckCard / avgPlaysPerCard;
                if (deckN > 0 && ratio < 0.5) {
                    line.append("  ⚠卡组稀释件");
                }
                sb.append(line).append('\n');
            }
            sb.append("- **机制交互**: 骰伤害占全场 ").append(pct(allDiceDmg, totalDmgAll)).append('\n');
        }

        // ---- 骰子统计（按幕） ----
        for (int act = 0; act < ACTS; act++) {
            List<DiceStat> list = new ArrayList<>();
            for (DiceStat d : diceStats.values()) {
                if (d.act == act && (d.dmg > 0 || d.block > 0 || d.produced > 0)) {
                    list.add(d);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            list.sort(Comparator.comparingLong((DiceStat d) -> d.dmg + d.block).reversed());
            sb.append("## 骰子统计-第").append(act + 1).append("幕\n\n");
            for (DiceStat d : list) {
                sb.append("### **").append(d.name).append("**\n\n");
                sb.append("- **产出数量**: ").append(d.produced)
                        .append("  **总伤害**: ").append(d.dmg)
                        .append("  **总格挡**: ").append(d.block).append('\n');
                sb.append("- **均伤/骰**: ").append(ave(d.dmg, d.produced))
                        .append("  **均格挡/骰**: ").append(ave(d.block, d.produced)).append('\n');
                long sum = 0;
                List<Map.Entry<String, Long>> shares = new ArrayList<>(d.sourceShare.entrySet());
                shares.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
                for (Map.Entry<String, Long> s : shares) {
                    sum += s.getValue();
                }
                if (sum > 0) {
                    StringBuilder line = new StringBuilder("- **来源构成**: ");
                    int shown = 0;
                    for (Map.Entry<String, Long> s : shares) {
                        if (shown++ > 0) {
                            line.append(" / ");
                        }
                        line.append(s.getKey()).append(" ").append(s.getValue() * 100 / sum).append("%");
                        if (shown >= 4) {
                            break;
                        }
                    }
                    if (shares.size() > 4) {
                        line.append(" / 其他");
                    }
                    sb.append(line).append("\n\n");
                } else {
                    sb.append("- **来源构成**: (无, 遗物/环境产骰)\n\n");
                }
            }
        }

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
        sb.append('\n');

        // ---- 诊断 ----
        sb.append("## 诊断\n\n");
        sb.append("- 无归属伤害(栈空且非骰子): ").append(unattributedDamage).append('\n');
        sb.append("- 无归属骰子伤害(sources为空, 遗物/环境产骰): ").append(unattributedDiceDamage).append('\n');
        sb.append("- 无归属格挡(栈空且非骰子): ").append(unattributedBlock).append('\n');
        sb.append("- 未分摊力量贡献(持恒豁免或无授予记录): ").append(unattributedStrength).append('\n');
        sb.append("- 未分摊敏捷贡献(持恒豁免或无授予记录): ").append(unattributedDexterity).append('\n');
        sb.append("- 未跟踪伤害事件(战斗外): ").append(untrackedDamageEvents).append('\n');
        sb.append("- 卡牌栈强制弹出(异常, 正常应为0): ").append(forcedPops).append('\n');
        sb.append("- 无归属能量增加(无栈顶且本回合未出牌): ").append(unattributedEnergyGain).append('\n');
        sb.append("- 无归属抽牌(开局首抽/无栈顶): ").append(unattributedDraws).append('\n');
        sb.append("- 无归属魂火伤害(无唤魂贡献记录): ").append(unattributedSoulDamage).append('\n');
        sb.append("- 无归属魂火格挡(无唤魂贡献记录): ").append(unattributedSoulBlock).append('\n');

        writeToFile(sb.toString());
        clearSnapshotFile(); // 本局已导出，快照作废（防止后续 SL 再恢复已导出的数据）
    }

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

    /** 百分比（分母 0 → "-"）。 */
    private static String pct(long num, long den) {
        return den <= 0 ? "-" : fmt(100.0 * num / den) + "%";
    }

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

    /**
     * cardID → 卡包短名，收容**不进 cardParentMap** 的两类牌（惰性构建）。
     *
     * cardParentMap 只在 AbstractPackage.initializePack() 里写入，而该方法只遍历
     * getCards()。因此以下两类牌永远缺席，只查 cardParentMap 会把它们全归成「本体」：
     * - 协同卡：addPairCard 声明，走 modcore.buildPairCards() → pairCardPool；
     * - 起手牌：getStarterCard() 返回值，不在任何 getCards() 列表里。
     * 归一包多声明同一张卡时取先声明者（当前无此情况）。
     */
    private static Map<String, String> extraCardPkg = null;

    private static String extraPackageOf(String cardID) {
        if (cardID == null) {
            return null;
        }
        if (extraCardPkg == null) {
            extraCardPkg = new HashMap<>();
            SS.modcore.modcore.ensurePackages(); // 保险：确保 mainPackageList 已填充
            for (SS.packages.AbstractPackage p : SS.modcore.modcore.mainPackageList) {
                if (p == null) {
                    continue;
                }
                String shortName = packageNameOf(p.ID);
                if (p.pairCards != null) {
                    for (String cid : p.pairCards.values()) {
                        if (cid != null) {
                            extraCardPkg.putIfAbsent(cid, shortName);
                        }
                    }
                }
                String starter = p.getStarterCard();
                if (starter != null) {
                    extraCardPkg.putIfAbsent(starter, shortName);
                }
            }
        }
        return extraCardPkg.get(cardID);
    }

    /**
     * 取实例的包归属。缓存为空时（SL 读档恢复的旧实例走不到 getOrCreate）现算一次并回填。
     * 回填用 liveCardOf——查不到活牌就只剩 cardParentMap 一条路，七宗罪卡会落到「本体」，
     * 属读档路径的已知近似（v1 快照本就没有 packageTag）。
     */
    private static String packageOf(CardInstance ci) {
        if (ci.packageTag == null) {
            ci.packageTag = cardPackageOf(ci.cardID, liveCardOf(ci.cardID));
        }
        return ci.packageTag;
    }

    /**
     * 牌归包，优先级：Sins tag → 七宗罪；cardParentMap → 对应包；协同卡表 → 声明它的包；其余 → 本体。
     * sample = 该 ID 的一张活牌（读 tag，可 null）。
     */
    private static String cardPackageOf(String cardID, AbstractCard sample) {
        if (sample != null && sample.hasTag(AbstractCardEnum.Sins)) {
            return "七宗罪";
        }
        String pkg = SS.modcore.modcore.cardParentMap.get(cardID);
        if (pkg != null) {
            return packageNameOf(pkg);
        }
        // 协同卡 / 起手牌：不在 cardParentMap 里，走补全反查表（按声明它的卡包归包）
        String extra = extraPackageOf(cardID);
        return extra != null ? extra : "本体";
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

    private static boolean activeInAct(CardInstance ins, int act) {
        return ins.plays[act] > 0 || ins.dmg[act] > 0 || ins.block[act] > 0
                || ins.strengthGrant[act] > 0 || ins.dexterityGrant[act] > 0
                || ins.strengthContrib[act] > 0 || ins.dexterityContrib[act] > 0;
    }

    private static boolean hasAnyStat(CardInstance ins) {
        for (int a = 0; a < ACTS; a++) {
            if (activeInAct(ins, a)) {
                return true;
            }
        }
        return false;
    }

    private static long totalDmg(CardInstance i) {
        return total(i.dmg);
    }

    private static long totalBlock(CardInstance i) {
        return total(i.block);
    }

    private static int totalPlays(CardInstance i) {
        int s = 0;
        for (int a = 0; a < ACTS; a++) {
            s += i.plays[a];
        }
        return s;
    }

    private static long total(long[] arr) {
        long s = 0;
        for (long v : arr) {
            s += v;
        }
        return s;
    }

    /** 每轮打出 = 幕内每场战斗(该实例打出数/回合数)的平均。 */
    private static double playsPerRound(CardInstance ins, int act) {
        double sum = 0;
        int n = 0;
        for (Combat c : combats) {
            if (c.act != act || !c.finalized) {
                continue;
            }
            int plays = c.playsByCard.getOrDefault(ins.uuid, 0);
            sum += (double) plays / c.rounds();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    /** 全局每轮打出 = 所有战斗(该实例打出数/回合数)的平均。 */
    private static double playsPerRoundAll(CardInstance ins) {
        double sum = 0;
        int n = 0;
        for (Combat c : combats) {
            if (!c.finalized) {
                continue;
            }
            int plays = c.playsByCard.getOrDefault(ins.uuid, 0);
            sum += (double) plays / c.rounds();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    private static String ave(long total, long count) {
        return count <= 0 ? "-" : fmt((double) total / count);
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    /** 统计文件最多保留的场数（超出后从最旧开始清理）。 */
    private static final int KEEP_FILES = 15;

    private static void writeToFile(String content) {
        try {
            File dir = new File(Gdx.files.local("analysis").path());
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("[DoubleSS] 创建 analysis 目录失败: " + dir.getAbsolutePath());
                return;
            }
            String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".md";
            File file = new File(dir, name);
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                w.write(content);
            }
            System.out.println("[DoubleSS] 战斗统计已导出: " + file.getAbsolutePath());
            pruneOldFiles(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 自动清理：只保留最近 KEEP_FILES 个统计文件（按文件名排序，yyyyMMdd_HHmmss
     * 字典序 = 时间序；txt 与 md 一并纳入，旧格式文件会被逐步挤出）。
     * 清理失败不影响导出。
     */
    private static void pruneOldFiles(File dir) {
        try {
            File[] files = dir.listFiles(f -> f.isFile() && f.getName().matches("\\d{8}_\\d{6}\\.(txt|md)"));
            if (files == null || files.length <= KEEP_FILES) {
                return;
            }
            java.util.Arrays.sort(files, Comparator.comparing(File::getName));
            int removed = 0;
            for (int i = 0; i < files.length - KEEP_FILES; i++) {
                if (files[i].delete()) {
                    removed++;
                }
            }
            if (removed > 0) {
                System.out.println("[DoubleSS] 自动清理了 " + removed + " 个旧统计文件（保留最近 " + KEEP_FILES + " 个）");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
