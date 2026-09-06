package SS.power;

import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;
import SS.patches.GainBlockDiceSource;
import SS.stats.DiceAttribution;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class NextTurnBlockPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("NextTurnBlockPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    /** 战斗统计：产生本 power 的骰子归属快照（EternalDefendDice 构造时携带，可为 null）。 */
    public DiceAttribution diceAttribution = null;

    public NextTurnBlockPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/NextTurnBlockPower84.png";
        String path48 = "img/power/NextTurnBlockPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atStartOfTurn() {
        this.flash();
        GainBlockAction gain = new GainBlockAction(owner, this.amount);
        if (this.diceAttribution != null) {
            GainBlockDiceSource.diceRef.set(gain, this.diceAttribution);
        }
        addToBot(gain);
        addToBot(new RemoveSpecificPowerAction(owner, owner, this));
    }

    public int onAttacked(final DamageInfo info, final int damageAmount) {
        if (damageAmount <= 0)
            return 0;
        if (info.owner != this.owner && info.type != DamageType.HP_LOSS
                || info.owner == this.owner && info.type == DamageInfoEnum.DELAY)
            addToBot(new RemoveSpecificPowerAction(owner, owner, this));
        return damageAmount;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}