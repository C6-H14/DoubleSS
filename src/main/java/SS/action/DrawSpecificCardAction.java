package SS.action;

import com.megacrit.cardcrawl.actions.*;
import com.megacrit.cardcrawl.characters.*;
import com.megacrit.cardcrawl.dungeons.*;

import SS.cards.Haohao.AbstractHaoCard;

import com.megacrit.cardcrawl.core.*;
import com.megacrit.cardcrawl.cards.*;
import basemod.*;
import com.megacrit.cardcrawl.actions.common.*;
import java.util.*;

import org.lwjgl.Sys;

public class DrawSpecificCardAction extends AbstractGameAction {
    public AbstractPlayer p;
    public int cards_to_draw;
    private AbstractCard c;

    public DrawSpecificCardAction(AbstractCard card, final int number) {
        this.cards_to_draw = number;
        this.c = card;
        this.setValues((AbstractCreature) (this.p = AbstractDungeon.player), (AbstractCreature) this.p);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_MED;
    }

    public DrawSpecificCardAction(AbstractCard card) {
        this.cards_to_draw = 1;
        this.c = card;
        this.setValues((AbstractCreature) (this.p = AbstractDungeon.player), (AbstractCreature) this.p);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_MED;
    }

    public void update() {
        System.out.println(c.cardID);
        if (this.duration == Settings.ACTION_DUR_MED) {
            if (this.p.drawPile.isEmpty()) {
                this.isDone = true;
                return;
            }
            if (AbstractDungeon.player.hasPower("No Draw")) {
                AbstractDungeon.player.getPower("No Draw").flash();
                this.isDone = true;
                return;
            }
            int counter = 0;
            final CardGroup tmp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            Iterator<AbstractCard> var2 = this.p.drawPile.group.iterator();
            System.out.println(this.cards_to_draw);
            System.out.println(var2.hasNext());
            for (; var2.hasNext() && counter < this.cards_to_draw;) {
                final AbstractCard card = var2.next();
                if (card.cardID.equals(c.cardID)) {
                    tmp.addToRandomSpot(card);
                    ++counter;
                }
            }
            if (tmp.size() == 0) {
                this.isDone = true;
                return;
            }
            for (int i = 0; i < counter; ++i) {
                if (!tmp.isEmpty()) {
                    tmp.shuffle();
                    final AbstractCard card = tmp.getBottomCard();
                    tmp.removeCard(card);
                    if (this.p.hand.size() == BaseMod.MAX_HAND_SIZE) {
                        this.p.createHandIsFullDialog();
                    } else {
                        this.p.drawPile.moveToDeck(card, false);
                        this.addToTop((AbstractGameAction) new DrawCardAction(1));
                    }
                }
            }
            this.isDone = true;
        }
        this.tickDuration();
    }
}
