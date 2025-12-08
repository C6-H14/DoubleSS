package SS.action.unique.lost;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import SS.action.common.ModifyPermanentDamageAction;
import java.util.UUID;

public class LifelineAction extends AbstractGameAction {
    private AbstractPlayer p;
    private boolean freeToPlayOnce = false;
    private int energyOnUse = -1;
    private UUID uid;
    private boolean upgraded, manager;

    public LifelineAction(boolean freeToPlayOnce, int energyOnUse, boolean upgraded, boolean manager) {
        this.actionType = ActionType.DAMAGE;
        this.p = AbstractDungeon.player;
        this.upgraded = upgraded;
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
        if (this.upgraded) {
            ++effect;
        }
        for (int i = 0; i < effect; ++i) {
            addToTop(new LifelinePlayCardAction(1, this.manager));
        }
        addToBot(new ModifyPermanentDamageAction(this.uid, effect));
        this.isDone = true;
    }
}
