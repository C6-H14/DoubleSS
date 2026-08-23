package SS.monster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.action.monster.AllyPlayCardAction;
import SS.helper.MonsterCardContext;
import SS.helper.MonsterIntentSimulator;
import basemod.ReflectionHacks;
import basemod.abstracts.CustomMonster;

/**
 * 卡牌怪物基类：让怪物/友军带一套手牌，并把"原版意图 UI"和"自定义 UI"彻底解耦。
 *
 * <p>渲染约定：本类 {@link #render} 完整重写且不调 {@code super.render()}，
 * 另外把原版意图的 4 个绘制方法（{@code renderIntent/renderDamageRange/
 * renderIntentVfxBehind/renderIntentVfxAfter}）覆写为 no-op —— 双保险屏蔽原版意图
 * 的图标/伤害数字/粒子，但意图的<b>状态与决策逻辑</b>（intent/intentDmg/createIntent/
 * 模拟器）照常运行。
 */
public abstract class AbstractCardMonster extends CustomMonster {

    // =====================================================================
    // 牌堆数据
    // masterDeck 为"母版牌堆"，未来可用于记录/还原初始牌组（保留给子类使用）。
    // =====================================================================
    public CardGroup masterDeck;
    public CardGroup drawPile;
    public CardGroup hand;
    public CardGroup discardPile;
    public CardGroup exhaustPile;
    public CardGroup limbo; // 悬空堆，用于播放打牌动画
    private ArrayList<UUID> cardSortOrder = new ArrayList<>();
    public int preDrawNumber = 3; // usePreBattleAction 时初始抽牌张数
    public int drawPerTurn = 2; // 每回合 atStartOfTurn 抽牌张数

    // 能量数据
    public int energyBase;
    public int energy = 0;
    private Color energyColor = Color.WHITE.cpy();

    // =====================================================================
    // 【UI 参量总览】调各 UI 元素的位置 / 大小，改下面对应字段即可。
    // 所有 Offset 的语义都是 "屏幕像素 × Settings.scale"，方向：X 向右为正、Y 向上为正。
    //
    //   立绘 sprite          -> modelScale（整体缩放）
    //   血条 / 名字          -> 跟随 hb（见 AbstractAlly.commonInit 里对 hb 的 move，
    //                           用 healthBarOffsetY 抬高碰撞箱）
    //   提示框 renderTip     -> 用 hb.cX/hb.width 定位，自动贴合，无需参量
    //   手牌（头顶扇形）     -> cardScale, hoverScale, handOffsetX/Y,
    //                           handSpacing, handHoverLift
    //   能量球 + 数字        -> energyScale（球）, energyTextScale（数字字体）,
    //                           energyOffsetX/Y（相对 hb 左下角）
    //   能力图标 + 数字      -> powerIconScale, powerTextScale, powerOffsetX/Y,
    //                           powerIconWidth, powerIconStartOffset,
    //                           powerNumberXOffset/YOffset
    //   卡牌意图队列         -> cardIntentOffsetX/Y, cardIntentSpacing
    // =====================================================================

    // ---- 立绘 ----
    public float modelScale = 1.0f; // 立绘整体缩放（注意：子类若每帧在 update 里改它，会以 update 为准）

    // ---- 卡牌意图队列（独立于原版意图，坐标完全参数化）----
    public float cardIntentOffsetX = 0.0F; // 左右偏移（正往右、负往左）
    public float cardIntentOffsetY = 280.0F; // 上下高度（越大卡牌意图越高）
    public float cardIntentSpacing = 30.0F; // 多张卡牌意图之间的间距

    // ---- 血条 / 碰撞箱 ----
    public float healthBarOffsetY = -20.0F; // 抬高 hb（进而抬高血条/名字/提示框锚点）；构造时传入

    // ---- 手牌 ----
    public float cardScale = 0.3f; // 手牌默认大小
    public float hoverScale = 0.6f; // 手牌悬停放大
    public float handOffsetX = 0.0F; // 手牌整体左右偏移（相对 hb.cX）
    public float handOffsetY = 150.0F; // 手牌整体上下偏移（相对 hb 顶部）
    public float handSpacing = 110.0F; // 手牌扇形间距基准（实际间距 = handSpacing × cardScale）
    public float handHoverLift = 50.0F; // 悬停时卡牌抬升高度

    // ---- 能量面板 ----
    public float energyOffsetX = 0.0F; // 能量球相对 hb 左下角 (hb.x, hb.y) 的水平偏移
    public float energyOffsetY = 0.0F; // 能量球相对 hb 左下角的垂直偏移
    public float energyScale = 0.8F; // 能量球（图标）缩放
    public float energyTextScale = 0.7F; // 能量数字字体缩放（要更小的数字就调低，如 0.4f）

    // ---- 能力图标 ----
    public float powerIconScale = 0.8F; // 能力图标缩放
    public float powerTextScale = 1.0F; // 能力数字字体缩放
    public float powerOffsetX = 0.0F; // 能力图标整体左右微调
    public float powerOffsetY = -5.0F; // 能力图标整体上下微调
    public float powerIconWidth = 48.0F; // 能力图标基准宽度（原版 48px）
    public float powerIconStartOffset = 10.0F; // 第一枚图标相对起点的水平偏移
    public float powerNumberXOffset = 32.0F; // 能力数字相对图标的水平偏移
    public float powerNumberYOffset = 66.0F; // 能力数字相对起点的垂直偏移

    // 意图模拟结果
    protected MonsterIntentSimulator.SimulationResult intentResult;
    public boolean isReadingSimulation = false; // 被 MonsterIntentPatches 读取的"模拟锁"

    // =====================================================================
    // 构造函数
    // =====================================================================

    // 构造函数 1（带立绘偏移 offsetX/offsetY）
    public AbstractCardMonster(
            String name, String id, int maxHealth,
            float hb_x, float hb_y, float hb_w, float hb_h,
            String imgUrl,
            float offsetX, float offsetY,
            int energyBase,
            float healthBarOffsetY) {
        super(name, id, maxHealth, hb_x, hb_y, hb_w, hb_h, imgUrl, offsetX, offsetY);
        this.energyBase = energyBase;
        this.healthBarOffsetY = healthBarOffsetY;
        initializeDecks();
    }

    // 构造函数 2（无立绘偏移；把意图碰撞箱清零并把 intentOffsetX 甩到无穷远，
    // 作为"屏蔽原版意图"的第二道保险）
    public AbstractCardMonster(
            String name, String id, int maxHealth,
            float hb_x, float hb_y, float hb_w, float hb_h,
            String imgUrl,
            int energyBase,
            float healthBarOffsetY) {
        super(name, id, maxHealth, hb_x, hb_y, hb_w, hb_h, imgUrl);
        this.energyBase = energyBase;
        this.healthBarOffsetY = healthBarOffsetY;
        this.intentHb.width = 0.0F;
        this.intentHb.height = 0.0F;
        this.intentOffsetX = -99999.0F;
        initializeDecks();
    }

    private void initializeDecks() {
        this.masterDeck = new CardGroup(CardGroup.CardGroupType.MASTER_DECK);
        this.drawPile = new CardGroup(CardGroup.CardGroupType.DRAW_PILE);
        this.hand = new CardGroup(CardGroup.CardGroupType.HAND);
        this.discardPile = new CardGroup(CardGroup.CardGroupType.DISCARD_PILE);
        this.exhaustPile = new CardGroup(CardGroup.CardGroupType.EXHAUST_PILE);
        this.limbo = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
    }

    protected abstract ArrayList<AbstractCard> getInitialDeck();

    public void initBattleDeck() {
        this.drawPile.clear();
        this.hand.clear();
        this.discardPile.clear();
        this.exhaustPile.clear();
        this.cardSortOrder.clear();

        ArrayList<AbstractCard> sourceCards = getInitialDeck();
        for (AbstractCard c : sourceCards) {
            AbstractCard copy = c.makeStatEquivalentCopy();
            this.cardSortOrder.add(copy.uuid);
            this.drawPile.addToBottom(copy);
        }
        this.energy = this.energyBase;
    }

    // =====================================================================
    // 更新与渲染
    // =====================================================================

    @Override
    public void update() {
        // hbYOffset 保持 0，healthHb 由父类跟随 hb 的位置，这里不手动 move
        super.update();

        this.hb.update();
        this.healthHb.update();
        this.intentHb.update();

        refreshHandPositions();
        updateHandLogic();
        updateLimboLogic();
    }

    private void updateLimboLogic() {
        for (AbstractCard c : this.limbo.group) {
            c.update();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDead && !this.escaped) {
            // 1. 立绘（以底部中心为原点，支持 modelScale 缩放）
            if (this.img != null) {
                sb.setColor(this.tint.color);
                sb.draw(this.img,
                        this.drawX - (float) this.img.getWidth() * Settings.scale / 2.0F + this.animX,
                        this.drawY + this.animY,
                        (float) this.img.getWidth() / 2.0F, 0.0F,
                        (float) this.img.getWidth(), (float) this.img.getHeight(),
                        this.modelScale * Settings.scale, this.modelScale * Settings.scale,
                        0.0F, 0, 0, this.img.getWidth(), this.img.getHeight(),
                        this.flipHorizontal, this.flipVertical);
            }

            // 2. 选中高亮光晕
            if (this == AbstractDungeon.getCurrRoom().monsters.hoveredMonster && this.atlas == null
                    && this.img != null) {
                sb.setBlendFunction(770, 1);
                sb.setColor(new Color(1.0F, 1.0F, 1.0F, 0.1F));
                sb.draw(this.img,
                        this.drawX - (float) this.img.getWidth() * Settings.scale / 2.0F + this.animX,
                        this.drawY + this.animY,
                        (float) this.img.getWidth() / 2.0F, 0.0F,
                        (float) this.img.getWidth(), (float) this.img.getHeight(),
                        this.modelScale * Settings.scale, this.modelScale * Settings.scale,
                        0.0F, 0, 0, this.img.getWidth(), this.img.getHeight(),
                        this.flipHorizontal, this.flipVertical);
                sb.setBlendFunction(770, 771);
            }

            // 3. 原版意图的图标/伤害/粒子在这里一律不绘制（见下方 no-op 覆写说明）。

            // 4. 调试碰撞箱（如开启了调试显示）
            this.hb.render(sb);
            this.healthHb.render(sb);
        }

        // 5. 血条与怪物名字
        if (!AbstractDungeon.player.isDead) {
            this.renderHealth(sb);
            this.renderName(sb);
        }

        // 6. 自定义 UI（能量、手牌、Limbo、能力图标、卡牌意图队列）
        renderEnergyPanel(sb);
        renderHand(sb);
        renderLimbo(sb);
        renderCustomPowerIcons(sb);
        renderIntentQueue(sb);
    }

    @Override
    public void renderTip(SpriteBatch sb) {
        // 只显示能力 Tip（过滤掉 this.intentTip），定位用 hb.cX/hb.width，自动贴合碰撞箱
        this.tips.clear();
        for (AbstractPower p : this.powers) {
            if (p.region48 != null) {
                this.tips.add(new PowerTip(p.name, p.description, p.region48));
            } else {
                this.tips.add(new PowerTip(p.name, p.description, p.img));
            }
        }

        if (!this.tips.isEmpty()) {
            if (this.hb.cX + this.hb.width / 2.0F < TIP_X_THRESHOLD) {
                TipHelper.queuePowerTips(this.hb.cX + this.hb.width / 2.0F + TIP_OFFSET_R_X,
                        this.hb.cY + TipHelper.calculateAdditionalOffset(this.tips, this.hb.cY), this.tips);
            } else {
                TipHelper.queuePowerTips(this.hb.cX - this.hb.width / 2.0F + TIP_OFFSET_L_X,
                        this.hb.cY + TipHelper.calculateAdditionalOffset(this.tips, this.hb.cY), this.tips);
            }
        }
    }

    private void renderLimbo(SpriteBatch sb) {
        for (AbstractCard c : this.limbo.group) {
            c.render(sb);
        }
    }

    // =====================================================================
    // 【解耦】彻底屏蔽原版意图渲染（图标 / 伤害数字 / 粒子特效）
    //
    // 这四个方法在 AbstractMonster 里是 private，BaseMod 的 CustomMonster 用
    // @SpireOverride 重新暴露为 protected，因此这里可以覆写为 no-op。
    //
    // 关键点：无论调用方是谁（本类 render、基类 render、第三方 mod 的 patch、
    // 或任何外部渲染管线），原版意图图标/伤害数字/粒子一律不再绘制 ——
    // 这是对"某个未知来源在额外调用意图渲染"最稳的兜底。
    //
    // 不受影响的部分：
    //   - 意图的【状态与逻辑】：intent / intentDmg / intentHb / intentAlpha /
    //     createIntent / updateIntent / setMove 全部照跑（战斗决策模拟保留）。
    //   - 【卡牌意图队列】renderIntentQueue：是我们显式调用的独立方法，与这四个无关。
    //   - 【提示框】renderTip：用 hb.cX/hb.width 定位，与意图无关，位置不变。
    // =====================================================================

    @Override
    protected void renderIntent(final SpriteBatch sb) {
        // no-op：不绘制原版意图图标
    }

    @Override
    protected void renderDamageRange(final SpriteBatch sb) {
        // no-op：不绘制原版伤害数字
    }

    @Override
    protected void renderIntentVfxBehind(final SpriteBatch sb) {
        // no-op：不绘制原版意图粒子（图标前）
    }

    @Override
    protected void renderIntentVfxAfter(final SpriteBatch sb) {
        // no-op：不绘制原版意图粒子（图标后）
    }

    // 卡牌意图队列（独立于原版意图渲染，坐标完全由 cardIntentOffsetX/Y、cardIntentSpacing 参数化）。
    // 注意：这里【不】画原版 UNKNOWN 意图的金色"?"——它曾被误认为原版意图泄漏，
    // 实际来源就是本方法 hasUnknown 分支的字体"?"（已按需求移除）。
    // 未来若要在这里画出 cardsToPlay 的卡牌预览，直接用下面的 x/y/spacing 布局即可。
    private void renderIntentQueue(SpriteBatch sb) {
        if (this.intentResult == null || this.intentResult.cardsToPlay.isEmpty()) {
            return;
        }

        // 使用独立的偏移参数进行定位
        float x = this.drawX + this.animX + this.cardIntentOffsetX * Settings.scale;
        float y = this.drawY + this.animY + this.cardIntentOffsetY * Settings.scale;
        float spacing = this.cardIntentSpacing * Settings.scale;

        int totalCount = this.intentResult.cardsToPlay.size();
        float totalWidth = (totalCount - 1) * spacing;
        x -= totalWidth / 2.0F;

        Color originalSbColor = sb.getColor().cpy();
        sb.setColor(originalSbColor);
    }

    // =====================================================================
    // 牌堆操作
    // =====================================================================

    // 战斗开始前的初始化（从 Ally 上移的通用逻辑）
    @Override
    public void usePreBattleAction() {
        super.usePreBattleAction();
        // 初始化牌堆
        this.initBattleDeck();
        // 初始抽牌
        for (int i = 0; i < preDrawNumber; i++) {
            this.drawCard();
        }
    }

    public void atStartOfTurn() {
        // 恢复能量
        this.energy = this.energyBase;
        // 回合开始抽牌
        for (int i = 0; i < drawPerTurn; i++) {
            this.drawCard();
        }
        // 刷新意图 (抽完牌后立刻计算)
        this.applyPowers();
    }

    public void atEndOfTurn() {
        // 友军在这里打牌，但敌人不是。这里只保留"回合结束"的钩子；
        // 牌堆清理单独封装在 endTurnDeckLogic()，打牌逻辑由子类决定何时调用，
        // 以保证"清理"排在"打牌"动作之后。
    }

    // =====================================================================
    // 通用 AI 逻辑（保留给子类在合适时机调用：友军在 atEndOfTurn，敌人在 takeTurn）
    // =====================================================================

    /**
     * 核心出牌逻辑：遍历手牌，能量足够就打出，最后统一清理手牌。
     */
    protected void performTurnAI() {
        ArrayList<AbstractCard> cardsToPlay = new ArrayList<>(this.hand.group);

        for (AbstractCard c : cardsToPlay) {
            // 能量判断
            if (this.energy >= c.costForTurn) {
                // 【差异点】获取目标 (由子类实现)
                AbstractCreature target = getCardTarget(c);

                // 加入动作队列
                AbstractDungeon.actionManager.addToBottom(
                        new AllyPlayCardAction(this, c, target));
            }
        }

        // 无论如何，最后都要清理手牌
        AbstractDungeon.actionManager.addToBottom(new AbstractGameAction() {
            @Override
            public void update() {
                AbstractCardMonster.this.endTurnDeckLogic();
                this.isDone = true;
            }
        });
    }

    /**
     * 【抽象方法】子类必须决定这张牌打谁。
     * 友军：返回锁定的敌人；敌人：返回玩家。
     */
    protected abstract AbstractCreature getCardTarget(AbstractCard c);

    public void reshuffleDiscardPile() {
        if (this.discardPile.isEmpty())
            return;

        ArrayList<AbstractCard> cards = new ArrayList<>(this.discardPile.group);
        for (AbstractCard c : cards) {
            this.discardPile.removeCard(c);
            this.drawPile.addToBottom(c);
            c.unhover();
            c.untip();
            c.stopGlowing();
        }

        Collections.sort(this.drawPile.group, new Comparator<AbstractCard>() {
            @Override
            public int compare(AbstractCard c1, AbstractCard c2) {
                int index1 = cardSortOrder.indexOf(c1.uuid);
                int index2 = cardSortOrder.indexOf(c2.uuid);
                if (index1 == -1)
                    index1 = 999;
                if (index2 == -1)
                    index2 = 999;
                return Integer.compare(index1, index2);
            }
        });

        this.drawPile.refreshHandLayout();
    }

    public void drawCard() {
        if (this.drawPile.isEmpty()) {
            if (!this.discardPile.isEmpty()) {
                reshuffleDiscardPile();
            }
            if (this.drawPile.isEmpty())
                return;
        }

        AbstractCard c = this.drawPile.getTopCard();
        this.drawPile.removeCard(c);
        this.hand.addToTop(c);

        c.current_x = this.hb.cX;
        c.current_y = this.hb.cY;
        c.setAngle(0.0F);
        c.drawScale = 0.01f;
        c.targetDrawScale = cardScale; // 使用参量
        c.triggerWhenDrawn();
        c.unhover();
        c.untip();
        c.stopGlowing();
        refreshIntentCalculation();
        createIntent();
        // 不需要在这里调用 refreshHandPositions，因为 update 里每帧都会调
    }

    public void endTurnDeckLogic() {
        ArrayList<AbstractCard> cardsToDiscard = new ArrayList<>(this.hand.group);
        for (AbstractCard c : cardsToDiscard) {
            this.hand.removeCard(c);
            this.discardPile.addToTop(c);
            c.triggerOnManualDiscard();
            c.targetDrawScale = 0.01f;
            c.target_y = c.current_y - 50.0F * Settings.scale;
        }
    }

    // =====================================================================
    // UI 逻辑细节
    // =====================================================================

    private void updateHandLogic() {
        // 【核心】将整个手牌更新逻辑包裹在 Context 中
        MonsterCardContext.run(this, () -> {
            for (AbstractCard c : this.hand.group) {
                // 原版逻辑
                c.update();
                c.updateHoverLogic();

                // 【新增】强制让卡牌根据当前 Context 刷新数值
                // 因为 update 内部不一定会每帧调用 applyPowers
                c.applyPowers();
            }
        });
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        refreshIntentCalculation();
    }

    // 计算手牌扇形坐标：以 hb.cX/hb 顶部为基准，间距 = handSpacing × cardScale
    private void refreshHandPositions() {
        int count = this.hand.size();
        if (count == 0)
            return;

        float spacing = this.handSpacing * this.cardScale * Settings.scale;
        float startX = this.hb.cX + this.handOffsetX * Settings.scale;
        float startY = this.hb.cY + this.hb.height / 2.0F + this.handOffsetY * Settings.scale;

        for (int i = 0; i < count; i++) {
            AbstractCard c = this.hand.group.get(i);

            float offset = (i - (count - 1) / 2.0F) * spacing;

            // 持续更新 target_x/y，这样当 hb.cX 变动时，卡牌会跟着跑
            c.target_x = startX + offset;
            c.target_y = startY;
            c.targetAngle = -offset * 0.1f;

            if (c.hb.hovered) {
                c.targetDrawScale = this.hoverScale;
                c.target_y = startY + this.handHoverLift * Settings.scale;
            } else {
                c.targetDrawScale = this.cardScale;
            }
        }
    }

    public void refreshIntentCalculation() {
        this.intentResult = MonsterIntentSimulator.simulate(this);

        if (this.intentResult.hasUnknown) {
            // this.setMove((byte) 0, Intent.UNKNOWN);
            this.setMove((byte) 0, Intent.NONE);
            this.createIntent();
        } else if (this.intentResult.totalDamage > 0) {
            // 1. 设置意图 (传入模拟好的总伤)
            this.isReadingSimulation = true; // 打开锁

            // setMove 把总伤存为 baseDamage，createIntent 会调用 calculateDamage(总伤)
            // 我们的 Patch 会检测到锁开了，直接令 intentDmg = 总伤，跳过力量加成
            this.setMove((byte) 1, Intent.NONE);
            this.createIntent();

            this.isReadingSimulation = false; // 关上锁，以免影响其他逻辑
        } else if (this.intentResult.totalBlock > 0) {
            // this.setMove((byte) 2, Intent.DEFEND);
            this.setMove((byte) 2, Intent.NONE);
            this.createIntent();
        } else if (!this.intentResult.cardsToPlay.isEmpty()) {
            // this.setMove((byte) 3, Intent.BUFF);
            this.setMove((byte) 3, Intent.NONE);
            this.createIntent();
        } else {
            // this.setMove((byte) 4, Intent.STUN);
            this.setMove((byte) 4, Intent.NONE);
            this.createIntent();
        }
    }

    private void renderHand(SpriteBatch sb) {
        for (AbstractCard c : this.hand.group) {
            if (!c.hb.hovered)
                c.render(sb);
        }
        for (AbstractCard c : this.hand.group) {
            if (c.hb.hovered)
                c.render(sb); // 悬停放大的牌最后画
        }
    }

    // 能量面板：能量球 + "能量/上限"数字。
    // 球大小 = energyScale，数字字体 = energyTextScale，位置 = hb 左下角 + energyOffsetX/Y。
    private void renderEnergyPanel(SpriteBatch sb) {
        TextureAtlas.AtlasRegion orbImg = ImageMaster.CARD_RED_ORB;

        // 坐标：以 hb 左下角为基准 + energyOffsetX/Y 微调
        float x = this.hb.x + this.energyOffsetX * Settings.scale;
        float y = this.hb.y + this.energyOffsetY * Settings.scale;

        float scale = this.energyScale * Settings.scale;

        sb.setColor(Color.WHITE);

        sb.draw(orbImg,
                x - (orbImg.packedWidth / 2f), y - (orbImg.packedHeight / 2f),
                orbImg.packedWidth / 2f, orbImg.packedHeight / 2f,
                orbImg.packedWidth, orbImg.packedHeight,
                scale, scale, 0f);

        String energyText = this.energy + "/" + this.energyBase;
        // 数字与球同心，字体大小由 energyTextScale 控制
        FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L, energyText, x, y, energyColor,
                this.energyTextScale);
    }

    // 能力图标 + 数字。整体位置 = hb 左下角 + powerOffsetX/Y（+ 父类 hbYOffset）。
    // 图标大小 = powerIconScale，数字字体 = powerTextScale。
    private void renderCustomPowerIcons(SpriteBatch sb) {
        if (this.powers.isEmpty())
            return;

        // 基准坐标：原版 x = hb.x；y = hb.y + hbYOffset，再叠加 powerOffsetX/Y 微调
        float x = this.hb.cX - this.hb.width / 2.0F + (this.powerOffsetX * Settings.scale);
        float hbYOffset = ReflectionHacks.getPrivate(this, AbstractCreature.class, "hbYOffset");
        float y = this.hb.cY - this.hb.height / 2.0F + hbYOffset + (this.powerOffsetY * Settings.scale);

        // 间距：图标基准宽度 × 缩放
        float spacing = this.powerIconWidth * Settings.scale * this.powerIconScale;

        // =================================================================
        // 循环 1: 渲染图标
        // =================================================================
        float offset = this.powerIconStartOffset * Settings.scale;

        for (AbstractPower p : this.powers) {
            if (p.region48 != null) {
                sb.setColor(Color.WHITE);

                float drawX = x + offset;
                float drawY = y - (this.powerIconWidth * Settings.scale * this.powerIconScale);

                sb.draw(p.region48,
                        drawX - p.region48.packedWidth / 2.0F,
                        drawY - p.region48.packedHeight / 2.0F,
                        p.region48.packedWidth / 2.0F, p.region48.packedHeight / 2.0F,
                        p.region48.packedWidth, p.region48.packedHeight,
                        this.powerIconScale * Settings.scale, this.powerIconScale * Settings.scale,
                        0.0F);

                // Tip 悬停检测 (坐标变了，必须手动补)
                float hbSize = this.powerIconWidth * this.powerIconScale * Settings.scale;
                if (com.megacrit.cardcrawl.helpers.input.InputHelper.mX >= drawX - hbSize / 2 &&
                        com.megacrit.cardcrawl.helpers.input.InputHelper.mX <= drawX + hbSize / 2 &&
                        com.megacrit.cardcrawl.helpers.input.InputHelper.mY >= drawY - hbSize / 2 &&
                        com.megacrit.cardcrawl.helpers.input.InputHelper.mY <= drawY + hbSize / 2) {

                    ArrayList<PowerTip> tips = new ArrayList<>();
                    tips.add(new PowerTip(p.name, p.description));
                    TipHelper.queuePowerTips(
                            drawX + sb.getTransformMatrix().val[com.badlogic.gdx.math.Matrix4.M03],
                            drawY + sb.getTransformMatrix().val[com.badlogic.gdx.math.Matrix4.M13],
                            tips);
                }
            }
            offset += spacing;
        }

        // =================================================================
        // 循环 2: 渲染数字
        // =================================================================
        offset = 0.0F * Settings.scale;

        for (AbstractPower p : this.powers) {
            if (p.amount != 0) {
                sb.setColor(Color.WHITE);
                Color c = Color.WHITE.cpy();
                if (p.amount > 0 && p.type == AbstractPower.PowerType.BUFF)
                    c = Color.GREEN.cpy();
                else if (p.amount < 0 && p.type == AbstractPower.PowerType.DEBUFF)
                    c = Color.RED.cpy();

                float drawX = x + offset + (this.powerNumberXOffset * Settings.scale * this.powerIconScale);
                float drawY = y - (this.powerNumberYOffset * Settings.scale * this.powerIconScale);

                FontHelper.renderFontRightTopAligned(
                        sb,
                        FontHelper.powerAmountFont,
                        Integer.toString(p.amount),
                        drawX,
                        drawY,
                        this.powerTextScale * Settings.scale,
                        c);
            }
            offset += spacing;
        }
    }
}
