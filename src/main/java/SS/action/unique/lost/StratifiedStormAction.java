package SS.action.unique.lost;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import SS.Dice.EternalAttackDice;
import SS.action.common.ModifyPermanentDamageAction;
import SS.action.dice.ChannelDiceAction;
import SS.power.DyingPower;

import java.util.UUID;

public class StratifiedStormAction extends AbstractGameAction {
    private AbstractPlayer p;
    private boolean freeToPlayOnce = false;
    private int energyOnUse = -1;
    private UUID uid;
    private int damage;
    private boolean manager;

    public StratifiedStormAction(AbstractPlayer p, boolean freeToPlayOnce, int energyOnUse, int damage, UUID uid,
            boolean manager) {
        this.actionType = ActionType.DAMAGE;
        this.p = AbstractDungeon.player;
        this.uid = uid;
        this.damage = damage;
        this.manager = manager;
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
        if (!this.freeToPlayOnce) {
            this.p.energy.use(EnergyPanel.totalCount);
        }
        for (int i = 0; i < effect; ++i) {
            addToBot(new ChannelDiceAction(new EternalAttackDice(damage + i, p)));
        }
        addToBot(new ModifyPermanentDamageAction(this.uid, effect));
        if (manager) {
            addToBot(new ApplyPowerAction(p, p, new DyingPower(p, effect)));
        }
        this.isDone = true;
    }
}
