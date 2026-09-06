package SS.stats;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
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

        public PowerLedger(String powerId, boolean isDebuff) {
            this.powerId = powerId;
            this.isDebuff = isDebuff;
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

            JsonObject inst = new JsonObject();
            for (CardInstance ci : instances.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", ci.uuid.toString());
                o.addProperty("id", ci.cardID);
                o.addProperty("name", ci.name);
                o.addProperty("up", ci.upgradedEver);
                o.addProperty("ci", ci.copyIndex);
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
        Combat c = new Combat();
        // 本版本 actNum 是 1 起始（dungeonTransitionSetup 开局 ++actNum），索引用 actNum-1
        c.act = Math.min(Math.max(AbstractDungeon.actNum - 1, 0), ACTS - 1);
        c.roomType = room instanceof MonsterRoomBoss ? 2 : room instanceof MonsterRoomElite ? 1 : 0;
        c.enemyCount = (room.monsters != null && room.monsters.monsters != null) ? room.monsters.monsters.size() : 0;
        c.hpStart = AbstractDungeon.player.currentHealth;
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
        c.finalized = true;
    }

    // ==================== 事件入口（由 patch 调用） ====================

    /** 出牌（AbstractPlayer.useCard Prefix，入栈 + 计数）。 */
    public static void onPlay(AbstractCard c) {
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
    }

    /** 玩家对怪物造成伤害（AbstractMonster.damage Postfix，info.owner==player）。 */
    public static void onMonsterDamage(DamageInfo info, AbstractMonster target) {
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

        // 1) 骰子伤害：SpireField（单目标）或 AOE 静态上下文（HitAll / NextTurnDamagePower 延迟 AOE）
        DiceAttribution att = DamageInfoDiceSource.diceRef.get(info);
        if (att == null) {
            att = aoeDice;
        }
        if (att != null) {
            attributeDice(att, amount, true, act);
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

        // 1) 骰子格挡：GainBlockAction 上的静态上下文
        DiceAttribution att = blockDice;
        if (att != null) {
            attributeDice(att, amount, false, act);
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
        if (playerOwned || monsterDebuff) {
            boolean debuff = power.type == AbstractPower.PowerType.DEBUFF;
            PowerLedger L = ledgers.get(power.ID);
            if (L == null) {
                L = new PowerLedger(power.ID, debuff);
                ledgers.put(power.ID, L);
            }
            if (power.amount > 0) {
                L.stacksApplied += power.amount;
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
        GameActionManager am = AbstractDungeon.actionManager;
        if (am == null) {
            return;
        }
        // 回合计数：turn 跨战斗累积，取本场最大值
        if (currentCombat != null && GameActionManager.turn > currentCombat.maxTurn) {
            currentCombat.maxTurn = GameActionManager.turn;
        }
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

    // ==================== 归属工具 ====================

    /** 栈顶牌（无牌时为 null）。 */
    public static AbstractCard stackTopCard() {
        Frame top = stack.peek();
        return top == null ? null : top.card;
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
            String label = displayName(ins, act);
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
            st.sourceShare.merge(label, share, Long::sum);
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
     * 防御性 buff 的格挡作用估算：怪物身上的减伤 debuff（如虚弱 atDamageGive ×0.75、
     * 缓速等）让本次命中少打了多少。用 atDamageGive(1, type) 探测每个 buff 的乘法
     * 修正（虚弱 0.75 → 倍率 m=0.75），从结算值反推：未减伤伤害 ≈ output/m，
     * 该 buff 挡掉的 ≈ output*(1/m − 1)。纯乘法关系，精确到本次命中。
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
            float probe = pw.atDamageGive(1f, DamageInfo.DamageType.NORMAL);
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
            float probe = pw.atDamageGive(1f, DamageInfo.DamageType.NORMAL);
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

        // ---- 战斗统计（按幕） ----
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
                for (Combat c : rc) {
                    net += c.netHpLoss();
                    taken += c.damageTaken;
                    turns += c.rounds();
                }
                sb.append("- **").append(roomNames[rt]).append("**: 场数 ").append(rc.size())
                        .append("  平均净HP损失 ").append(fmt(net / rc.size()))
                        .append("  平均受伤 ").append(fmt(taken / rc.size()))
                        .append("  平均回合 ").append(fmt(turns / rc.size())).append('\n');
            }
            sb.append('\n');
        }

        // ---- 卡牌统计（按幕、按实例） ----
        for (int actRaw = 0; actRaw < ACTS; actRaw++) {
            final int act = actRaw;
            List<CardInstance> rows = new ArrayList<>();
            for (CardInstance ins : instances.values()) {
                if (activeInAct(ins, act)) {
                    rows.add(ins);
                }
            }
            if (rows.isEmpty()) {
                continue;
            }
            rows.sort(Comparator.comparingLong((CardInstance i) -> i.dmg[act]).reversed()
                    .thenComparing(Comparator.comparingInt((CardInstance i) -> i.plays[act]).reversed()));
            sb.append("## 卡牌统计-第").append(act + 1).append("幕-按实例\n\n");
            for (CardInstance ins : rows) {
                appendCardBlock(sb, ins, act);
            }
            sb.append("> 注: 均伤=总伤害/打出次数, 伤害为结算后、扣格挡前的\"打出值\"; 群怪=开战时敌人>=2的战斗;\n");
            sb.append("> 每轮打出=对幕内每场战斗 打出次数/回合数 求平均; 升级=本局内曾升级过;\n");
            sb.append("> 力量授予=本牌直接施加的力量层数; 力量贡献=命中中力量加成按授予比例分摊到本牌的点数;\n");
            sb.append("> 敏捷同理。骰子伤害/格挡按 sources 均分到来源牌并计入本行。持恒牌不吃力敏(豁免)。\n\n");
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
            sb.append("## 卡牌汇总-第").append(act + 1).append("幕-按卡ID分组\n\n");
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
                String baseId = e.getKey().split("#")[0];
                boolean up = e.getKey().endsWith("#up");
                sb.append("### **").append(baseId).append(up ? "(升级)" : "(未升级)").append("**\n\n");
                sb.append("- **张数**: ").append(list.size())
                        .append("  **总打出**: ").append(sumPlays)
                        .append("  **均伤/次**: ").append(ave(sumDmg, sumPlays))
                        .append("  **均格挡/次**: ").append(ave(sumBlock, sumPlays))
                        .append("  **平均每轮打出**: ").append(fmt(ppr / list.size())).append('\n');
                sb.append("- **力量贡献**: ").append(sumStrC)
                        .append("  **敏捷贡献**: ").append(sumDexC).append("\n\n");
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
                sb.append("## 卡牌汇总-全局-按卡ID分组\n\n");
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
                    String baseId = e.getKey().split("#")[0];
                    boolean up = e.getKey().endsWith("#up");
                    sb.append("### **").append(baseId).append(up ? "(升级)" : "(未升级)").append("**\n\n");
                    sb.append("- **张数**: ").append(list.size())
                            .append("  **总打出**: ").append(sumPlays)
                            .append("  **均伤/次**: ").append(ave(sumDmg, sumPlays))
                            .append("  **均格挡/次**: ").append(ave(sumBlock, sumPlays))
                            .append("  **平均每轮打出**: ").append(fmt(ppr / list.size())).append('\n');
                    sb.append("- **力量**: 授予 ").append(sumStrG).append("  贡献 ").append(sumStrC)
                            .append("  **敏捷**: 授予 ").append(sumDexG).append("  贡献 ").append(sumDexC).append("\n\n");
                }
            }
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

        // ---- BUFF/DEBUFF 台账 ----
        if (!ledgers.isEmpty()) {
            List<PowerLedger> list = new ArrayList<>(ledgers.values());
            list.sort((a, b) -> Long.compare(
                    b.damageDuring + b.damageTakenDuring + b.damageDealtToTarget + b.blockSaved,
                    a.damageDuring + a.damageTakenDuring + a.damageDealtToTarget + a.blockSaved));
            sb.append("## BUFF/DEBUFF台账\n\n");
            for (PowerLedger L : list) {
                if (L.stacksApplied <= 0 && L.damageDuring <= 0 && L.blockDuring <= 0
                        && L.damageTakenDuring <= 0 && L.damageDealtToTarget <= 0 && L.blockSaved <= 0) {
                    continue;
                }
                sb.append("### **").append(L.powerId).append("**").append(L.isDebuff ? "（debuff）" : "").append("\n\n");
                sb.append("- **授予层数**: ").append(L.stacksApplied).append('\n');
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
                    sb.append("- **格挡作用(约)**: ").append(L.blockSaved).append("（该 debuff 挂在敌人身上时,替玩家挡掉的伤害）\n");
                }
                sb.append('\n');
            }
        }

        // ---- 诊断 ----
        sb.append("## 诊断\n\n");
        sb.append("- 无归属伤害(栈空且非骰子): ").append(unattributedDamage).append('\n');
        sb.append("- 无归属骰子伤害(sources为空, 遗物/环境产骰): ").append(unattributedDiceDamage).append('\n');
        sb.append("- 无归属格挡(栈空且非骰子): ").append(unattributedBlock).append('\n');
        sb.append("- 未分摊力量贡献(持恒豁免或无授予记录): ").append(unattributedStrength).append('\n');
        sb.append("- 未分摊敏捷贡献(持恒豁免或无授予记录): ").append(unattributedDexterity).append('\n');
        sb.append("- 未跟踪伤害事件(战斗外): ").append(untrackedDamageEvents).append('\n');
        sb.append("- 卡牌栈强制弹出(异常, 正常应为0): ").append(forcedPops).append('\n');

        writeToFile(sb.toString());
        clearSnapshotFile(); // 本局已导出，快照作废（防止后续 SL 再恢复已导出的数据）
    }

    /** 单牌信息块（按实例统计节用）。 */
    private static void appendCardBlock(StringBuilder sb, CardInstance ins, int act) {
        sb.append("### **").append(displayName(ins, act)).append("**\n\n");
        sb.append("- **升级**: ").append(ins.upgradedEver ? "是" : "否").append('\n');
        sb.append("- **打出**: ").append(ins.plays[act])
                .append("（单敌 ").append(ins.playsSingle[act])
                .append(" / 群怪 ").append(ins.playsMulti[act]).append("）\n");
        sb.append("- **总伤害**: ").append(ins.dmg[act])
                .append("  **均伤/次**: ").append(ave(ins.dmg[act], ins.plays[act]))
                .append("（单敌 ").append(ave(ins.dmgSingle[act], ins.playsSingle[act]))
                .append(" / 群怪 ").append(ave(ins.dmgMulti[act], ins.playsMulti[act])).append("）\n");
        sb.append("- **总格挡**: ").append(ins.block[act])
                .append("  **均格挡/次**: ").append(ave(ins.block[act], ins.plays[act])).append('\n');
        sb.append("- **每轮打出**: ").append(fmt(playsPerRound(ins, act))).append('\n');
        sb.append("- **力量**: 授予 ").append(ins.strengthGrant[act]).append("  贡献 ").append(ins.strengthContrib[act])
                .append("  **敏捷**: 授予 ").append(ins.dexterityGrant[act]).append("  贡献 ").append(ins.dexterityContrib[act])
                .append("\n\n");
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

    private static String displayName(CardInstance ins, int act) {
        int copies = 0;
        for (CardInstance other : instances.values()) {
            if (other.cardID.equals(ins.cardID)
                    && (other.plays[act] > 0 || other.dmg[act] > 0 || other.block[act] > 0)) {
                copies++;
            }
        }
        return copies > 1 ? ins.name + "#" + (ins.copyIndex + 1) : ins.name;
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
