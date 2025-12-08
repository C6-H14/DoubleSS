package SS.Dice;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.helper.ModHelper;
import SS.power.BleedingPower;
import SS.power.OpenInjuryPower;

public class WitherDice extends AbstractDice {
    public static final String ORB_ID = ModHelper.makePath("WitherDice");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    private static final float PI_DIV_16 = 0.19634955F;
    private static final float ORB_WAVY_DIST = 0.05F;
    private static final float PI_4 = 12.566371F;
    private static final float ORB_BORDER_SCALE = 1.2F;

    public WitherDice(int amount, AbstractMonster m) {
        this.ID = ORB_ID;
        this.img = ImageMaster.loadImage("img/dice/WitherDice.png");
        this.name = orbString.NAME;
        this.baseEvokeAmount = amount;
        this.evokeAmount = this.baseEvokeAmount;
        this.channelAnimTimer = 0.5F;
        this.angle = MathUtils.random(360.0F);
        this.myColor = CardHelper.getColor(249, 0, 0);
        this.target = m;
        this.faces = 4;
        updateDescription();
    }

    public void updateDescription() {
        this.description = orbString.DESCRIPTION[0] + this.evokeAmount + orbString.DESCRIPTION[1];
    }

    public void myEvoke() {
        int temp = result, amount = this.evokeAmount;
        if (temp == 1) {
            amount--;
        }
        AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this.target, this.target, new BleedingPower(this.target, amount), amount));
        if (temp == 4) {
            AbstractDungeon.actionManager
                    .addToBottom(
                            new ApplyPowerAction(this.target, this.target, new OpenInjuryPower(this.target, 1), 1));
        }
    }

    public AbstractDice makeCopy() {
        return new WitherDice(this.baseEvokeAmount, (AbstractMonster) this.target);
    }
}