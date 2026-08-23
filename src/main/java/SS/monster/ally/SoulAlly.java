package SS.monster.ally;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.InflameEffect;

import SS.action.monster.SoulColorCycleAction;
import SS.cards.C6H14.Covenant;
import SS.cards.C6H14.Revelation;
import SS.helper.ModHelper;
import SS.helper.MonsterIntentSimulator;
import SS.power.InscribeCardPower;
import SS.power.SoulFirePower;

/**
 * 魂火跟班：一个会"染色"的友军。
 * - 血量与"魂火"能力双向绑定（见 {@link #syncSoulFire}）；
 * - 可被染成 白/红/绿/蓝/紫，改变吸牌类型与意图；
 * - 在"创世记"环境下，已染色的跟班会打出《圣约》并整体缩小卡牌 UI。
 */
public class SoulAlly extends AbstractAlly {
    public static final String ID = ModHelper.makePath("SoulAlly");
    private static final MonsterStrings MONSTER_STRINGS = CardCrawlGame.languagePack.getMonsterStrings(ID);
    public static final String NAME = MONSTER_STRINGS.NAME;
    private static final String IMG_PATH = "img/monster/ally/SoulAlly.png";

    private static final int MAX_HP = 3; // 最大生命值
    private static final int DAMAGE_AMOUNT = 3; // 《圣约》基础伤害
    public int slotIndex = -1; // 召唤槽位（EnterEvokeSoulEnvAction 使用）

    // =====================================================================
    // 1. 颜色枚举与定义
    // =====================================================================
    public enum SoulColor {
        WHITE, RED, GREEN, BLUE, PURPLE
    }

    public SoulColor stateColor = SoulColor.WHITE; // 当前的逻辑状态

    // 渐变控制变量
    private Color colorStart = Color.WHITE.cpy(); // 渐变起始色
    private Color colorTarget = Color.WHITE.cpy(); // 渐变目标色
    private float colorTimer = 0.0f;
    private float colorDuration = 1.0f; // 渐变耗时 (秒)
    private boolean isChangingColor = false;

    public SoulAlly(float offsetX, float offsetY) {
        // 注意最后一个参数 60.0F：Hitbox 比脚底板高 60 像素，
        // 这样格挡碎裂特效会出现在半空中，而不是脚底。
        super(NAME, ID, MAX_HP, IMG_PATH, TauntType.OVERFLOW,
                offsetX, offsetY - 50.0F * Settings.scale, 0F, -10F, 150F, 250F,
                MAX_HP, 60.0F);

        // UI 微调（基线值）。注意：能量/能力/卡牌/手牌这些参量每帧都会被 update() 重设，
        // 所以真正的"每帧生效值"以 update() 为准（见下方 update 中的【每帧 UI 参数】段）。
        this.energyOffsetX = 10.0F;
        this.energyOffsetY = 30.0F;
        this.energyScale = 0.7F; // 能量球图标（缩小）
        this.energyTextScale = 0.4F; // 能量数字字体（缩小）
        this.powerIconScale = 0.8F;
        this.powerTextScale = 1.0F;

        this.cardScale = 0.25F;
        this.hoverScale = 0.75F;

        this.handOffsetX = 0.0F;
        this.handOffsetY = -80.0F;

        // 给友军挂上"铭刻"能力（用于暂存吸来的牌）。必须在 init() 之前加，
        // 这样 refreshIntentCalculation/applyPowers 跑的时候它已经在 powers 里了。
        this.powers.add(new InscribeCardPower(this, null));

        this.cardIntentOffsetX = 0.0F; // 卡牌意图队列：觉得偏左/偏右改这个
        this.cardIntentOffsetY = 300.0F; // 觉得太低/太高改这个（像素）
        this.cardIntentSpacing = 35.0F; // 卡牌之间的间距

        this.tint.color = Color.WHITE.cpy();
        init();

        syncSoulFire();
        this.preDrawNumber = this.drawPerTurn = 1;
    }

    // 【唯一需要你写逻辑的地方】：定义这个友军带什么牌
    @Override
    protected ArrayList<AbstractCard> getInitialDeck() {
        ArrayList<AbstractCard> list = new ArrayList<>();
        list.add(new Revelation());
        list.add(new Revelation());
        return list;
    }

    // 【核心同步方法】根据当前血量反推魂火值和最大生命值
    public void syncSoulFire() {
        if (this.isDead || this.isDying)
            return;

        // 1. 计算魂火值：血量 / 3 向上取整
        int newValue = com.badlogic.gdx.math.MathUtils.ceil(this.currentHealth / 3.0f);
        if (newValue <= 0)
            newValue = 1; // 保底

        // 2. 强行修正最大生命值
        if (!AbstractDungeon.player.hasPower("Double:BacktrackingPower"))
            this.maxHealth = newValue * 3;
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }

        // 3. 更新 Power 状态
        AbstractPower p = this.getPower("Double:SoulFirePower");
        if (p == null) {
            // 如果没有，直接添加（不用 Action，因为状态必须立刻同步）
            this.powers.add(new SoulFirePower(this, newValue));
        } else {
            if (AbstractDungeon.player.hasPower("Double:BacktrackingPower")) {
                p.amount = Math.max(p.amount, newValue);
            } else {
                p.amount = newValue; // 直接修改 Power 的值
            }
            p.updateDescription();
        }

        // 4. 刷新血条 UI 和 意图伤害 (因为魂火值变了，伤害也变了)
        this.healthBarUpdatedEvent();
        this.refreshIntentCalculation();
    }

    @Override
    public void refreshIntentCalculation() {
        // 判断当前环境卡是否为"创世记"
        boolean isGenesisActive = SS.helper.EnvironmentManager.inst.activeCard != null &&
                SS.helper.EnvironmentManager.inst.activeCard.getEnvironmentID().equals("Double:GENESIS");

        // 【核心修正】只有在 创世记 结界下 且 已经染过色，意图才是《圣约》
        if (isGenesisActive && this.stateColor != SoulColor.WHITE) {
            int soulFireVal = 1;
            AbstractPower p = this.getPower("Double:SoulFirePower");
            if (p != null)
                soulFireVal = p.amount;

            int testamentDmg = DAMAGE_AMOUNT + soulFireVal;

            this.intentResult = new MonsterIntentSimulator.SimulationResult();
            this.intentResult.totalDamage = testamentDmg;

            this.isReadingSimulation = true;
            this.setMove((byte) 1, Intent.ATTACK, testamentDmg);
            this.createIntent();
            this.isReadingSimulation = false;

            this.intentResult.cardsToPlay.clear();
        } else {
            // 普通状态：正常走手牌模拟器
            super.refreshIntentCalculation();
        }
    }

    @Override
    public void atEndOfTurn() {
        boolean isGenesisActive = SS.helper.EnvironmentManager.inst.activeCard != null &&
                SS.helper.EnvironmentManager.inst.activeCard.getEnvironmentID().equals("Double:GENESIS");

        // 【核心修正】只有在 创世记 结界下 且 已经染过色，才打出《圣约》
        if (isGenesisActive && this.stateColor != SoulColor.WHITE) {
            AbstractCard testament = new Revelation();
            testament.freeToPlayOnce = true;

            addToBot(new SS.action.monster.AllyPlayCardAction(this, testament, this.getTarget()));

            addToBot(new com.megacrit.cardcrawl.actions.AbstractGameAction() {
                @Override
                public void update() {
                    SoulAlly.this.endTurnDeckLogic();
                    SoulAlly.this.lockedTarget = null;
                    this.isDone = true;
                }
            });
        } else {
            // 正常状态下：像普通跟班一样，正常结算自己的手牌 AI
            super.atEndOfTurn();
        }
    }

    // 【拦截受伤】
    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        // 受伤后立刻同步魂火值
        syncSoulFire();
    }

    public void updateIntent(int num) {
    }

    @Override
    public void getMove(int num) {
        this.updateIntent(num);
    }

    @Override
    public void takeTurn() {
        // 要加一个 rollmove 的 action，重 roll 怪物的意图
        addToBot(new RollMoveAction(this));
    }

    public void changeColor(SoulColor newColor) {
        if (this.stateColor == newColor)
            return;
        boolean isGenesisActive = SS.helper.EnvironmentManager.inst.activeCard != null &&
                SS.helper.EnvironmentManager.inst.activeCard.getEnvironmentID().equals("Double:GENESIS");
        if (isGenesisActive && this.stateColor != SoulColor.WHITE) {
            return;
        }

        this.stateColor = newColor;

        // 1. 记录起点 (当前 Tint 的颜色)
        this.colorStart = this.tint.color.cpy();

        // 2. 设定终点 (目标颜色)
        switch (newColor) {
            case WHITE:
                this.colorTarget = Color.WHITE.cpy();
                break;
            // 建议稍微调亮一点，避免纯色导致画面太暗
            case RED:
                this.colorTarget = new Color(1.0f, 0.1f, 0.1f, 1.0f);
                break;
            case GREEN:
                this.colorTarget = new Color(0.2f, 1.0f, 0.2f, 1.0f);
                break;
            case BLUE:
                this.colorTarget = new Color(0.2f, 0.6f, 1.0f, 1.0f);
                break; // 天蓝
            case PURPLE:
                this.colorTarget = new Color(0.8f, 0.2f, 1.0f, 1.0f);
                break;
        }
        this.colorTimer = 0.0f;
        this.isChangingColor = true;

        // 4. 播放特效 (增加打击感)
        AbstractDungeon.effectsQueue.add(new BorderFlashEffect(this.colorTarget));
        // 如果是红色，冒火；如果是其他，可以加其他特效
        if (newColor == SoulColor.RED) {
            AbstractDungeon.effectsQueue.add(new InflameEffect(this));
        }
        AbstractCard.CardType type = AbstractCard.CardType.SKILL; // 默认
        switch (newColor) {
            case RED:
                type = AbstractCard.CardType.ATTACK;
                break;
            case GREEN:
                type = AbstractCard.CardType.SKILL;
                break;
            case BLUE:
                type = AbstractCard.CardType.POWER;
                break;
            case PURPLE:
                type = AbstractCard.CardType.STATUS;
                break; // 紫色: 其他 (通常归类为 Skill 或 Status/Curse，看你需求)
            default:
                return; // 白色不吸牌
        }

        // 触发吸牌逻辑
        AbstractDungeon.actionManager.addToBottom(new SoulColorCycleAction(this, type));
    }

    // =====================================================================
    // 3. Update 中执行渐变 + 每帧锁定 UI 参数
    // =====================================================================
    @Override
    public void update() {
        super.update();

        // 1. 颜色渐变与锁定逻辑
        if (isChangingColor) {
            this.colorTimer += Gdx.graphics.getDeltaTime();
            float progress = Math.min(1.0f, this.colorTimer / this.colorDuration);
            this.tint.color.set(colorStart).lerp(colorTarget, Interpolation.pow2Out.apply(progress));
            if (progress >= 1.0f)
                isChangingColor = false;
        } else if (this.stateColor != SoulColor.WHITE) {
            this.tint.color.set(this.colorTarget);
        }

        // =================================================================
        // 【每帧 UI 参数】—— 调这些 UI 元素的位置/大小，请改这一段。
        // 这里每帧重设，是为了防止其他逻辑意外改动它们（比如 ExitEvokeSoulEnvAction
        // 会在退出创世记时把 modelScale 重置回 1.0f）。
        // 注意：立绘高度/碰撞箱大小由构造阶段的自变量公式定死，这里不碰 hb。
        // =================================================================
        this.modelScale = 1.0f; // 立绘缩放

        // 能量面板 + 能力图标（永久保持缩小）
        this.energyScale = 0.7F; // 能量球图标大小
        this.energyTextScale = 0.4F; // 能量数字字体大小
        this.energyOffsetX = 10.0F;
        this.energyOffsetY = 30.0F;
        this.powerIconScale = 0.8F;
        this.powerTextScale = 1.0F;

        // 检测"创世记"环境是否激活
        boolean isGenesisActive = SS.helper.EnvironmentManager.inst.activeCard != null &&
                SS.helper.EnvironmentManager.inst.activeCard.getEnvironmentID().equals("Double:GENESIS");

        if (isGenesisActive) {
            // 创世记：卡牌整体缩小、贴近头部
            this.cardScale = 0.15F;
            this.hoverScale = 0.45F;
            this.handOffsetY = -120.0F;
            this.handOffsetX = 0.0F;
        } else {
            // 正常状态：卡牌大小
            this.cardScale = 0.25F;
            this.hoverScale = 0.75F;
            this.handOffsetY = -80.0F;
            this.handOffsetX = 0.0F;
        }

        syncSoulFire();
    }

    @Override
    public void die(boolean triggerRelics) {
        // 1. 检查友军身上是否有"铭刻"能力
        if (this.hasPower("Double:InscribeCardPower")) {
            InscribeCardPower power = (InscribeCardPower) this.getPower("Double:InscribeCardPower");

            // 2. 检查能力中是否存有卡牌
            if (power.card != null) {
                AbstractCard c = power.card;
                c.costForTurn = c.cost;
                c.isCostModified = false;
                c.freeToPlayOnce = false;

                // 【核心】视觉特效与逻辑结算：
                // ShowCardAndAddToDiscardEffect 包办了"展示卡牌 -> 飞向右下角 -> 塞入玩家弃牌堆"，
                // 特效起始坐标设为友军中心点 (hb.cX, hb.cY)
                AbstractDungeon.effectList.add(
                        new com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect(c, this.hb.cX,
                                this.hb.cY));

                // 清空记录
                power.card = null;
            }
        }

        // 最后必须调用父类的死亡逻辑
        super.die(triggerRelics);
    }

    // =====================================================================
    // 进入创世记：洗牌，手牌变为1张圣约，抽牌堆塞2张圣约
    // =====================================================================
    public void transformToGenesisDeck() {
        // 1. 彻底清空所有战斗牌堆
        this.hand.clear();
        this.drawPile.clear();
        this.discardPile.clear();
        this.exhaustPile.clear();
        this.limbo.clear();

        // 2. 放入 1 张圣约到手牌
        this.hand.addToTop(new Covenant());

        // 3. 放入 2 张圣约到抽牌堆
        this.drawPile.addToBottom(new Covenant());
        this.drawPile.addToBottom(new Covenant());

        // 4. 重新计算并刷新视觉、意图
        this.refreshIntentCalculation();
    }

    // =====================================================================
    // 退出创世记：清空牌堆，手牌改回1张启示，抽牌堆塞1张启示
    // =====================================================================
    public void revertFromGenesisDeck() {
        // 1. 初始化原版牌堆 (清空牌堆，并将原版的 2 张《启示》放入抽牌堆)
        this.initBattleDeck();

        // 2. 抽 1 张牌到手牌中 (恢复战斗开始时的 1 手牌、1 抽牌堆的状态)
        this.drawCard();

        // 3. 重新计算并刷新视觉、意图
        this.refreshIntentCalculation();
    }

}
