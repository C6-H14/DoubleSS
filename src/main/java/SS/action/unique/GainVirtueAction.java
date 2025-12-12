package SS.action.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

import SS.power.SinsPower;

public class GainVirtueAction extends AbstractGameAction {

    public GainVirtueAction(AbstractCreature target, int amount) {
        this.duration = Settings.ACTION_DUR_MED;
        this.amount = amount;
        this.target = target;
    }

    public void update() {
        if (this.target.getPower("Double:SinsPower") == null) {
            addToBot(new ApplyPowerAction(target, target, new SinsPower(target, -amount)));
        } else {
            addToBot(new ReducePowerAction(this.target, this.target, "Double:SinsPower", amount));
        }
        this.isDone = true;

    }
}