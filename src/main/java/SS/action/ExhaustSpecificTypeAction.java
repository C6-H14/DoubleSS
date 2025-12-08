package SS.action;

import java.util.Iterator;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class ExhaustSpecificTypeAction
        extends AbstractGameAction {
    private float startingDuration = Settings.ACTION_DUR_FAST;

    private CardType t;

    public ExhaustSpecificTypeAction(CardType tt) {
        this.t = tt;
        this.actionType = ActionType.WAIT;
        this.startingDuration = Settings.ACTION_DUR_FAST;
        this.duration = this.startingDuration;
    }

    public void update() {
        if (this.duration == this.startingDuration) {
            Iterator var1 = AbstractDungeon.player.hand.group.iterator();

            while (var1.hasNext()) {
                AbstractCard c = (AbstractCard) var1.next();
                if (c.type == t) {
                    this.addToTop(new ExhaustSpecificCardAction(c, AbstractDungeon.player.hand));
                }
            }

            this.isDone = true;
            if (AbstractDungeon.player.exhaustPile.size() >= 20) {
                UnlockTracker.unlockAchievement("THE_PACT");
            }
        }
    }
}