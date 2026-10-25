package SS.action.unique.c6h14;

import SS.action.monster.EvokeSoulAction;
import SS.power.PassingTheTorchPower;
import SS.power.PassingTheTorchEnergyPower;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class PassingTheTorchAction extends AbstractGameAction {
    private final AbstractPlayer player;
    private final boolean free;
    private final boolean upgraded;
    private final int energy;
    private final boolean manager;

    public PassingTheTorchAction(AbstractPlayer player, boolean free, int energyOnUse, boolean upgraded, boolean manager) {
        this.player = player;
        this.free = free;
        this.upgraded = upgraded;
        this.manager = manager;
        this.energy = energyOnUse >= 0 ? energyOnUse : EnergyPanel.totalCount;
        this.actionType = ActionType.SPECIAL;
    }

    @Override
    public void update() {
        int effect = energy + (upgraded ? 1 : 0);
        if (player.hasRelic("Chemical X")) {
            effect += 2;
            player.getRelic("Chemical X").flash();
        }
        if (!free) player.energy.use(EnergyPanel.totalCount);
        if (effect > 0) {
            addToBot(new EvokeSoulAction(effect));
            addToBot(new ApplyPowerAction(player, player, new PassingTheTorchPower(player, effect), effect));
            if (manager) {
                addToBot(new ApplyPowerAction(player, player, new PassingTheTorchEnergyPower(player, effect), effect));
            }
        }
        isDone = true;
    }
}
