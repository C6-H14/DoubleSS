package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class IncreasePeptideDamageAction extends AbstractGameAction {

    public IncreasePeptideDamageAction(int amount) {
        this.amount = amount;
    }

    public void update() {

        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (c instanceof SS.cards.PeptideStrike) {
                c.baseDamage += this.amount;
                c.applyPowers();
            }
        }

        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (c instanceof SS.cards.PeptideStrike) {
                c.baseDamage += this.amount;
                c.applyPowers();
            }
        }

        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c instanceof SS.cards.PeptideStrike) {
                c.baseDamage += this.amount;
                c.applyPowers();
            }
        }

        this.isDone = true;
    }
}