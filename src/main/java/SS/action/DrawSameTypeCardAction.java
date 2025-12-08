package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.AbstractGameAction.ActionType;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import SS.cards.AbstractDoubleCard;
import basemod.BaseMod;

public class DrawSameTypeCardAction extends AbstractGameAction {
    private AbstractPlayer p;
    private CardType t, tracker;

    public DrawSameTypeCardAction(CardType t) {
        this.p = AbstractDungeon.player;
        setValues((AbstractCreature) this.p, (AbstractCreature) this.p);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_MED;
        this.t = t;
    }

    public DrawSameTypeCardAction() {
        this.p = AbstractDungeon.player;
        setValues((AbstractCreature) this.p, (AbstractCreature) this.p);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_MED;
        this.t = null;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            if ((this.p.drawPile.isEmpty() && this.p.discardPile.isEmpty())
                    || this.p.hand.size() >= BaseMod.MAX_HAND_SIZE) {
                this.isDone = true;

                return;
            }
            if (AbstractDungeon.player.hasPower("No Draw")) {

                AbstractDungeon.player.getPower("No Draw").flash();
                this.isDone = true;

                return;
            }
            if (!this.p.drawPile.isEmpty()) {
                AbstractCard c = AbstractDungeon.player.drawPile.group.get(AbstractDungeon.player.drawPile.size() - 1);
                this.tracker = c.type;
                if (this.t == null) {
                    this.t = this.tracker;
                } else if (this.t != this.tracker) {
                    addToTop(new DrawCardAction(1));
                    this.isDone = true;
                    return;
                }
                addToTop(new DrawSameTypeCardAction(this.t));
                addToTop(new DrawCardAction(1));
            } else {
                addToTop(new DrawSameTypeCardAction(this.t));
                addToTop(new EmptyDeckShuffleAction());
            }

            this.isDone = true;

            return;
        }
        tickDuration();
    }
}
