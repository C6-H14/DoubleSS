package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.WitherDice;
import SS.action.dice.ChannelDiceAction;
import SS.cardmodifiers.ConjugateModifier;
import SS.helper.ModHelper;

public class DecomposeAction extends AbstractGameAction {
    public static final String ID = ModHelper.makePath("ConjugateCardAction");
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(ID);
    public static final String[] TEXT = uiStrings.TEXT;
    private AbstractPlayer p;
    public static int numCombined;
    private static final float DURATION = Settings.ACTION_DUR_XFAST;
    final ConjugateModifier newConjugate = new ConjugateModifier();

    public DecomposeAction(AbstractMonster target, AbstractCreature source, int amount) {
        this.p = AbstractDungeon.player;
        this.target = target;
        this.source = source;
        this.amount = amount;
        setValues(target, source, amount);
        this.duration = DURATION;
    }

    public void update() {
        boolean tag = false;
        for (AbstractCard c : p.drawPile.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToTop(new ExhaustSpecificCardAction(c, p.drawPile));
                tag = true;
            }
        }
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToTop(new ExhaustSpecificCardAction(c, p.hand));
                tag = true;
            }
        }
        for (AbstractCard c : p.discardPile.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToTop(new ExhaustSpecificCardAction(c, p.discardPile));
                tag = true;
            }
        }
        this.isDone = true;

        tickDuration();
    }
}