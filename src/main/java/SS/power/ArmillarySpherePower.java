package SS.power;

import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;

public class ArmillarySpherePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("ArmillarySpherePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public ArmillarySpherePower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = -1;

        String path128 = "img/power/ArmillarySpherePower84.png";
        String path48 = "img/power/ArmillarySpherePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    public void atStartOfTurn() {
        addToBot(new DrawCardAction(getShock(this.owner)));
    }

    protected int getShock(AbstractCreature m) {
        int amount = 0;
        if (m.hasPower("Vulnerable") && m.hasPower("Weakened")) {
            amount = Math.min(m.getPower("Vulnerable").amount, m.getPower("Weakened").amount);
        }
        return amount;
    }

}
