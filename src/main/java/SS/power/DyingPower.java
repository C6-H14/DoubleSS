package SS.power;

import SS.action.common.DieAction;
import SS.helper.ModHelper;
import SS.interfaces.OnReduceDyingPowerSubscriber;
import SS.relic.SS.HolyMantle;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.mod.stslib.powers.interfaces.OnPlayerDeathPower;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
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
        this.priority = 5;
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

    public void atStartOfTurn() {
        if (this.amount == 1) {
            this.flash();
        }
    }

    @Override
    public void reducePower(int reduceAmount) {
        if (this.amount == 0)
            return;

        // 若处于神圣斗篷保护期，直接拦截减层
        if (this.owner.isPlayer && ((AbstractPlayer) this.owner).hasRelic(HolyMantle.ID)) {
            HolyMantle mantle = (HolyMantle) ((AbstractPlayer) this.owner).getRelic(HolyMantle.ID);
            if (mantle != null && mantle.isProtected) {
                return;
            }
        }

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

        // 若处于神圣斗篷保护期，拦截扣除
        if (this.owner.isPlayer && ((AbstractPlayer) this.owner).hasRelic(HolyMantle.ID)) {
            HolyMantle mantle = (HolyMantle) ((AbstractPlayer) this.owner).getRelic(HolyMantle.ID);
            if (mantle != null && mantle.isProtected) {
                return false;
            }
        }

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

    @Override
    public boolean onPlayerDeath(AbstractPlayer p, DamageInfo info) {
        if (p.hasPower("Double:ResurrectionPower"))
            return true;

        HolyMantle mantle = p.hasRelic(HolyMantle.ID) ? (HolyMantle) p.getRelic(HolyMantle.ID) : null;

        // 1. 如果已处于神圣斗篷保护期中再次受到致死伤害：直接免死回血，不失去任何 DyingPower
        if (mantle != null && mantle.isProtected) {
            mantle.flash();
            this.owner.decreaseMaxHealth(10);
            addToBot(new HealAction(this.owner, this.owner, this.owner.maxHealth));
            return false;
        }

        // 2. 正常保命：扣除 (amount - 1) 层 DyingPower
        if (reduce(this.amount - 1)) {
            // 成功因致命伤害失去 DyingPower 后，激活斗篷保护期（直到下回合开始）
            if (mantle != null) {
                mantle.triggerProtection();
            }
            this.owner.decreaseMaxHealth(10);
            addToBot(new HealAction(this.owner, this.owner, this.owner.maxHealth));
            return false;
        }

        return true;
    }

    public void onRemove() {
        addToBot(new DieAction());
    }

    public void atEndOfTurn(boolean isPlayer) {
        reduce(1);
    }
}