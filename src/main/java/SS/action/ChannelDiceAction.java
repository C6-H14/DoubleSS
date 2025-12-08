package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

public class ChannelDiceAction extends AbstractGameAction {
    private AbstractOrb orbType;

    public ChannelDiceAction(AbstractOrb newOrbType) {
        this.startDuration = 0.1F;
        this.duration = this.startDuration;
        this.orbType = newOrbType;
        this.amount = 1;
    }

    public void update() {
        AbstractDungeon.player.channelOrb(this.orbType);
        if (Settings.FAST_MODE) {
            this.isDone = true;
            return;
        }
        tickDuration();
        this.isDone = true;
    }
}