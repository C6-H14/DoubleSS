package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.unique.PoisonLoseHpAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import SS.cards.PeptideStrike;

public class TriggerPoisonAction extends AbstractGameAction {

    public TriggerPoisonAction(AbstractCreature target, AbstractCreature source) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
        this.source = source;
        this.target = target;
        this.amount = 1;
    }

    public TriggerPoisonAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
        this.source = source;
        this.target = target;
        this.amount = amount;
    }

    public void update() {
        if (this.target.hasPower("Poison")) {
            for (int i = 0; i < this.amount; ++i) {
                addToBot(new PoisonLoseHpAction(this.target, this.source, this.target.getPower("Poison").amount,
                        AttackEffect.POISON));
            }
        }
        this.isDone = true;
    }
}
