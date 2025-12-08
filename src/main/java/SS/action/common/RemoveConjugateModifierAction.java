package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;

import basemod.helpers.CardModifierManager;

public class RemoveConjugateModifierAction extends AbstractGameAction {
    private boolean exhaustCards;
    private AbstractCard card;

    public RemoveConjugateModifierAction(AbstractCard c) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
        this.card = c;
    }

    public void update() {
        CardModifierManager.removeModifiersById(this.card, "Double:ConjugateModifier", false);
        this.isDone = true;
    }
}
