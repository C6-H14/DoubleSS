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

import SS.action.common.AllyPlayCardAction;
import SS.helper.MonsterCardContext;
import SS.helper.MonsterIntentSimulator;
import basemod.ReflectionHacks;
import basemod.abstracts.CustomMonster;

public abstract class AbstractCardMonster extends CustomMonster {

    // 牌堆数据
    public CardGroup masterDeck;
    public CardGroup drawPile;
    public CardGroup hand;
    public CardGroup discardPile;
    public CardGroup exhaustPile;
    public CardGroup limbo; // 【新增】悬空堆，用于播放打牌动画
    private ArrayList<UUID> cardSortOrder = new ArrayList<>();

    // 能量数据
    public int energyBase;
    public int energy = 0;
    private Color energyColor = Color.WHITE.cpy();

    // =================================================================
    // 【新增】UI 调整参数 (可在子类构造函数中修改)
    // =================================================================

    // 【新增】血条位置微调变量
    public float healthBarOffsetX = 0.0F;
    public float healthBarOffsetY = -20.0F; // 默认稍微往下一点，你可以改

    // 手牌大小缩放
    public float cardScale = 0.3f; // 原来0.4太大了，改小
    public float hoverScale = 0.6f;

    // 手牌相对于头顶的位置偏移
    public float handOffsetX = 0.0F;
    public float handOffsetY = 150.0F;

    // 能量球相对于 Hitbox 左下角(hb.x, hb.y) 的偏移
    // 你可以在 SoulAlly 里修改这两个值来解决错位
    public float energyOffsetX = 0.0F;
    public float energyOffsetY = 0.0F;

    // 能量球的缩放
    public float energyScale = 0.8F;// 自定义缩放变量
    public float powerIconScale = 0.8F; // 图标缩小
    public float powerTextScale = 1.0F; // 字体缩小

    public float powerOffsetX = 0.0F; // 【变量1】整体左右微调
    public float powerOffsetY = -5.0F; // 【变量2】整体上下微调
    // 意图模拟结果
    protected MonsterIntentSimulator.SimulationResult intentResult;
    // 意图图标缩放
    private float intentIconScale = 0.5f;
    public boolean isReadingSimulation = false;

    // =================================================================

    // 构造函数 1
    public AbstractCardMonster(
            String name, String id, int maxHealth,
            float hb_x, float hb_y, float hb_w, float hb_h,
            String imgUrl,
            float offsetX, float offsetY,
            int energyBase,
            float healthBarOffsetY // 【新增参数】
    ) {
        super(name, id, maxHealth, hb_x, hb_y, hb_w, hb_h, imgUrl, offsetX, offsetY);
        this.energyBase = energyBase;
        this.healthBarOffsetY = healthBarOffsetY; // 保存下来
        initializeDecks();
    }

    // 构造函数 2
    public AbstractCardMonster(
            String name, String id, int maxHealth,
            float hb_x, float hb_y, float hb_w, float hb_h,
            String imgUrl,
            int energyBase,
            float healthBarOffsetY // 【新增参数】
    ) {
        super(name, id, maxHealth, hb_x, hb_y, hb_w, hb_h, imgUrl);
        this.energyBase = energyBase;
        this.healthBarOffsetY = healthBarOffsetY; // 保存下来
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

    // =================================================================
    // 更新与渲染
    // =================================================================

    @Override
    public void update() {
        // 不需要再设置 hbYOffset 了，保持为 0 即可
        // 也不要手动 move healthHb 了，父类会跟随 hb 的位置
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
        ArrayList<com.megacrit.cardcrawl.powers.AbstractPower> backupPowers = new ArrayList<>(this.powers);
        this.powers.clear();
        super.render(sb);
        this.powers.addAll(backupPowers);
        renderEnergyPanel(sb);
        renderHand(sb);
        renderLimbo(sb);
        renderCustomPowerIcons(sb);
        renderIntentQueue(sb);
    }

    private void renderLimbo(SpriteBatch sb) {
        for (AbstractCard c : this.limbo.group) {
            c.render(sb);
        }
    }

    private void renderIntentQueue(SpriteBatch sb) {
        if (this.intentResult == null || this.intentResult.cardsToPlay.isEmpty()) {
            return;
        }

        float x = this.intentHb.cX;
        float y = this.intentHb.cY + 50.0F * Settings.scale;
        float spacing = 30.0F * Settings.scale;

        int totalCount = this.intentResult.cardsToPlay.size() + (this.intentResult.hasUnknown ? 1 : 0);
        float totalWidth = (totalCount - 1) * spacing;
        x -= totalWidth / 2.0F;

        // 备份原来的 SB 颜色
        Color originalSbColor = sb.getColor().cpy();

        // 恢复 SB 颜色，否则会影响后续渲染
        sb.setColor(originalSbColor);

        if (this.intentResult.hasUnknown) {
            FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L, "?", x, y, Color.GOLD, 0.6f);
        }
    }

    // =================================================================
    // 牌堆操作
    // =================================================================

    // =================================================================
    // 1. 通用初始化逻辑 (从 Ally 上移)
    // =================================================================
    @Override
    public void usePreBattleAction() {
        super.usePreBattleAction();
        // 初始化牌堆
        this.initBattleDeck();
        // 初始抽牌 (默认3张，可提取为变量)
        for (int i = 0; i < 3; i++) {
            this.drawCard();
        }
    }

    public void atStartOfTurn() {
        // 恢复能量
        this.energy = this.energyBase;
        // 回合开始抽牌 (默认2张)
        for (int i = 0; i < 2; i++) {
            this.drawCard();
        }
        // 刷新意图 (抽完牌后立刻计算)
        this.applyPowers();
    }

    public void atEndOfTurn() {
        // 注意：友军在这里打牌，但敌人不是。
        // 所以这里只处理牌堆清理，打牌逻辑由子类决定何时调用。
        // 我们把"清理牌堆"单独封装，不要在这里直接调用，
        // 因为如果友军在 atEndOfTurn 打牌，清理动作必须排在打牌动作之后。
    }

    // =================================================================
    // 2. 通用 AI 逻辑 (从 Ally 上移)
    // =================================================================

    /**
     * 核心出牌逻辑。
     * 子类需要在合适的时机调用此方法（友军在 atEndOfTurn，敌人在 takeTurn）。
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
     * 【抽象方法】子类必须决定这张牌打谁
     * 友军：返回锁定的敌人
     * 敌人：返回玩家
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
        c.targetDrawScale = cardScale; // 使用变量
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

    // =================================================================
    // UI 逻辑细节
    // =================================================================

    private void updateHandLogic() {
        // 【核心修改】将整个手牌更新逻辑包裹在 Context 中
        MonsterCardContext.run(this, () -> {
            for (AbstractCard c : this.hand.group) {
                // 原版逻辑：
                c.update();
                c.updateHoverLogic();

                // 【新增】强制让卡牌根据当前 Context 刷新数值
                // 因为 update 内部不一定会每帧调用 applyPowers
                c.applyPowers();

                // 【可选】如果卡牌是单体的，且友军锁定了目标，可以进一步计算针对目标的伤害
                // if (this instanceof AbstractAlly && ((AbstractAlly)this).getTarget() != null)
                // {
                // c.calculateCardDamage(((AbstractAlly)this).getTarget());
                // }
            }
        });
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        refreshIntentCalculation();
    }

    // 【核心修复】计算卡牌坐标
    private void refreshHandPositions() {
        int count = this.hand.size();
        if (count == 0)
            return;

        // 根据卡牌大小动态调整间距
        float spacing = 110.0F * cardScale * Settings.scale;
        float startX = this.hb.cX + handOffsetX * Settings.scale;
        float startY = this.hb.cY + this.hb.height / 2.0F + handOffsetY * Settings.scale;

        for (int i = 0; i < count; i++) {
            AbstractCard c = this.hand.group.get(i);

            float offset = (i - (count - 1) / 2.0F) * spacing;

            // 持续更新 target_x/y，这样当 hb.cX 变动时，卡牌会跟着跑
            c.target_x = startX + offset;
            c.target_y = startY;
            c.targetAngle = -offset * 0.1f;

            if (c.hb.hovered) {
                c.targetDrawScale = hoverScale; // 使用变量
                c.target_y = startY + 50.0F * Settings.scale;
            } else {
                c.targetDrawScale = cardScale; // 使用变量
            }
        }
    }

    public void refreshIntentCalculation() {
        this.intentResult = MonsterIntentSimulator.simulate(this);

        if (this.intentResult.hasUnknown) {
            this.setMove((byte) 0, Intent.UNKNOWN);
            this.createIntent();
        } else if (this.intentResult.totalDamage > 0) {
            // 1. 设置意图 (传入模拟好的总伤 8)
            this.isReadingSimulation = true; // 打开锁

            // 传入模拟器算好的 8 点
            // setMove 会把 8 存为 baseDamage
            // createIntent 会调用 calculateDamage(8)
            // 我们的 Patch 会检测到锁开了，直接令 intentDmg = 8，跳过力量加成
            this.setMove((byte) 1, Intent.ATTACK, this.intentResult.totalDamage);
            this.createIntent();

            this.isReadingSimulation = false; // 关上锁，以免影响其他逻辑

        } else if (this.intentResult.totalBlock > 0) {
            this.setMove((byte) 2, Intent.DEFEND);
            this.createIntent();
        } else if (!this.intentResult.cardsToPlay.isEmpty()) {
            this.setMove((byte) 3, Intent.BUFF);
            this.createIntent();
        } else {
            this.setMove((byte) 4, Intent.STUN);
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
                c.render(sb); // 渲染放大的牌
        }
    }

    private void renderEnergyPanel(SpriteBatch sb) {
        // 【修复】改为 AtlasRegion 类型
        TextureAtlas.AtlasRegion orbImg = ImageMaster.CARD_RED_ORB;

        // 计算坐标：引入 energyOffsetX/Y 变量供微调
        float x = this.hb.x + this.energyOffsetX * Settings.scale;
        float y = this.hb.y + this.energyOffsetY * Settings.scale;

        float scale = this.energyScale * Settings.scale;

        sb.setColor(Color.WHITE);

        // 【修复】使用 draw 的 AtlasRegion 重载方法
        sb.draw(orbImg,
                x - (orbImg.packedWidth / 2f), y - (orbImg.packedHeight / 2f), // 坐标 (减去一半宽高以居中)
                orbImg.packedWidth / 2f, orbImg.packedHeight / 2f, // 旋转中心
                orbImg.packedWidth, orbImg.packedHeight, // 宽高
                scale, scale, 0f); // 缩放与旋转

        String energyText = this.energy + "/" + this.energyBase;
        // 字体位置微调，确保居中于图标
        FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L, energyText, x, y, energyColor, 0.7f);
    }

    private void renderCustomPowerIcons(SpriteBatch sb) {
        if (this.powers.isEmpty())
            return;

        // 1. 基础坐标计算 (完全照搬原版逻辑 + 你的微调变量)
        // 原版: x = this.hb.cX - this.hb.width / 2.0F; (即 hb.x)
        // 原版: y = this.hb.cY - this.hb.height / 2.0F + this.hbYOffset; (即 hb.y +
        // offset)
        float x = this.hb.cX - this.hb.width / 2.0F + (this.powerOffsetX * Settings.scale);
        float hbYOffset = ReflectionHacks.getPrivate(this, AbstractCreature.class, "hbYOffset");
        float y = this.hb.cY - this.hb.height / 2.0F + hbYOffset + (this.powerOffsetY * Settings.scale);

        // 间距：48是原版图标宽度，乘以缩放比例
        float spacing = 48.0F * Settings.scale * this.powerIconScale;

        // =================================================================
        // 循环 1: 渲染图标 (起始 offset = 10.0F)
        // =================================================================
        float offset = 10.0F * Settings.scale;

        for (com.megacrit.cardcrawl.powers.AbstractPower p : this.powers) {
            if (p.region48 != null) {
                sb.setColor(Color.WHITE);

                // 原版 Desktop: y - 48.0F
                // 我们修改为: y - (48.0F * scale * iconScale) 以适应缩小
                float drawX = x + offset;
                float drawY = y - (48.0F * Settings.scale * this.powerIconScale);

                sb.draw(p.region48,
                        drawX - p.region48.packedWidth / 2.0F,
                        drawY - p.region48.packedHeight / 2.0F,
                        p.region48.packedWidth / 2.0F, p.region48.packedHeight / 2.0F,
                        p.region48.packedWidth, p.region48.packedHeight,
                        this.powerIconScale * Settings.scale, this.powerIconScale * Settings.scale,
                        0.0F);

                // Tip 悬停检测 (必须手动补，因为坐标变了)
                float hbSize = 48.0F * this.powerIconScale * Settings.scale;
                if (com.megacrit.cardcrawl.helpers.input.InputHelper.mX >= drawX - hbSize / 2 &&
                        com.megacrit.cardcrawl.helpers.input.InputHelper.mX <= drawX + hbSize / 2 &&
                        com.megacrit.cardcrawl.helpers.input.InputHelper.mY >= drawY - hbSize / 2 &&
                        com.megacrit.cardcrawl.helpers.input.InputHelper.mY <= drawY + hbSize / 2) {

                    java.util.ArrayList<com.megacrit.cardcrawl.helpers.PowerTip> tips = new java.util.ArrayList<>();
                    tips.add(new com.megacrit.cardcrawl.helpers.PowerTip(p.name, p.description));
                    com.megacrit.cardcrawl.helpers.TipHelper.queuePowerTips(
                            drawX + sb.getTransformMatrix().val[com.badlogic.gdx.math.Matrix4.M03],
                            drawY + sb.getTransformMatrix().val[com.badlogic.gdx.math.Matrix4.M13],
                            tips);
                }
            }
            offset += spacing;
        }

        // =================================================================
        // 循环 2: 渲染数字 (起始 offset = 0.0F, 照搬原版)
        // =================================================================
        offset = 0.0F * Settings.scale;

        for (com.megacrit.cardcrawl.powers.AbstractPower p : this.powers) {
            if (p.amount != 0) {
                sb.setColor(Color.WHITE);
                Color c = Color.WHITE.cpy();
                if (p.amount > 0 && p.type == com.megacrit.cardcrawl.powers.AbstractPower.PowerType.BUFF)
                    c = Color.GREEN.cpy();
                else if (p.amount < 0 && p.type == com.megacrit.cardcrawl.powers.AbstractPower.PowerType.DEBUFF)
                    c = Color.RED.cpy();

                // 原版 Desktop X: x + offset + 32.0F
                // 原版 Desktop Y: y - 66.0F
                float drawX = x + offset + (32.0F * Settings.scale * this.powerIconScale);
                float drawY = y - (66.0F * Settings.scale * this.powerIconScale);

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