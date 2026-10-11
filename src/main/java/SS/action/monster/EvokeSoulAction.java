package SS.action.monster;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.action.common.SpawnAllyAction;

import SS.action.unique.GainVirtueAction;
import SS.monster.ally.AbstractAlly;
import SS.monster.ally.AllyManager;
import SS.monster.ally.AllyPositionHelper;
import SS.monster.ally.SoulAlly;
import SS.power.PaintingPower;
import SS.power.SoulFirePower;

public class EvokeSoulAction extends AbstractGameAction {

    public EvokeSoulAction() {
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = 1;
    }

    public EvokeSoulAction(int amount) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
    }

    @Override
    public void update() {
        // 1. 获得 1 点美德
        addToTop(new GainVirtueAction(AbstractDungeon.player, this.amount));
        if (this.amount >= 1) {
            if (AbstractDungeon.player.hasPower("Double:SermonPower")) {
                addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player,
                        new PaintingPower(AbstractDungeon.player, 1)));
            }
        }

        // 2. 统计场上存活的魂火，同时记下参考魂火值。
        //    "参考魂火值" = 场上存活魂火里的最大值，用于给新召唤的魂火对齐 ——
        //    保证补出来的跟班与场上已有的魂火强度一致（用户口径）。
        ArrayList<SoulAlly> aliveSouls = new ArrayList<>();
        int refSoulFire = 0;
        for (AbstractMonster m : AllyManager.allies.monsters) {
            if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                aliveSouls.add((SoulAlly) m);
                AbstractPower p = m.getPower(SoulFirePower.POWER_ID);
                if (p != null && p.amount > refSoulFire) {
                    refSoulFire = p.amount;
                }
            }
        }
        if (refSoulFire <= 0)
            refSoulFire = 1; // 保底

        // 是否处于魂火阵法。后续"熔炼统一数值"和"补齐到 3 只"都只在该阵法下生效。
        boolean inGenesis = SS.helper.EnvironmentManager.isSoulSquadActive();

        // 3a. 已有魂火：升级它们（血量上限 +3·amount，回复等量血）。
        //     increaseMaxHp 内部会触发 heal -> syncSoulFire()，层数自动 +amount。
        for (SoulAlly s : aliveSouls) {
            s.increaseMaxHp(3 * this.amount, true);
        }

        // 3a-2. 【魂火阵法】熔炼：把所有存活魂火的魂火值统一到最高的一只。
        //     阵法里的魂火是一团火，不该有强弱差；且后续补充的跟班也按这个值对齐，
        //     所以必须先在这里收敛，再谈补充，否则 refSoulFire 是从不齐的集合里取的。
        //     （与 Conduct / SoulStorage 的"取场上最高"口径一致。）
        //     注意是补血而非硬改层数：syncSoulFire() 每帧按 ceil(血量/3) 覆写 p.amount，
        //     直接改 p.amount 撑不过一帧。
        if (inGenesis && aliveSouls.size() > 1 && refSoulFire > 0) {
            for (SoulAlly s : aliveSouls) {
                raiseToSoulFire(s, refSoulFire);
            }
        }

        // 3b. 补齐魂火。
        //     【关键】只有创世记阵法的"维持 3 只魂火"是一种阵法效果。
        //     非创世记状态下，一只魂火就是一只，死光了下次唤魂才重新从零召唤 1 只
        //     —— 这才是原来的行为，不能因为补丁顺手就把没开阵法的场合也铺成 3 只。
        int missing = inGenesis ? SOUL_SQUAD_TARGET - aliveSouls.size() : (aliveSouls.isEmpty() ? 1 : 0);
        if (missing > 0) {
            if (aliveSouls.isEmpty()) {
                // 从零开始：只召唤 1 只
                addToBot(new SpawnAllyAction(spawnSoul(0, inGenesis ? refSoulFire : -1)));
            } else {
                // 补齐剩余名额，占用未被保留的槽位
                for (int slot : freeSlots(aliveSouls, missing)) {
                    addToBot(new SpawnAllyAction(spawnSoul(slot, refSoulFire)));
                }
            }
        }

        // 4. 战斗统计：记本次唤魂贡献（召唤 1 只 = 1 点魂火值；否则 = amount 点）。
        //    魂火后续的伤害/格挡按各唤魂牌的累计贡献加权均分（用户定口径）。
        //    栈顶即打出本牌的卡（EvokeSoulAction 在 card.use() 内入队）。
        SS.stats.CardStats.onSoulContribute(SS.stats.CardStats.stackTopCard(),
                aliveSouls.isEmpty() ? 1 : this.amount);

        this.isDone = true;
    }

    /** 创世记阵法维持的魂火数量；与 EnterEvokeSoulEnvAction.SOUL_TARGET 保持一致。 */
    private static final int SOUL_SQUAD_TARGET = 3;

    /**
     * 构造一只待召唤的魂火。
     *
     * @param slot      目标槽位
     * @param soulFire  要继承的魂火值；传 -1 表示不继承（走从零开始的自然成长）
     */
    private static SoulAlly spawnSoul(int slot, int soulFire) {
        SoulAlly s = new SoulAlly(slot);
        if (soulFire > 0) {
            raiseToSoulFire(s, soulFire);
        }
        return s;
    }

    /**
     * 把某只魂火的魂火值提到指定值。
     *
     * <p>
     * <b>只补血，不硬改能力层数。</b> 魂火值的唯一真实来源是血量：
     * {@code SoulAlly.syncSoulFire()} 每帧都会按 {@code ceil(血量/3)} 重算并覆盖
     * {@code p.amount}，所以事后直接改 p.amount 撑不过一帧就被冲掉。
     * 补足血量后让 syncSoulFire() 自己算出目标层数才稳。
     *
     * <p>
     * 只在目标值更高时才抬（不降），避免把修炼出来的魂火削回去。
     * 血量上限由 syncSoulFire() 自行写为 {@code 值×3}。
     */
    private static void raiseToSoulFire(SoulAlly s, int soulFire) {
        AbstractPower p = s.getPower(SoulFirePower.POWER_ID);
        if (p != null && p.amount >= soulFire) {
            return; // 已经不低于目标值，不动
        }
        s.currentHealth = soulFire * SOUL_HP_PER_LEVEL;
        s.syncSoulFire();
    }

    /** 每 1 点魂火对应的血量。与 SoulAlly.syncSoulFire() 的换算一致。 */
    private static final int SOUL_HP_PER_LEVEL = 3;

    /**
     * 把某只魂火的魂火值设为指定值（只动能力层数，不动血量）。
     *
     * <p>
     * 必须【改已有的那个 SoulFirePower】，绝不能再 add 一个新的：SoulAlly 构造函数
     * 末尾的 syncSoulFire() 已经放了一个（层数 1），再 add 会让身上挂两个同名能力，
     * 图标和伤害加成各叠一份。直接改字段而不走 Action 是因为状态必须立刻到位。
     */
    private static void setSoulFire(SoulAlly s, int soulFire) {
        AbstractPower p = s.getPower(SoulFirePower.POWER_ID);
        if (p == null) {
            return;
        }
        p.amount = soulFire;
        p.updateDescription();
    }

    /** 挑出前 count 个未被其它友军占用的槽位（升序）。 */
    private static ArrayList<Integer> freeSlots(ArrayList<SoulAlly> souls, int count) {
        ArrayList<Integer> taken = new ArrayList<>();
        for (AbstractMonster m : AllyManager.allies.monsters) {
            if (m instanceof AbstractAlly && !m.isDeadOrEscaped()) {
                taken.add(((AbstractAlly) m).slotIndex);
            }
        }
        ArrayList<Integer> free = new ArrayList<>();
        for (int i = 0; i < AllyPositionHelper.SLOT_COUNT && free.size() < count; i++) {
            if (!taken.contains(i)) {
                free.add(i);
            }
        }
        return free;
    }
}