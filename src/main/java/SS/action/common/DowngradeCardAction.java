package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.core.Settings;

import SS.cards.AbstractDoubleCard;

public class DowngradeCardAction extends AbstractGameAction {
    private AbstractDoubleCard c;

    public DowngradeCardAction(AbstractDoubleCard cardToDowngrade) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.c = cardToDowngrade;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.c.canDowngrade() && this.c.type != CardType.STATUS) {
                this.c.downgrade();
                this.c.superFlash();
                this.c.applyPowers();
            }

            this.isDone = true;
        } else {
            this.tickDuration();
        }
    }
}
