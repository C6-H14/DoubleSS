package SS.power;

import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class AggressionPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("AggressionPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public AggressionPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.DEBUFF;

        this.amount = amount;

        String path128 = "img/power/AggressionPower84.png";
        String path48 = "img/power/AggressionPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public float atDamageReceive(float damage, DamageInfo.DamageType type) {
        if (type == DamageType.NORMAL || type == DamageType.THORNS) {
            return damage + damage * this.amount * 0.5F;
        }
        return damage;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + (this.amount * 50) + DESCRIPTIONS[1];
    }

}
