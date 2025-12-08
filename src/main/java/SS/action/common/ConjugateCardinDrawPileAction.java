package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

import SS.cardmodifiers.ConjugateModifier;
import SS.cardmodifiers.FindFather;
import SS.helper.ModHelper;

public class ConjugateCardinDrawPileAction extends AbstractGameAction {
    public static final String ID = ModHelper.makePath("ConjugateCardAction");
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(ID);
    public static final String[] TEXT = uiStrings.TEXT;
    private AbstractPlayer p;
    public static int numCombined;
    private static final float DURATION = Settings.ACTION_DUR_XFAST;
    ConjugateModifier newConjugate = new ConjugateModifier();

    public ConjugateCardinDrawPileAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.p = (AbstractPlayer) target;
        setValues(target, source, amount);
        this.amount = amount;
        this.duration = DURATION;
    }

    public void update() {
        if (this.duration == DURATION) {
            if (AbstractDungeon.getMonsters().areMonstersBasicallyDead() || this.p.drawPile.size() <= 1
                    || this.amount <= 1) {
                this.isDone = true;

                return;
            }
            if (this.p.drawPile.size() <= this.amount) {
                AbstractCard headCard = this.p.drawPile.getTopCard();
                this.amount = this.p.drawPile.size();

                for (AbstractCard c : this.p.drawPile.group) {
                    AbstractCard d = c;
                    if (d == headCard)
                        continue;
                    FindFather.ConjugateCard(d, headCard);
                }
                tickDuration();
                this.isDone = true;

                return;
            }
            CardGroup temp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            for (AbstractCard c : this.p.drawPile.group) {
                temp.addToTop(c);
            }
            temp.sortAlphabetically(true);
            temp.sortByRarityPlusStatusCardType(false);
            AbstractDungeon.gridSelectScreen.open(temp, this.amount, TEXT[1] + this.amount + TEXT[2], false);
            AbstractDungeon.player.drawPile.applyPowers();
            tickDuration();
            return;
        }

        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            AbstractCard headCard = (AbstractCard) AbstractDungeon.gridSelectScreen.selectedCards
                    .get(0);
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
                AbstractCard d = (AbstractCard) c;
                if (d == headCard)
                    continue;
                FindFather.ConjugateCard(d, headCard);
            }
        }
        this.isDone = true;

        tickDuration();
    }
}