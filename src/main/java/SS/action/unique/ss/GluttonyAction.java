package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import SS.cards.PeptideStrike;

import java.util.Iterator;

public class GluttonyAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    private AbstractPlayer p;
    private boolean flag = false;

    public GluttonyAction(int amount, boolean upgrade) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
        this.flag = upgrade;
    }

    public void update() {
        AbstractCard pep = new PeptideStrike();
        if (flag) {
            pep.upgrade();
        }
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.p.hand.isEmpty()) {
                this.isDone = true;
            } else if (this.p.hand.size() <= amount) {
                for (AbstractCard c : p.hand.group) {
                    if (c.costForTurn == -1) {
                        this.addToTop(new MakeTempCardInHandAction(pep, EnergyPanel.getCurrentEnergy()));
                    } else if (c.costForTurn > 0) {
                        this.addToTop(
                                new MakeTempCardInHandAction(pep, c.costForTurn));
                    }
                    this.p.hand.moveToExhaustPile(c);
                }

                this.tickDuration();
            } else {
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], amount, false);
                this.tickDuration();
            }
        } else {
            if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
                AbstractCard c;
                for (Iterator var1 = AbstractDungeon.handCardSelectScreen.selectedCards.group.iterator(); var1
                        .hasNext(); this.p.hand.moveToExhaustPile(c)) {
                    c = (AbstractCard) var1.next();
                    if (c.costForTurn == -1) {
                        this.addToTop(
                                new MakeTempCardInHandAction(pep, EnergyPanel.getCurrentEnergy()));
                    } else if (c.costForTurn > 0) {
                        this.addToTop(
                                new MakeTempCardInHandAction(pep, c.costForTurn));
                    }
                }

                AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
                AbstractDungeon.handCardSelectScreen.selectedCards.group.clear();
            }

            this.tickDuration();
        }
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString("RecycleAction");
        TEXT = uiStrings.TEXT;
    }
}
