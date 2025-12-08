package SS.power;

import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.DamageAction;

public class SinsPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("SinsPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public SinsPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/SinsPower84.png";
        String path48 = "img/power/SinsPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    public void stackPower(int stackAmount) {
        int temp = this.amount;
        super.stackPower(stackAmount);
        if (this.amount > temp) {
            AbstractDungeon.player.decreaseMaxHealth(this.amount);
        }
    }

    public void onInitialApplication() {
        AbstractDungeon.player.decreaseMaxHealth(this.amount);
    }

    public void atEndOfTurn(final boolean isPlayer) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p.hasPower("Double:SulfurBlackPower"))
            return;
        this.flash();
        addToBot(new DamageAction(p, new DamageInfo(p, this.amount)));
    }

}
