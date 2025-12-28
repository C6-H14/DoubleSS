package SS.helper;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import SS.interfaces.OnAllyAttackSubscriber;
import SS.monster.ally.AbstractAlly;

public class AllyTriggerHelper {
    public static void triggerOnAllyAttack(AbstractAlly minion, AbstractCreature target, int damage) {
        if (AbstractDungeon.player == null)
            return;

        // 触发玩家Power
        for (AbstractPower p : AbstractDungeon.player.powers) {
            if (p instanceof OnAllyAttackSubscriber) {
                ((OnAllyAttackSubscriber) p).onAllyAttack(minion, target, damage);
            }
        }
        // 触发玩家Relic
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            if (r instanceof OnAllyAttackSubscriber) {
                ((OnAllyAttackSubscriber) r).onAllyAttack(minion, target, damage);
            }
        }
    }

}
