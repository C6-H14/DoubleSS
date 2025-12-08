package SS.action;

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

import SS.Dice.AttackDice;
import SS.cardmodifiers.ConjugateModifier;
import SS.cardmodifiers.FindFather;
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
        for (AbstractCard c : p.drawPile.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToTop(new ExhaustSpecificCardAction(c, p.drawPile));
                addToTop(new ChannelDiceAction(new AttackDice(this.amount, (AbstractMonster) this.target)));
            }
        }
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToTop(new ExhaustSpecificCardAction(c, p.hand));
                addToTop(new ChannelDiceAction(new AttackDice(this.amount, (AbstractMonster) this.target)));
            }
        }
        for (AbstractCard c : p.discardPile.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToTop(new ExhaustSpecificCardAction(c, p.discardPile));
                addToTop(new ChannelDiceAction(new AttackDice(this.amount, (AbstractMonster) this.target)));
            }
        }
        this.isDone = true;

        tickDuration();
    }
}