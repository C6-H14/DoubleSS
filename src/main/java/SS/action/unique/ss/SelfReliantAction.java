// Source code is decompiled from a .class file using FernFlower decompiler.
package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import SS.cards.PeptideStrike;

public class SelfReliantAction extends AbstractGameAction {
    private AbstractCard card = new PeptideStrike();

    public SelfReliantAction(AbstractCreature target, int amount) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
        this.source = AbstractDungeon.player;
        this.target = target;
        this.amount = amount;
    }

    public void update() {
        int count = 0;
        for (AbstractCard c : AbstractDungeon.actionManager.cardsPlayedThisCombat) {
            if (c.cardID == card.cardID) {
                ++count;
            }
        }
        this.addToTop(new GainBlockAction(this.target, count * amount));
        this.isDone = true;
    }
}
