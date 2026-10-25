package SS.action.unique.c6h14;

import SS.power.SinsPower;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.InstantKillAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class FinalJudgementAction extends AbstractGameAction {
    private final int multiplier;

    public FinalJudgementAction(AbstractCreature owner, int multiplier) {
        this.source = owner;
        this.multiplier = multiplier;
    }

    @Override
    public void update() {
        int virtue = source.hasPower(SinsPower.POWER_ID)
                ? Math.max(0, -source.getPower(SinsPower.POWER_ID).amount) : 0;
        long threshold = (long) virtue * multiplier;
        for (AbstractMonster enemy : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (!enemy.isDeadOrEscaped() && enemy.currentHealth <= threshold) {
                addToBot(new InstantKillAction(enemy));
            }
        }
        isDone = true;
    }
}
