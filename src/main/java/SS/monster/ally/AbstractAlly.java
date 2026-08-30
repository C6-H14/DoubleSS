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

import SS.action.monster.AllyPlayCardAction;
import SS.action.monster.AllyTurnPlannerAction;
import SS.helper.AllyTriggerHelper;
import SS.monster.AbstractCardMonster;

/**
 * 友军基类：在 {@link AbstractCardMonster} 之上补上"友军"特有的行为 ——
 * 嘲讽类型、锁定目标、玩家回合结束出牌、防误伤、贪婪 AI 等。
 */
public abstract class AbstractAlly extends AbstractCardMonster {

    // 嘲讽/格挡类型
    public enum TauntType {
        NONE, SOLID, OVERFLOW
    }

    public TauntType tauntType = TauntType.NONE;
    public AbstractMonster lockedTarget = null; // 当前锁定的攻击目标
    protected boolean isAOE = false;

    // 站位槽位（0..4，见 AllyPositionHelper.SLOT_COUNT）；所有重定位只走 relocateToSlot。
    public int slotIndex = 0;

    // 手牌"头部上方"保底的头部余量（1920 基准设计像素）：
    // 手牌基线不得低于 立绘头顶 + 该余量，防止友军立绘增高时手牌压到头部/侵入玩家选牌区
    private static final float HAND_ABOVE_HEAD_MARGIN = 40.0F;

    // =====================================================================
    // 构造与初始化
    // 构造函数统一走父类 (AbstractCardMonster)。站位只由 slotIndex 决定
    // （AllyPositionHelper：player.drawX / floorY 锚定 + 偏移×scale），
    // 构造末尾 relocateToSlot 立即覆盖父类按 offsetX/offsetY 算出的初始坐标
    // （传 0F/0F，父类初始位为 0.75W/floorY，不会被渲染到）。
    // 悬浮高度由 hb_y / hb_h 决定：父类每帧 refreshHitboxLocation 会据此重算 hb，
    // 构造期手动抬 hb 的写法会被逐帧覆盖（死代码），故不写。
    // =====================================================================

    // 构造函数 1：固定碰撞箱 150×150、能量 3
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            int slotIndex, float hb_x, float hb_y) {
        super(name, id, maxHealth, hb_x, hb_y, 150F, 150F, imgUrl, 0F, 0F, 3, -20F);
        this.tauntType = type;
        commonInit(slotIndex);
    }

    // 构造函数 2：固定碰撞箱 150×150，自定义能量
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            int slotIndex, float hb_x, float hb_y,
            int energy) {
        super(name, id, maxHealth, hb_x, hb_y, 150F, 150F, imgUrl, 0F, 0F, energy, -20F);
        this.tauntType = type;
        commonInit(slotIndex);
    }

    // 构造函数 3：完全自定义碰撞箱尺寸与能量（SoulAlly 使用这个）
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            int slotIndex, float hb_x, float hb_y, float hb_w, float hb_h,
            int energy) {
        super(name, id, maxHealth, hb_x, hb_y, hb_w, hb_h, imgUrl, 0F, 0F, energy, -20F);
        this.tauntType = type;
        commonInit(slotIndex);
    }

    public void init() {
        this.setMove((byte) 0, Intent.NONE);
        this.createIntent();
        // 初始刷新
        this.refreshIntentCalculation();
    }

    private void commonInit(int slotIndex) {
        this.relocateToSlot(slotIndex);
        this.showHealthBar();
        this.healthBarUpdatedEvent();
        this.refreshIntentCalculation();
    }

    /**
     * 把本友军移动到指定槽位。所有重定位（召唤、阵法变换、回归）只走这里。
     * X/Y 锚点与偏移换算全部见 {@link AllyPositionHelper}；
     * drawX/drawY 落下后 refreshHitboxLocation 随之重算 hb
     * （基类每帧也会再算一遍，所以这里只需设 drawX/drawY 并同步一次，保证当帧 hb 已就位）。
     */
    public void relocateToSlot(int slotIndex) {
        this.slotIndex = AllyPositionHelper.clampSlot(slotIndex);
        this.drawX = AllyPositionHelper.slotDrawX(this.slotIndex);
        this.drawY = AllyPositionHelper.slotDrawY(this.slotIndex);
        this.refreshHitboxLocation();
        // 持久屏蔽原版意图碰撞箱：每帧 refreshIntentHbLocation 都会按 intentOffsetX 重算，
        // 所以必须把偏移量本身甩到无穷远（旧写法构造期 hb.move(-9999) 只活一帧，已被重置）。
        this.intentOffsetX = -99999.0F;
        this.refreshIntentHbLocation();
    }

    public void lockTarget(AbstractMonster m) {
        this.lockedTarget = m;
    }

    // =====================================================================
    // 渲染与更新 (UI)
    // =====================================================================

    @Override
    public void update() {
        super.update();

        // 【手牌头部上方保底】基类 refreshHandPositions 在 super.update() 里已按 hb 算好 target_y，
        // 这里只做"抬不压"钳制：手牌基线不得低于 立绘头顶 + 余量。
        // 已经更高的手牌（含悬停抬升）不受影响；SoulAlly 现有参数下钳制不触发，纯为立绘增高兜底。
        if (this.hand.size() > 0 && this.img != null) {
            float headTop = this.drawY + this.img.getHeight() * this.modelScale * Settings.scale;
            float minHandY = headTop + HAND_ABOVE_HEAD_MARGIN * Settings.scale;
            for (AbstractCard c : this.hand.group) {
                if (c.target_y < minHandY)
                    c.target_y = minHandY;
            }
        }
    }

    // =====================================================================
    // 战斗逻辑 (牌堆与回合)
    // =====================================================================

    // 友军在玩家回合结束时行动
    @Override
    public void atEndOfTurn() {
        addToBot(new AllyTurnPlannerAction(this));
        super.atEndOfTurn();
    }

    // 出牌目标路由：攻击类→锁定敌人，AOE→null，其余（Buff/防御）→自己
    @Override
    protected AbstractCreature getCardTarget(AbstractCard c) {
        if (c.target == AbstractCard.CardTarget.ENEMY || c.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
            return this.getTarget();
        } else if (c.target == AbstractCard.CardTarget.ALL_ENEMY) {
            return null;
        } else {
            return this;
        }
    }

    /**
     * 默认的贪婪 AI：遍历手牌，能量足够就打出。
     * 攻击牌打锁定目标，AOE 无需目标，Buff/技能打自己。
     */
    protected void defaultAI() {
        // 用副本遍历，因为打牌会修改 hand
        ArrayList<AbstractCard> cardsToPlay = new ArrayList<>(this.hand.group);

        for (AbstractCard c : cardsToPlay) {
            if (this.energy >= c.costForTurn) {
                AbstractCreature target;
                if (c.target == AbstractCard.CardTarget.ENEMY || c.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                    target = this.getTarget(); // 攻击类：打锁定敌人
                } else if (c.target == AbstractCard.CardTarget.ALL_ENEMY || c.target == AbstractCard.CardTarget.ALL) {
                    target = null; // AOE：无需目标
                } else {
                    target = this; // 增益/技能：打自己
                }

                AbstractDungeon.actionManager.addToBottom(new AllyPlayCardAction(this, c, target));
            }
        }
    }

    // =====================================================================
    // 意图与计算
    // =====================================================================

    @Override
    public void applyPowers() {
        super.applyPowers();
        // 确保有目标（否则模拟器里的 calculateCardDamage 会算错）
        if (this.lockedTarget == null || this.lockedTarget.isDeadOrEscaped()) {
            this.lockedTarget = this.getRandomTarget();
        }
        this.refreshIntentCalculation();
    }

    // 逻辑被 refreshIntentCalculation 接管，getMove 只负责触发重算
    @Override
    public void getMove(int num) {
        this.refreshIntentCalculation();
    }

    // =====================================================================
    // 目标管理
    // =====================================================================

    // 获取当前攻击目标（带锁定逻辑）
    public AbstractMonster getTarget() {
        if (lockedTarget == null || lockedTarget.isDeadOrEscaped()) {
            lockedTarget = this.getRandomTarget();
        }
        return lockedTarget;
    }

    // 随机索敌（排除友军）
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

    // =====================================================================
    // 受击与攻击辅助
    // =====================================================================

    // 防误伤：只对玩家/被允许的来源造成伤害
    @Override
    public void damage(DamageInfo info) {
        if (SS.patches.AllyDamagePatch.allowFriendlyFire.get(info) ||
                (info.owner != AbstractDungeon.player)) {
            super.damage(info);
        }
    }

    @Override
    public void die(boolean triggerRelics) {
        super.die(triggerRelics);
    }

    // 非卡牌攻击的辅助方法（用于 ActionQueue 回调等）
    public void attack(AbstractMonster target, int damage, AbstractGameAction.AttackEffect effect) {
        if (target == null)
            return;

        DamageInfo info = new DamageInfo(this, damage, DamageInfo.DamageType.NORMAL);
        info.applyPowers(this, target); // 计算力量等

        AbstractDungeon.actionManager.addToBottom(new DamageAction(target, info, effect));

        // 触发攻击回调 Helper
        AbstractDungeon.actionManager.addToBottom(new AbstractGameAction() {
            @Override
            public void update() {
                AllyTriggerHelper.triggerOnAllyAttack(AbstractAlly.this, target, info.output);
                this.isDone = true;
            }
        });
    }
}
