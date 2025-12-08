package SS.action.unique.lost;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class LifelinePlayCardAction extends AbstractGameAction {

    private AbstractPlayer p;
    private boolean manager;
    private int playAmt;

    public LifelinePlayCardAction(int numberOfCards, boolean manager) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
        this.p = AbstractDungeon.player;
        this.manager = manager;
        this.playAmt = numberOfCards;
    }

    public void update() {
        if (p.drawPile.isEmpty() && p.discardPile.isEmpty()) {
            this.isDone = true;
            return;
        }
        if (p.drawPile.isEmpty()) {
            addToTop(new LifelinePlayCardAction(this.playAmt, this.manager));
            addToTop(new EmptyDeckShuffleAction());
            this.isDone = true;
            return;
        }
        if (!p.drawPile.isEmpty()) {
            AbstractCard c = p.drawPile.getTopCard();
            c.exhaustOnUseOnce = true;
            p.drawPile.group.remove(c);
            (AbstractDungeon.getCurrRoom()).souls.remove(c);
            if (!Settings.FAST_MODE) {
                addToTop((AbstractGameAction) new WaitAction(Settings.ACTION_DUR_MED));
            } else {
                addToTop((AbstractGameAction) new WaitAction(Settings.ACTION_DUR_FASTER));
            }
            addToTop(new NewQueueCardAction(c, true, false, true));
            if (!c.isEthereal && this.manager) {
                this.playAmt++;
            }
            for (int i = 0; i < this.playAmt - 1; i++) {
                AbstractCard tmp = c.makeStatEquivalentCopy();
                tmp.purgeOnUse = true;
                if (!Settings.FAST_MODE) {
                    addToTop((AbstractGameAction) new WaitAction(Settings.ACTION_DUR_MED));
                } else {
                    addToTop((AbstractGameAction) new WaitAction(Settings.ACTION_DUR_FASTER));
                }
                addToTop(new NewQueueCardAction(tmp, true, false, true));
            }
            AbstractDungeon.player.hand.refreshHandLayout();
        }
        this.isDone = true;
    }
}
