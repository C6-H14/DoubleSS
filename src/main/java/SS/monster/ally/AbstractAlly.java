package SS.monster.ally;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.common.AllyPlayCardAction;
import SS.action.common.AllyTurnPlannerAction;
import SS.helper.AllyTriggerHelper;
import SS.monster.AbstractCardMonster;

public abstract class AbstractAlly extends AbstractCardMonster {

    public enum TauntType {
        NONE, SOLID, OVERFLOW
    }

    public TauntType tauntType = TauntType.NONE;
    public AbstractMonster lockedTarget = null;
    protected boolean isAOE = false;

    // =================================================================
    // 构造与初始化
    // =================================================================
    // 构造函数 1
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            float offsetX, float offsetY, float hb_x, float hb_y,
            float healthBarOffsetY // 【新增参数】接收高度
    ) {
        // 传给父类，最后那个参数就是高度
        super(name, id, maxHealth, hb_x, hb_y, 150F, 150F, imgUrl, offsetX, offsetY, 3, healthBarOffsetY);
        this.tauntType = type;
        commonInit(offsetX, offsetY);
    }

    // 构造函数 2 (自定义能量)
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            float offsetX, float offsetY, float hb_x, float hb_y,
            int energy,
            float healthBarOffsetY // 【新增参数】接收高度
    ) {
        // 传给父类
        super(name, id, maxHealth, hb_x, hb_y, 150F, 150F, imgUrl, offsetX, offsetY, energy, healthBarOffsetY);
        this.tauntType = type;
        commonInit(offsetX, offsetY);
    }

    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            float offsetX, float offsetY, float hb_x, float hb_y, float hb_w, float hb_h,
            int energy,
            float healthBarOffsetY // 【新增参数】接收高度
    ) {
        // 传给父类
        super(name, id, maxHealth, hb_x, hb_y, hb_w, hb_h, imgUrl, offsetX, offsetY, energy, healthBarOffsetY);
        this.tauntType = type;
        commonInit(offsetX, offsetY);
    }

    public void init() {
        this.setMove((byte) 0, Intent.NONE);
        this.createIntent();
        // 初始刷新
        this.refreshIntentCalculation();
    }

    private void commonInit(float x, float y) {
        // 1. 设定立绘（图片）的渲染位置
        // 这是"脚底板"的位置，保持不变，这样立绘就会稳稳站在地上
        this.drawX = AbstractDungeon.player.drawX + x;
        this.drawY = AbstractDungeon.player.drawY + y;

        // 2. 【核心黑科技：灵肉分离】
        // 我们不使用 hbYOffset 来调整高度了，因为特效不认它。
        // 我们直接把 Hitbox 本身往上抬！

        // 计算目标高度偏移 (由你的参数 healthBarOffsetY 决定)
        float verticalShift = this.healthBarOffsetY * Settings.scale;

        // 4. 刷新组件
        this.refreshHitboxLocation();

        // 移动 Hitbox
        // X: 保持在 drawX
        // Y: 地面(drawY) + 半高(height/2) + 【上移量(verticalShift)】
        this.hb.move(this.drawX, this.drawY + this.hb.height / 2.0F + verticalShift);

        // 5. 意图位置
        // 因为 hb 已经抬高了，意图只需要相对 hb 微调即可
        this.intentHb.move(this.hb.cX, this.hb.cY + this.hb.height / 2.0F + 50.0F * Settings.scale);

        this.showHealthBar();
        this.healthBarUpdatedEvent();
        this.refreshIntentCalculation();
    }

    public void lockTarget(AbstractMonster m) {
        this.lockedTarget = m;
    }

    // =================================================================
    // 渲染与更新 (UI)
    // =================================================================

    @Override
    public void update() {
        super.update();
        // this.hb.update();
        // this.healthHb.update();
        // this.intentHb.update();
        // this.hbAlpha = 1.0F;
    }

    // =================================================================
    // 战斗逻辑 (牌堆与回合)
    // =================================================================

    // 1. 定义行动时机
    @Override
    public void atEndOfTurn() {
        // 友军在玩家回合结束时行动
        addToBot(new AllyTurnPlannerAction(this));

        super.atEndOfTurn();
    }

    // 2. 定义目标逻辑
    @Override
    protected AbstractCreature getCardTarget(AbstractCard c) {
        // 如果是攻击牌 -> 打锁定的敌人
        if (c.target == AbstractCard.CardTarget.ENEMY || c.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
            return this.getTarget();
        }
        // 如果是 AOE -> null
        else if (c.target == AbstractCard.CardTarget.ALL_ENEMY) {
            return null;
        }
        // 如果是 Buff/防御 -> 打自己
        else {
            return this;
        }
    }

    /**
     * 默认的贪婪 AI：
     * 遍历手牌，只要能量足够，就打出。
     * 攻击牌打锁定目标，Buff牌打自己。
     */
    protected void defaultAI() {
        // 使用副本遍历，因为打牌会修改 hand
        ArrayList<AbstractCard> cardsToPlay = new ArrayList<>(this.hand.group);

        for (AbstractCard c : cardsToPlay) {
            // 能量判断
            if (this.energy >= c.costForTurn) {

                // 自动判断目标
                AbstractCreature target = null;

                // 1. 攻击类：打锁定的敌人
                if (c.target == AbstractCard.CardTarget.ENEMY || c.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                    target = this.getTarget();
                }
                // 2. AOE类：无需目标 (传 null)
                else if (c.target == AbstractCard.CardTarget.ALL_ENEMY || c.target == AbstractCard.CardTarget.ALL) {
                    target = null;
                }
                // 3. 增益/技能类：打自己
                else {
                    target = this;
                }

                // 加入动作队列
                AbstractDungeon.actionManager.addToBottom(
                        new AllyPlayCardAction(this, c, target));
            }
        }
    }

    // =================================================================
    // 意图与计算 (接入 Phase 3 模拟器)
    // =================================================================

    @Override
    public void applyPowers() {
        // 1. 先调用原版逻辑(虽然大部分没用，但保持兼容)
        super.applyPowers();

        // 2. 确保有目标 (如果不加这个，simulate 里的 calculateCardDamage 会算错)
        if (this.lockedTarget == null || this.lockedTarget.isDeadOrEscaped()) {
            this.lockedTarget = this.getRandomTarget();
        }

        // 3. 【核心】触发模拟计算
        this.refreshIntentCalculation();
    }

    // 必须实现，但实际上逻辑被 refreshIntentCalculation 接管了
    @Override
    public void getMove(int num) {
        this.refreshIntentCalculation();
    }

    // 你原来的 updateIntent 在这里已经不再需要手动编写了，因为全自动了

    // =================================================================
    // 核心逻辑：目标管理 (补全)
    // =================================================================

    // 获取当前攻击目标 (带锁定逻辑)
    public AbstractMonster getTarget() {
        if (lockedTarget == null || lockedTarget.isDeadOrEscaped()) {
            lockedTarget = this.getRandomTarget();
        }
        return lockedTarget;
    }

    // 随机索敌
    public AbstractMonster getRandomTarget() {
        ArrayList<AbstractMonster> validTargets = new ArrayList<>();
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (!m.isDeadOrEscaped() && !m.isDying && !(m instanceof AbstractAlly)) {
                validTargets.add(m);
            }
        }
        if (validTargets.isEmpty())
            return null;
        return validTargets.get(AbstractDungeon.monsterRng.random(validTargets.size() - 1));
    }

    // =================================================================
    // 受击与攻击辅助 (补全)
    // =================================================================

    // 必须保留：防误伤逻辑
    @Override
    public void damage(DamageInfo info) {
        if (info.owner != null && info.owner != AbstractDungeon.player) {
            super.damage(info);
        }
    }

    @Override
    public void die(boolean triggerRelics) {
        super.die(triggerRelics);
    }

    // 攻击辅助方法 (用于非卡牌攻击，或 ActionQueue 中的回调)
    public void attack(AbstractMonster target, int damage, AbstractGameAction.AttackEffect effect) {
        if (target == null)
            return;

        DamageInfo info = new DamageInfo(this, damage, DamageInfo.DamageType.NORMAL);
        info.applyPowers(this, target); // 计算力量等

        AbstractDungeon.actionManager.addToBottom(new DamageAction(target, info, effect));

        // 触发你的 Helper
        AbstractDungeon.actionManager.addToBottom(new AbstractGameAction() {
            @Override
            public void update() {
                AllyTriggerHelper.triggerOnAllyAttack(AbstractAlly.this, target, info.output);
                this.isDone = true;
            }
        });
    }
}