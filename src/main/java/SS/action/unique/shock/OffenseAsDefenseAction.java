package SS.action.unique.shock;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.core.Settings;

import SS.Dice.AttackDice;
import SS.Dice.DefendDice;
import SS.Dice.EmptyDiceSlot;
import SS.action.dice.ChannelDiceAction;

public class OffenseAsDefenseAction extends AbstractGameAction {

    public OffenseAsDefenseAction() {
        this.duration = Settings.ACTION_DUR_MED;
    }

    public void update() {
        for (int i = 0; i < AbstractDungeon.player.orbs.size(); i++) {
            if (((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyDiceSlot)
                    || ((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyOrbSlot)) {
                addToTop(new OffenseAsDefenseAction());
                addToTop(new ChannelDiceAction((AbstractDungeon.cardRandomRng.randomBoolean())
                        ? new AttackDice(1, AbstractDungeon.getRandomMonster())
                        : new DefendDice(i, AbstractDungeon.player)));
                break;
            }
        }
        this.isDone = true;

    }
}