package SS.power;

import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class SunbowPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("SunbowPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public SunbowPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/SunbowPower84.png";
        String path48 = "img/power/SunbowPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    /** One trigger per actual virtue gain, irrespective of how many points were gained. */
    public void onVirtueGained() {
        if (!SS.cards.AbstractDoubleCard.isInCombat()) return;
        if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) return;
        flash();
        addToBot(new SS.action.monster.GainAllyEnergyAction(amount));
        addToBot(new SS.action.unique.c6h14.CommandAllSoulsAction());
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

}
