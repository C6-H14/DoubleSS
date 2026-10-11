package SS.action.monster;

import java.util.ArrayList;
import java.util.HashSet;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.monster.ally.AbstractAlly;
import SS.monster.ally.AllyManager;
import SS.monster.ally.AllyPositionHelper;
import SS.monster.ally.SoulAlly;
import SS.power.SoulFirePower;

public class EnterEvokeSoulEnvAction extends AbstractGameAction {
    // 阵法槽位表（SLOT_COUNT 个槽位）与坐标换算全部收敛在 AllyPositionHelper。
    // 这里负责"把魂火维持到 SOUL_TARGET 个，并各就其位"。
    //
    // SOUL_TARGET 是创世记阵法维持的【魂火】数量，独立于总槽位数 SLOT_COUNT(5)：
    // 魂火只占前几个槽位，其余槽位留给其他（未来新增的）AbstractAlly 友军。

    private static final int SOUL_TARGET = 3;

    @Override
    public void update() {
        // 场上所有存活友军（含未来新增的 AbstractAlly 子类），它们的槽位要被保留
        ArrayList<AbstractAlly> alive = AllyManager.getAliveAllies();
        HashSet<Integer> reserved = new HashSet<>();
        for (AbstractAlly a : alive) {
            if (!(a instanceof SoulAlly)) {
                reserved.add(a.slotIndex);
            }
        }

        // 空槽位（升序，魂火从前往后占）
        ArrayList<Integer> free = new ArrayList<>();
        for (int i = 0; i < AllyPositionHelper.SLOT_COUNT; i++) {
            if (!reserved.contains(i)) {
                free.add(i);
            }
        }

        // 当前场上的魂火
        ArrayList<SoulAlly> souls = new ArrayList<>();
        for (AbstractAlly a : alive) {
            if (a instanceof SoulAlly) {
                souls.add((SoulAlly) a);
            }
        }

        // 0. 【统一魂火值】阵法内的魂火是一团火，进阵时先收敛到最高的那只。
        //    必须在"补新魂火"之前做，否则新魂火会按进阵前的旧值生成，
        //    而旧的几只又各自不齐 —— 用户实测到的"提升到 2 之后新跟班还是 1"。
        int targetValue = 1;
        for (SoulAlly s : souls) {
            AbstractPower p = s.getPower(SoulFirePower.POWER_ID);
            if (p != null && p.amount > targetValue) {
                targetValue = p.amount;
            }
        }

        // 1. 移动现有的魂火到空槽位，并将其牌组变形为《圣约》（最多 SOUL_TARGET 个）
        int placed = 0;
        for (SoulAlly s : souls) {
            if (placed >= SOUL_TARGET || free.isEmpty()) {
                break;
            }
            int slot = free.remove(0);
            s.relocateToSlot(slot);
            // 【关键顺序】先把血补到 targetValue 对应的血量，再 syncSoulFire()。
            // syncSoulFire 只在"算出的值更大"时才写层数，且它自己会写 maxHealth，
            // 所以必须让血量先到位，否则层数会被它按旧血量覆写回去。
            s.currentHealth = targetValue * SOUL_HP_PER_LEVEL;
            s.syncSoulFire();
            s.transformToGenesisDeck();
            placed++;
        }

        // 2. 生成新的魂火补齐到目标数；槽位耗尽即停，场上友军总数永不超过 SLOT_COUNT
        while (placed < SOUL_TARGET && !free.isEmpty()) {
            int slot = free.remove(0);
            SoulAlly newSoul = new SoulAlly(slot);
            // 【核心】补血而不是硬改能力层数。
            // 魂火值的【唯一真实来源是血量】：syncSoulFire() 每帧都会按 ceil(血量/3)
            // 重算并覆盖 p.amount。所以事后直接改 p.amount 撑不过一帧，
            // 必须补血让 syncSoulFire() 自己算出目标层数。
            newSoul.currentHealth = targetValue * SOUL_HP_PER_LEVEL;
            newSoul.syncSoulFire();
            AllyManager.addMinion(newSoul);
            newSoul.transformToGenesisDeck();
            placed++;
        }

        this.isDone = true;
    }

    /** 每 1 点魂火对应的血量（3 点上限）。与 SoulAlly.syncSoulFire() 的换算一致。 */
    private static final int SOUL_HP_PER_LEVEL = 3;
}
