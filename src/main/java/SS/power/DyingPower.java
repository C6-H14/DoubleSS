package SS.power;

import SS.action.common.DieAction;
import SS.helper.ModHelper;
import SS.interfaces.OnReduceDyingPowerSubscriber;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.mod.stslib.powers.interfaces.OnPlayerDeathPower;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.cards.DamageInfo;

public class DyingPower extends AbstractPower implements OnPlayerDeathPower {
    public static final String POWER_ID = ModHelper.makePath("DyingPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public DyingPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/DyingPower84.png";
        String path48 = "img/power/DyingPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    @Override
    public void reducePower(int reduceAmount) {
        if (amount == 0)
            return;
        for (AbstractPower p : this.owner.powers) {
            if (p instanceof OnReduceDyingPowerSubscriber) {
                ((OnReduceDyingPowerSubscriber) p).onReduceDyingPower(amount);
            }
        }
        if (this.amount - reduceAmount <= 0) {
            this.fontScale = 8.0F;
            this.amount = 0;
        } else {
            this.fontScale = 8.0F;
            this.amount -= reduceAmount;
        }

    }

    private boolean reduce(int amount) {
        if (amount == 0)
            return false;
        for (AbstractPower p : this.owner.powers) {
            if (p instanceof OnReduceDyingPowerSubscriber) {
                ((OnReduceDyingPowerSubscriber) p).onReduceDyingPower(amount);
            }
        }
        if (this.amount <= amount) {
            addToBot(new DieAction());
            return false;
        }
        addToBot(new ReducePowerAction(this.owner, this.owner, this.ID, amount));
        return true;
    }

    public boolean onPlayerDeath(AbstractPlayer p, DamageInfo info) {
        if (reduce(1)) {
            AbstractDungeon.player.decreaseMaxHealth(10);
            addToBot(new HealAction(this.owner, this.owner, this.owner.maxHealth));
            return false;
        }
        return true;
    }

    public void onRemove() {
        addToBot(new DieAction());
    }

    public void atStartOfTurn() {
        reduce(1);
    }

}
