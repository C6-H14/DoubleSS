package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

import SS.cardmodifiers.ConjugateModifier;
import SS.cardmodifiers.FindFather;
import SS.helper.ModHelper;

public class ConjugateCardInHandAction extends AbstractGameAction {
    public static final String ID = ModHelper.makePath("ConjugateCardAction");
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(ID);
    public static final String[] TEXT = uiStrings.TEXT;
    private AbstractPlayer p;
    public static int numCombined;
    private static final float DURATION = Settings.ACTION_DUR_XFAST;
    final ConjugateModifier newConjugate = new ConjugateModifier();

    public ConjugateCardInHandAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.p = (AbstractPlayer) target;
        setValues(target, source, amount);
        this.duration = DURATION;
    }

    public void update() {
        if (this.duration == DURATION) {
            if (AbstractDungeon.getMonsters().areMonstersBasicallyDead() || this.p.hand.size() <= 1
                    || this.amount <= 1) {
                this.isDone = true;

                return;
            }
            if (this.p.hand.size() <= this.amount) {
                AbstractCard headCard = this.p.hand.getTopCard();
                this.amount = this.p.hand.size();

                for (AbstractCard c : this.p.hand.group) {
                    AbstractCard d = c;
                    if (d == headCard)
                        continue;
                    FindFather.ConjugateCard(d, headCard);
                }
                tickDuration();
                this.isDone = true;

                return;
            }
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], this.amount, false);
            AbstractDungeon.player.hand.applyPowers();
            tickDuration();
            return;

        }

        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            AbstractCard headCard = AbstractDungeon.handCardSelectScreen.selectedCards.group
                    .get(0);
            AbstractDungeon.player.hand.addToHand(headCard);
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                AbstractCard d = (AbstractCard) c;
                if (d == headCard)
                    continue;
                FindFather.ConjugateCard(d, headCard);
                AbstractDungeon.player.hand.addToHand(d);
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.isDone = true;

        tickDuration();
    }
}