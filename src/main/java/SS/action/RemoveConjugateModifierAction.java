// Source code is decompiled from a .class file using FernFlower decompiler.
package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.AbstractGameAction.ActionType;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.actions.utility.UnlimboAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import basemod.helpers.CardModifierManager;

public class RemoveConjugateModifierAction extends AbstractGameAction {
    private boolean exhaustCards;
    private AbstractCard card;

    public RemoveConjugateModifierAction(AbstractCard c) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
        this.card = c;
    }

    public void update() {
        CardModifierManager.removeModifiersById(this.card, "Double:ConjugateModifier", false);
        this.isDone = true;
    }
}
