package SS.power;

import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;

public class SunkPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("SunkPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public SunkPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.DEBUFF;

        this.amount = -1;

        String path128 = "img/power/SunkPower84.png";
        String path48 = "img/power/SunkPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public int onAttacked(final DamageInfo info, final int damageAmount) {
        if (damageAmount == 0)
            return 0;
        if (info.type == DamageInfo.DamageType.NORMAL) {
            AbstractCreature p = AbstractDungeon.player;
            for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                if (mo.getPower(this.POWER_ID) != null && mo != this.owner) {
                    addToTop(new DamageAction(mo,
                            new DamageInfo((AbstractCreature) p, damageAmount, DamageInfo.DamageType.THORNS)));
                }
            }
        }
        return damageAmount;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

}
