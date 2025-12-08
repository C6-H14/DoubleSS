package SS.action;

import java.util.ArrayList;
import java.util.Collections;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

import SS.Dice.AbstractDice;
import SS.Dice.EmptyDiceSlot;

public class RemoveDiceAction extends AbstractGameAction {
    private ArrayList<String> dice = new ArrayList<>();

    public RemoveDiceAction(ArrayList<String> dice, int amount) {
        this.amount = amount;
        this.dice.clear();
        this.dice.addAll(dice);
    }

    public RemoveDiceAction(AbstractDice dice, int amount) {
        this.amount = amount;
        this.dice.clear();
        this.dice.add(dice.ID);
    }

    public RemoveDiceAction(int amount) {
        this.amount = amount;
        this.dice.clear();
        this.dice.add(new EmptyDiceSlot().ID);
    }

    public void update() {
        for (int i = 0; i < AbstractDungeon.player.orbs.size(); i++) {
            if (((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyDiceSlot)
                    || ((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyOrbSlot)) {
                continue;
            }
            if (this.dice.contains(new EmptyDiceSlot().ID)
                    || this.dice.contains(AbstractDungeon.player.orbs.get(i).ID)) {
                --this.amount;
                AbstractDungeon.player.orbs.set(i, new EmptyDiceSlot());
                ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).setSlot(i, AbstractDungeon.player.maxOrbs);
                for (int j = i + 1; j < AbstractDungeon.player.orbs.size(); ++j) {
                    Collections.swap(AbstractDungeon.player.orbs, j, j - 1);
                }
                EmptyDiceSlot emptyDiceSlot = new EmptyDiceSlot((AbstractDungeon.player.orbs.get(i)).cX,
                        (AbstractDungeon.player.orbs.get(i)).cY);
                AbstractDungeon.player.orbs.set(AbstractDungeon.player.orbs.size() - 1, emptyDiceSlot);
                for (int j = i; j < AbstractDungeon.player.orbs.size(); ++j) {
                    (AbstractDungeon.player.orbs.get(j)).setSlot(j, AbstractDungeon.player.maxOrbs);
                }
            }
            if (this.amount == 0)
                break;
        }
        this.isDone = true;
    }
}