package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.ShuffleAction;
import com.megacrit.cardcrawl.actions.defect.ShuffleAllAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import SS.Dice.DefendDice;
import SS.action.dice.ChannelDiceAction;

public class NutrientCycleAction extends AbstractGameAction {
    private AbstractPlayer p;
    private boolean freeToPlayOnce = false;
    private int energyOnUse = -1;

    public NutrientCycleAction(AbstractPlayer p, boolean freeToPlayOnce, int energyOnUse) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.freeToPlayOnce = freeToPlayOnce;
        this.duration = Settings.ACTION_DUR_XFAST;
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        this.energyOnUse = energyOnUse;
    }

    public void update() {
        int effect = EnergyPanel.totalCount;
        if (this.energyOnUse != -1) {
            effect = this.energyOnUse;
        }

        if (this.p.hasRelic("Chemical X")) {
            effect += 2;
            this.p.getRelic("Chemical X").flash();
        }
        int amount = p.hand.size();
        addToBot(new ShuffleAllAction());
        addToBot(new ShuffleAction(p.drawPile, false));
        if (!this.freeToPlayOnce) {
            this.p.energy.use(EnergyPanel.totalCount);
        }
        for (int i = 0; i < amount; ++i) {
            addToBot(new DrawCardAction(1));
            addToBot(new ChannelDiceAction(new DefendDice(effect, p)));
            addToBot(new GainEnergyAction(1));
        }
        this.isDone = true;
    }
}
