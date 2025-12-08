package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;

public class DoubleFiendFireAction extends AbstractGameAction {
    private int damage;
    private float startingDuration;

    public DoubleFiendFireAction(AbstractCreature target, int damage) {
        this.damage = damage;
        this.actionType = ActionType.WAIT;
        this.attackEffect = AttackEffect.FIRE;
        this.startingDuration = Settings.ACTION_DUR_FAST;
        this.duration = this.startingDuration;
        this.target = target;
    }

    public void update() {
        int count = AbstractDungeon.player.hand.size();

        int i;
        for (i = 0; i < count; ++i) {
            this.addToTop(new ChannelDiceAction(new AttackDice(this.damage, (AbstractMonster) this.target)));
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
