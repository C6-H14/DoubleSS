package SS.action.unique.lost;

import java.util.Iterator;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class FallenPactAction extends AbstractGameAction {
    private float startingDuration = Settings.ACTION_DUR_FAST;

    public FallenPactAction(int amount) {
        this.amount = amount;
        this.actionType = ActionType.WAIT;
        this.startingDuration = Settings.ACTION_DUR_FAST;
        this.duration = this.startingDuration;
    }

    public void update() {
        if (this.duration == this.startingDuration) {
            Iterator var1 = AbstractDungeon.player.hand.group.iterator();

            while (var1.hasNext()) {
                AbstractCard c = (AbstractCard) var1.next();
                if (c.isEthereal) {
                    addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player,
                            new StrengthPower(AbstractDungeon.player, this.amount)));
                    addToTop(new ExhaustSpecificCardAction(c, AbstractDungeon.player.hand));
                }
            }

            this.isDone = true;
            if (AbstractDungeon.player.exhaustPile.size() >= 20) {
                UnlockTracker.unlockAchievement("THE_PACT");
            }
        }
    }
}