package SS.power;

import SS.action.monster.EvokeSoulAction;
import SS.helper.ModHelper;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/** Remaining end-of-turn triggers; repeated applications add their remaining counts. */
public class PassingTheTorchPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("PassingTheTorchPower");
    private static final PowerStrings STRINGS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);

    public PassingTheTorchPower(AbstractCreature owner, int amount) {
        ID = POWER_ID;
        name = STRINGS.NAME;
        this.owner = owner;
        this.amount = amount;
        type = PowerType.BUFF;
        isTurnBased = true;
        // The new art is installed after approval; keep previews playable until then.
        String icon = Gdx.files.internal("img/power/PassingTheTorchPower84.png").exists()
                ? "PassingTheTorchPower" : "SoulFirePower";
        region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage("img/power/" + icon + "84.png"), 0, 0, 84, 84);
        region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage("img/power/" + icon + "32.png"), 0, 0, 32, 32);
        updateDescription();
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer && amount > 0) {
            flash();
            addToBot(new EvokeSoulAction(1));
            addToBot(new ReducePowerAction(owner, owner, this, 1));
        }
    }

    @Override
    public void updateDescription() {
        description = STRINGS.DESCRIPTIONS[0] + amount + STRINGS.DESCRIPTIONS[1];
    }
}
