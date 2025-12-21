package SS.power;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;

public class OffenseAsDefensePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("OffenseAsDefensePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public OffenseAsDefensePower(AbstractPlayer owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = (AbstractCreature) owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/OffenseAsDefensePower84.png";
        String path48 = "img/power/OffenseAsDefensePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atStartOfTurn() {
        addToBot(new ReducePowerAction(owner, owner, this, 1));
    }

    public void onAttack(final DamageInfo info, final int damageAmount, final AbstractCreature target) {
        if (info.type != DamageType.HP_LOSS && info.type != DamageInfoEnum.DELAY && target != this.owner) {
            addToBot(new GainBlockAction(owner, damageAmount));
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

}
