package SS.power;

import SS.helper.ModHelper;
import SS.relic.SS.DemonReward;
import SS.relic.SS.HolyReward;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.mod.stslib.powers.interfaces.InvisiblePower;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.DamageAction;

public class SinsPower extends AbstractPower implements InvisiblePower {
    public static final String POWER_ID = ModHelper.makePath("SinsPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private static final int max_sin = 50, min_sin = -50;
    private boolean lock;

    public SinsPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;
        this.lock = false;

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
        if (this.lock)
            return;
        this.amount += stackAmount;
        this.amount = Math.min(this.amount, max_sin);
        this.amount = Math.max(this.amount, min_sin);
        if (this.amount == max_sin || this.amount == min_sin) {
            this.lock = true;
        }
    }

    public void reducePower(int stackAmount) {
        if (this.lock)
            return;
        this.amount -= stackAmount;
        this.amount = Math.min(this.amount, max_sin);
        this.amount = Math.max(this.amount, min_sin);
        if (this.amount == max_sin || this.amount == min_sin) {
            this.lock = true;
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
        if (this.amount >= 10)
            addToBot(new DamageAction(p, new DamageInfo(p, this.amount / 10)));
    }

    public void onVictory() {
        if (this.amount >= 10) {
            AbstractDungeon.player.decreaseMaxHealth(this.amount / 10);
        } else if (this.amount <= -10) {
            AbstractDungeon.player.increaseMaxHp(-amount / 10, true);
        }
        if (this.amount == max_sin) {
            if (AbstractDungeon.getCurrRoom() != null) {
                AbstractDungeon.getCurrRoom().addRelicToRewards(new DemonReward());
            }
            return;
        }
        if (this.amount == min_sin) {
            if (AbstractDungeon.getCurrRoom() != null) {
                AbstractDungeon.getCurrRoom().addRelicToRewards(new HolyReward());
            }
            return;
        }
    }
}
