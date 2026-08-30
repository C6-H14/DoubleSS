package SS.action.monster;

import java.util.ArrayList;
import java.util.HashSet;

import com.megacrit.cardcrawl.actions.AbstractGameAction;

import SS.monster.ally.AbstractAlly;
import SS.monster.ally.AllyManager;
import SS.monster.ally.AllyPositionHelper;
import SS.monster.ally.SoulAlly;

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

        // 1. 移动现有的魂火到空槽位，并将其牌组变形为《圣约》（最多 SOUL_TARGET 个）
        int placed = 0;
        for (SoulAlly s : souls) {
            if (placed >= SOUL_TARGET || free.isEmpty()) {
                break;
            }
            int slot = free.remove(0);
            s.relocateToSlot(slot);
            s.transformToGenesisDeck();
            s.syncSoulFire();
            placed++;
        }

        // 2. 生成新的魂火补齐到目标数；槽位耗尽即停，场上友军总数永不超过 SLOT_COUNT
        while (placed < SOUL_TARGET && !free.isEmpty()) {
            int slot = free.remove(0);
            SoulAlly newSoul = new SoulAlly(slot);
            AllyManager.addMinion(newSoul);
            // 【核心】新生成的也立刻变形牌组
            newSoul.transformToGenesisDeck();
            placed++;
        }

        this.isDone = true;
    }
}
