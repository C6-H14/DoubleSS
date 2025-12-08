package SS.power;

import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DelayDamagePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("DelayDamagePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private int count = 0;

    public DelayDamagePower(AbstractCreature owner, int amount, int count, int damage) {
        this.name = NAME;
        this.ID = POWER_ID + (count + amount);
        this.owner = owner;
        this.type = AbstractPower.PowerType.DEBUFF;
        this.amount = amount;
        this.count = damage;

        String path128 = "img/power/DelayDamagePower84.png";
        String path48 = "img/power/DelayDamagePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void addCount(int amount) {
        this.count += amount;
        updateDescription();
    }

    public void atEndOfTurn(final boolean isPlayer) {
        addToBot((AbstractGameAction) new ReducePowerAction(this.owner, this.owner, this, 1));
        if (this.amount == 1) {
            DamageInfo info = new DamageInfo(AbstractDungeon.player, this.count, DamageInfoEnum.DELAY);
            addToBot(new DamageAction(AbstractDungeon.player, info));
        }
    }

    public void updateDescription() {
        this.description = this.amount + DESCRIPTIONS[0] + this.count + DESCRIPTIONS[1];
    }

}
