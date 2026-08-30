package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.InflameEffect; // 或者是其他召唤特效

import SS.monster.ally.AbstractAlly;
import SS.monster.ally.AllyManager;

public class SpawnAllyAction extends AbstractGameAction {
    private AbstractAlly minionToSpawn;

    public SpawnAllyAction(AbstractAlly minion) {
        this.actionType = ActionType.SPECIAL;
        this.duration = Settings.ACTION_DUR_FAST; // 或者是 0.5F 之类的
        this.minionToSpawn = minion;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            // 槽位检查：场上存活友军（任意 AbstractAlly 子类）已达 SLOT_COUNT 上限时，
            // 直接取消本次召唤（不播特效、不入管理器）
            if (!AllyManager.canAddAlly()) {
                this.minionToSpawn = null;
                return;
            }

            // 1. 播放召唤特效 (可选)
            // 在友军脚下播放一个火焰特效
            AbstractDungeon.effectList.add(new InflameEffect(minionToSpawn));

            // 2. 核心：添加到管理器
            AllyManager.addMinion(minionToSpawn);
        }

        this.tickDuration();
    }
}
