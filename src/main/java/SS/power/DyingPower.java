package SS.power;

import SS.action.common.DieAction;
import SS.helper.ModHelper;
import SS.interfaces.OnReduceDyingPowerSubscriber;
import SS.relic.SS.HolyMantle;
import SS.stats.CardStats;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.mod.stslib.powers.interfaces.OnPlayerDeathPower;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
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
    private boolean isResurrencted = false;

    // 【方案B】呼吸警报动画计时器
    private float pulseTimer = 0.0F;

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

    @Override
    public void updateParticles() {
        super.updateParticles();

        // 仅在只剩 1 层时累计动画时间
        if (this.amount == 1) {
            this.pulseTimer += Gdx.graphics.getDeltaTime() * 5.0F; // 速度系数，5.0F 约为 1 秒呼吸 1 次
        } else {
            this.pulseTimer = 0.0F;
        }
    }

    /**
     * 【方案B核心】：自定义渲染图标，实现平滑红色心跳呼吸灯效果
     */
    @Override
    public void renderIcons(SpriteBatch sb, float x, float y, Color c) {
        if (this.amount == 1 && this.region48 != null) {
            // 利用正弦波计算 0.0 ~ 1.0 的平滑波动值
            float sinVal = (MathUtils.sin(this.pulseTimer) + 1.0F) / 2.0F;

            // 1. 颜色呼吸变换：在 原色 和 纯血红色 之间平滑过渡
            Color warningColor = c.cpy().lerp(new Color(1.0F, 0.15F, 0.15F, c.a), sinVal * 0.85F);
            sb.setColor(warningColor);

            // 2. 尺寸微弱缩放（心跳跳动感）：正常大小 ~ 1.15 倍大小
            float scale = (1.0F + sinVal * 0.15F) * Settings.scale;

            sb.draw(this.region48,
                    x - (float) this.region48.packedWidth / 2.0F,
                    y - (float) this.region48.packedHeight / 2.0F,
                    (float) this.region48.packedWidth / 2.0F,
                    (float) this.region48.packedHeight / 2.0F,
                    (float) this.region48.packedWidth,
                    (float) this.region48.packedHeight,
                    scale, scale,
                    0.0F);
        } else {
            // 大于 1 层时走正常渲染
            super.renderIcons(sb, x, y, c);
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
            CardStats.onDyingSaved();
            return false;
        }

        // 2. 正常保命：扣除 (amount - 1) 层 DyingPower
        if (!isResurrencted && reduce(this.amount - 1)) {
            // 成功因致命伤害失去 DyingPower 后，激活斗篷保护期（直到下回合开始）
            if (mantle != null) {
                mantle.triggerProtection();
            }
            this.owner.decreaseMaxHealth(10);
            this.isResurrencted = true;
            addToBot(new HealAction(this.owner, this.owner, this.owner.maxHealth));
            CardStats.onDyingSaved();
            return false;
        }

        return true;
    }

    public void onRemove() {
        addToBot(new DieAction());
    }

    public void stackPower(final int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        if (this.amount > 10) {
            this.amount = 10;
        }
    }

    public void atEndOfTurn(boolean isPlayer) {
        reduce(1);
    }
}