package SS.action.unique.c6h14;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import SS.action.monster.EvokeSoulAction;

public class PaperPenanceAction extends AbstractGameAction {

    public PaperPenanceAction() {
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        int amount = 0;
        if (AbstractDungeon.player.hasPower("Double:SinsPower")) {
            amount = -AbstractDungeon.player.getPower("Double:SinsPower").amount;
        }
        if (amount >= 5) {
            addToBot(new EvokeSoulAction(amount / 5));
        }

        this.isDone = true;
    }
}