package SS.action;

import com.evacipated.cardcrawl.mod.stslib.patches.core.AbstractCreature.TempHPField;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.BufferPower;
import com.megacrit.cardcrawl.powers.IntangiblePower;
import com.megacrit.cardcrawl.powers.RepairPower;

public class KindnessAction extends AbstractGameAction {
    private AbstractPlayer p;
    private int num;

    public KindnessAction(AbstractPlayer p, int amount, int num) {
        this.amount = amount;
        this.target = this.p = p;
        this.num = num;
    }

    public void update() {
        this.amount = this.amount - p.currentHealth
                - ((Integer) TempHPField.tempHp.get(AbstractDungeon.player)).intValue();
        addToBot(new ApplyPowerAction(p, p, new BufferPower(p, amount / num),
                amount / num));
        addToBot(new ApplyPowerAction(p, p, new RepairPower(p, amount), amount));

        this.isDone = true;
    }
}