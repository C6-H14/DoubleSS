package SS.power;

import SS.Dice.AbstractDice;
import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class BinPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("BinPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public BinPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/BinPower84.png";
        String path48 = "img/power/BinPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    public void onChannel(AbstractOrb orb) {
        if (orb instanceof AbstractDice) {
            int amount = ((AbstractDice) orb).getDiceEvokeAmount();
            if (amount <= 0)
                amount = 0;
            else if (amount == 1)
                amount = 1;
            else if (amount == 2)
                amount = 10;
            else if (amount == 3)
                amount = 11;
            else if (amount == 4)
                amount = 100;
            else if (amount == 5)
                amount = 101;
            else if (amount == 6)
                amount = 110;
            else if (amount == 7)
                amount = 111;
            ((AbstractDice) orb).updateEvokeAmount(amount);
        }
    }

}
