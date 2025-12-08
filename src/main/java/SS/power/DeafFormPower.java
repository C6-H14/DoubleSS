package SS.power;

import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;

public class DeafFormPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("DeafFormPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private int count = 0;

    public DeafFormPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;
        this.count = 0;

        String path128 = "img/power/DeafFormPower84.png";
        String path48 = "img/power/DeafFormPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public int onAttacked(final DamageInfo info, final int damageAmount) {
        if (damageAmount == 0)
            return 0;
        if (info.type != DamageInfoEnum.DELAY) {
            AbstractCreature p = AbstractDungeon.player;
            if (p.hasPower("Double:DelayDamagePower" + (this.count + this.amount))) {
                for (AbstractPower q : p.powers) {
                    if (q.ID.equals("Double:DelayDamagePower" + (this.count + this.amount))) {
                        ((DelayDamagePower) q).addCount(damageAmount);
                        break;
                    }
                }
            } else {
                addToTop(new ApplyPowerAction(p, p, new DelayDamagePower(p, this.amount, this.count, damageAmount),
                        this.amount));
            }
            return 0;
        }
        return damageAmount;
    }

    public void atEndOfRound(final boolean isPlayer) {
        count++;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

}
