package SS.power;

import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;

public class BleedingPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("BleedingPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public BleedingPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.DEBUFF;

        this.amount = amount;

        String path128 = "img/power/BleedingPower84.png";
        String path48 = "img/power/BleedingPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public float atDamageReceive(float damage, DamageInfo.DamageType type) {
        if (type == DamageInfo.DamageType.NORMAL || type == DamageType.THORNS || type == DamageInfoEnum.DELAY) {
            return damage + this.amount;
        }
        return damage;
    }

    public int onAttacked(final DamageInfo info, final int damageAmount) {
        if (damageAmount <= 0)
            return 0;
        if (info.type == DamageType.NORMAL || info.type == DamageType.THORNS || info.type == DamageInfoEnum.DELAY) {
            this.flashWithoutSound();
            addToTop(new ApplyPowerAction(this.owner, this.owner, new BleedingPower(this.owner, 1)));
        }
        return damageAmount;
    }

    public void atStartOfTurn() {
        if ((AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.COMBAT
                && !AbstractDungeon.getMonsters().areMonstersBasicallyDead() &&
                !this.owner.hasPower(OpenInjuryPower.POWER_ID)) {
            addToBot(new GainBlockAction(this.owner, this.amount));
            addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

}
