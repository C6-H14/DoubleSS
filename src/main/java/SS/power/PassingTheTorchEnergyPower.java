package SS.power;

import SS.action.monster.GainAllyEnergyAction;
import SS.helper.ModHelper;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/** Separate duration keeps unenhanced casts from extending the energy bonus. */
public class PassingTheTorchEnergyPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("PassingTheTorchEnergyPower");
    private static final PowerStrings STRINGS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);

    public PassingTheTorchEnergyPower(AbstractCreature owner, int amount) {
        ID = POWER_ID;
        name = STRINGS.NAME;
        this.owner = owner;
        this.amount = amount;
        type = PowerType.BUFF;
        isTurnBased = true;
        region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage("img/power/PassingTheTorchPower84.png"), 0, 0, 84, 84);
        region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage("img/power/PassingTheTorchPower32.png"), 0, 0, 32, 32);
        updateDescription();
    }

    @Override
    public void atEndOfTurnPreEndTurnCards(boolean isPlayer) {
        if (isPlayer && amount > 0) {
            // callEndOfTurnActions invokes pre-card powers before AllyHookPatch's
            // postfix queues ally planners. Grant energy before they choose cards;
            // the next reset happens only at the following player turn start.
            flash();
            addToBot(new GainAllyEnergyAction(1));
            addToBot(new ReducePowerAction(owner, owner, this, 1));
        }
    }

    @Override
    public void updateDescription() {
        description = STRINGS.DESCRIPTIONS[0] + amount + STRINGS.DESCRIPTIONS[1];
    }
}
