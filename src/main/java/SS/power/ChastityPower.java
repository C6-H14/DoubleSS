package SS.power;

import SS.helper.ModHelper;

import java.util.Iterator;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;

public class ChastityPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("ChastityPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public ChastityPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/ChastityPower84.png";
        String path48 = "img/power/ChastityPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public int onAttacked(final DamageInfo info, final int damageAmount) {
        this.flash();
        Iterator<AbstractMonster> var3 = (AbstractDungeon.getCurrRoom()).monsters.monsters.iterator();
        while (var3.hasNext()) {
            AbstractMonster mo = var3.next();
            addToBot(new ApplyPowerAction(mo, this.owner, new BleedingPower(mo, this.amount)));
            addToBot(new ApplyPowerAction(mo, this.owner, new OpenInjuryPower(mo, 1)));
        }
        return damageAmount;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

}
