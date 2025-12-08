package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ExhaustAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import SS.cards.AbstractDoubleCard;
import SS.cards.PeptideStrike;
import SS.power.CrimePower;

public class TaotieRiteAction extends AbstractGameAction {
    private boolean flag;
    private float startingDuration;

    public TaotieRiteAction(boolean upgraded, int crime) {
        this.amount = crime;
        this.flag = upgraded;
        this.actionType = ActionType.WAIT;
        this.attackEffect = AttackEffect.FIRE;
        this.startingDuration = Settings.ACTION_DUR_FAST;
        this.duration = this.startingDuration;
    }

    public void update() {
        int count = AbstractDungeon.player.hand.size();
        AbstractDoubleCard pep = new PeptideStrike();
        if (this.flag) {
            pep.upgrade();
        }

        int i;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c.costForTurn == -1) {
                this.addToTop(
                        new MakeTempCardInHandAction(pep, EnergyPanel.getCurrentEnergy()));
            } else if (c.costForTurn > 0) {
                this.addToTop(
                        new MakeTempCardInHandAction(pep, c.costForTurn));
            }
        }
        for (i = 0; i < count; ++i) {
            this.addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player,
                    new CrimePower(AbstractDungeon.player, this.amount)));
        }
        if (this.flag) {
            this.addToTop(new HealAction(AbstractDungeon.player, AbstractDungeon.player, count));
        }

        for (i = 0; i < count; ++i) {
            if (Settings.FAST_MODE) {
                this.addToTop(new ExhaustAction(1, true, true, false, Settings.ACTION_DUR_XFAST));
            } else {
                this.addToTop(new ExhaustAction(1, true, true));
            }
        }

        this.isDone = true;
    }
}
