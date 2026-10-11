package SS.power;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.FocusPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

import SS.helper.ModHelper;

public class RerollPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("RerollPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public RerollPower(AbstractPlayer owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = (AbstractCreature) owner;
        this.type = AbstractPower.PowerType.BUFF;
        this.isTurnBased = false;
        // AbstractPower.renderAmount 只在 amount>0、或 amount<0 且 canGoNegative 时才画层数。
        // 本 Power 是靠负数表达的减益，不打开这个开关就永远不显示数字。
        this.canGoNegative = true;

        this.amount = amount;

        String path128 = "img/power/RerollPower84.png";
        String path48 = "img/power/RerollPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    @Override
    public void stackPower(final int stackAmount) {
        // 用 -2 而非 8：8 是原版用来把两位数缩小的字号，
        // 而 renderFontRightTopAligned 会再乘一次 Settings.scale，
        // 单开一个能力时 8 会把数字缩到几乎看不见。
        this.fontScale = -2.0f;
        this.amount += stackAmount;
        if (this.amount == 0) {
            this.addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, this));
        }
        if (this.amount >= 999) {
            this.amount = 999;
        }
        if (this.amount <= -999) {
            this.amount = -999;
        }
    }

    @Override
    public void reducePower(final int reduceAmount) {
        this.fontScale = -2.0f;
        this.amount -= reduceAmount;
        if (this.amount == 0) {
            this.addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, this));
        }
        if (this.amount >= 999) {
            this.amount = 999;
        }
        if (this.amount <= -999) {
            this.amount = -999;
        }
    }

    public void updateDescription() {
        // DESCRIPTIONS[0] 用于负数（减益），DESCRIPTIONS[1] 用于正数（增益）。
        // 文案里没有 #b / #y 之类的占位符，直接拼绝对值即可。
        if (this.amount > 0) {
            this.description = DESCRIPTIONS[1] + this.amount;
            this.type = PowerType.BUFF;
        } else {
            final int tmp = -this.amount;
            this.description = DESCRIPTIONS[0] + tmp;
            this.type = PowerType.DEBUFF;
        }
    }

}
