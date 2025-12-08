package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

import java.util.ArrayList;
import java.util.Iterator;

import SS.cards.Charity;
import SS.cards.Chastity;
import SS.cards.Diligence;
import SS.cards.Humility;
import SS.cards.Kindness;
import SS.cards.Patience;
import SS.cards.Temperance;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class YinYangAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    private AbstractPlayer p;
    private int dupeAmount = 1;
    private ArrayList<AbstractCard> cannotTransform = new ArrayList();

    public YinYangAction(AbstractCreature source, int amount) {
        this.setValues(AbstractDungeon.player, source, amount);
        this.actionType = ActionType.DRAW;
        this.duration = 0.25F;
        this.p = AbstractDungeon.player;
        this.dupeAmount = amount;
    }

    public void update() {
        Iterator var1;
        AbstractCard c;
        if (this.duration == Settings.ACTION_DUR_FAST) {
            var1 = this.p.hand.group.iterator();

            while (var1.hasNext()) {
                c = (AbstractCard) var1.next();
                if (!c.hasTag(AbstractCardEnum.Sins)) {
                    this.cannotTransform.add(c);
                }
            }

            if (this.cannotTransform.size() == this.p.hand.group.size()) {
                this.isDone = true;
                return;
            }

            if (this.p.hand.group.size() - this.cannotTransform.size() == 1) {
                var1 = this.p.hand.group.iterator();

                while (var1.hasNext()) {
                    c = (AbstractCard) var1.next();
                    if (c.hasTag(AbstractCardEnum.Sins)) {
                        AbstractCard cc = this.addCard(c);
                        if (c.upgraded)
                            cc.upgrade();
                        addToTop(new MakeTempCardInHandAction(cc, this.amount));
                        addToTop(new ExhaustSpecificCardAction(c, this.p.hand));
                        this.isDone = true;
                        return;
                    }
                }
            }

            this.p.hand.group.removeAll(this.cannotTransform);
            if (this.p.hand.group.size() > 1) {
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false, false, false);
                this.tickDuration();
                return;
            }

            if (this.p.hand.group.size() == 1) {
                c = this.p.hand.getTopCard();
                AbstractCard cc = this.addCard(c);
                if (c.upgraded)
                    cc.upgrade();
                addToTop(new MakeTempCardInHandAction(cc, this.amount));
                addToTop(new ExhaustSpecificCardAction(c, this.p.hand));
                returnCards();
                this.isDone = true;
            }
        }

        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (var1 = AbstractDungeon.handCardSelectScreen.selectedCards.group.iterator(); var1
                    .hasNext(); this.p.hand.moveToExhaustPile(c)) {
                c = (AbstractCard) var1.next();
                AbstractCard cc = this.addCard(c);
                if (c.upgraded)
                    cc.upgrade();
                addToTop(new MakeTempCardInHandAction(cc, this.amount));
            }
            returnCards();

            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
            AbstractDungeon.handCardSelectScreen.selectedCards.group.clear();
            this.isDone = true;
        }

        this.tickDuration();
    }

    private void returnCards() {
        for (AbstractCard c : this.cannotTransform) {
            this.p.hand.addToTop(c);
        }
        this.p.hand.refreshHandLayout();
    }

    private AbstractCard addCard(AbstractCard c) {
        if (c.hasTag(AbstractCardEnum.Gluttony)) {
            return new Temperance();
        }
        if (c.hasTag(AbstractCardEnum.Greed)) {
            return new Charity();
        }
        if (c.hasTag(AbstractCardEnum.Sloth)) {
            return new Diligence();
        }
        if (c.hasTag(AbstractCardEnum.Envy)) {
            return new Kindness();
        }
        if (c.hasTag(AbstractCardEnum.Wrath)) {
            return new Patience();
        }
        if (c.hasTag(AbstractCardEnum.Lust)) {
            return new Chastity();
        }
        if (c.hasTag(AbstractCardEnum.Pride)) {
            return new Humility();
        }
        return new Madness();
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString(ModHelper.makePath("YinYangAction"));
        TEXT = uiStrings.TEXT;
    }
}
