package SS.action.unique.lost;

import java.util.Iterator;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

import SS.power.DyingPower;

public class AltarActon extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ExhaustAction");
    public static final String[] TEXT = uiStrings.TEXT;
    private AbstractPlayer p;
    public static int numCombined;
    private static final float DURATION = Settings.ACTION_DUR_XFAST;
    public int block;

    public AltarActon(AbstractCreature target, AbstractCreature source, int amount, int block) {
        this.p = (AbstractPlayer) target;
        this.amount = amount;
        this.block = block;
        setValues(target, source, amount);
        this.duration = DURATION;
    }

    public void update() {
        if (this.duration == DURATION) {
            if (AbstractDungeon.getMonsters().areMonstersBasicallyDead() || this.p.hand.size() <= 0
                    || this.amount <= 0) {
                this.isDone = true;

                return;
            }
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], this.amount, true, true);
            AbstractDungeon.player.hand.applyPowers();
            tickDuration();
            return;

        }

        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            Iterator var4 = AbstractDungeon.handCardSelectScreen.selectedCards.group.iterator();

            while (var4.hasNext()) {
                AbstractCard c = (AbstractCard) var4.next();
                this.p.hand.moveToExhaustPile(c);
                addToBot(new GainBlockAction(this.target, this.block));
                addToBot(new ApplyPowerAction(this.target, this.target, new DyingPower(this.target, 1)));
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.isDone = true;

        tickDuration();
    }
}