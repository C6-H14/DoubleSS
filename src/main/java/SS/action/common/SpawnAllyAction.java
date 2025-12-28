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
            // 1. 检查是否允许召唤 (比如你可以限制场上最多 3 个友军)
            // if (MinionManager.minions.size() >= 3) { ... }

            // 2. 播放召唤特效 (可选)
            // 在友军脚下播放一个火焰特效
            AbstractDungeon.effectList.add(new InflameEffect(minionToSpawn));

            // 3. 核心：添加到管理器
            AllyManager.addMinion(minionToSpawn);
        }

        this.tickDuration();
    }
}