package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class RevengeAction extends AbstractGameAction {
    public int heal;
    private DamageInfo info;

    public RevengeAction(AbstractCreature target, DamageInfo info, int healHPAmount) {
        this.target = target;
        setValues(target, info);
        this.info = info;
        this.heal = healHPAmount;
    }

    public void update() {
        this.target.damage(this.info);
        if ((this.target.isDying || this.target.currentHealth <= 0) && !this.target.halfDead
                && !this.target.hasPower("Minion")) {
            AbstractDungeon.player.heal(this.heal);
        }
        this.isDone = true;
    }
}