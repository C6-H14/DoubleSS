package SS.action.dice;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

import SS.Dice.EmptyDiceSlot;

public class EvokeDiceAction extends AbstractGameAction {
    private int num;

    public EvokeDiceAction(int amount, int num) {
        this.amount = amount;
        this.num = num;
    }

    public void update() {
        for (int i = 0; i < AbstractDungeon.player.orbs.size() && i < num; i++) {
            if (((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyDiceSlot)
                    || ((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyOrbSlot)) {
                continue;
            }
            for (int j = 1; j <= this.amount; ++j) {
                ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).triggerEvokeAnimation();
                ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).onEvoke();
            }
            AbstractDungeon.player.orbs.set(i, new EmptyDiceSlot());
            ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).setSlot(i, AbstractDungeon.player.maxOrbs);
        }
        this.isDone = true;
    }
}