package SS.power;

import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;

public class ContemptPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("ContemptPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public ContemptPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/ContemptPower84.png";
        String path48 = "img/power/ContemptPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atEndOfRound() {
        if (this.owner.hasPower("Strength")) {
            System.out.println("Yes");
            if (this.owner.getPower("Strength").amount > 0) {
                System.out.println("YES");
                for (int i = 0; i < this.amount; ++i) {
                    addToBot(new ApplyPowerAction(this.owner, this.owner,
                            new StrengthPower(this.owner, this.owner.getPower("Strength").amount),
                            this.owner.getPower("Strength").amount));
                }
            }
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

}
