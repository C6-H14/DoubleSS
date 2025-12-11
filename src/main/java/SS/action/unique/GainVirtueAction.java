package SS.action.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

public class GainVirtueAction extends AbstractGameAction {
    private DamageInfo info;

    public GainVirtueAction(AbstractCreature target, int amount) {
        this.setValues(target, info);
        this.duration = Settings.ACTION_DUR_MED;
        this.amount = amount;
        this.target = target;
    }

    public void update() {
        addToBot(new ReducePowerAction(this.target, this.target, "Double:SinsPower", amount));
        this.isDone = true;

    }
}