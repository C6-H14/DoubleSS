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

    // =====================================================================
    // 构造与初始化
    // 三个构造函数都最终走父类 (AbstractCardMonster)。healthBarOffsetY 用来抬高
    // 碰撞箱 hb（进而抬高血条/名字/提示框锚点）；立绘 drawX/drawY 则由 commonInit 定死。
    // =====================================================================

    // 构造函数 1：固定碰撞箱 150×150、能量 3
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            float offsetX, float offsetY, float hb_x, float hb_y,
            float healthBarOffsetY) {
        super(name, id, maxHealth, hb_x, hb_y, 150F, 150F, imgUrl, offsetX, offsetY, 3, healthBarOffsetY);
        this.tauntType = type;
        commonInit(offsetX, offsetY);
    }

    // 构造函数 2：固定碰撞箱 150×150，自定义能量
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            float offsetX, float offsetY, float hb_x, float hb_y,
            int energy,
            float healthBarOffsetY) {
        super(name, id, maxHealth, hb_x, hb_y, 150F, 150F, imgUrl, offsetX, offsetY, energy, healthBarOffsetY);
        this.tauntType = type;
        commonInit(offsetX, offsetY);
    }

    // 构造函数 3：完全自定义碰撞箱尺寸与能量（SoulAlly 使用这个）
    public AbstractAlly(
            String name, String id, int maxHealth, String imgUrl, TauntType type,
            float offsetX, float offsetY, float hb_x, float hb_y, float hb_w, float hb_h,
            int energy,
            float healthBarOffsetY) {
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
        // 1. 立绘（图片）渲染位置：以"脚底板"为基准，保持站在地面上
        this.drawX = AbstractDungeon.player.drawX + x;
        this.drawY = AbstractDungeon.player.drawY + y;

        // 2. 【灵肉分离】不用 hbYOffset 抬高度（特效不认它），直接把 Hitbox 本身往上抬。
        //    抬高量 = healthBarOffsetY（构造参数）× scale。
        float verticalShift = this.healthBarOffsetY * Settings.scale;

        // 3. 刷新组件位置，再把 hb 抬到目标高度
        this.refreshHitboxLocation();
        // X: 保持在 drawX
        // Y: 地面(drawY) + 半高(hb.height/2) + 上移量(verticalShift)
        this.hb.move(this.drawX, this.drawY + this.hb.height / 2.0F + verticalShift);

        // 4. 意图碰撞箱甩到无穷远处（配合 AbstractCardMonster 里 render 的 no-op 覆写，
        //    双保险屏蔽原版意图渲染）
        this.intentHb.move(-9999.0F, -9999.0F);

        this.showHealthBar();
        this.healthBarUpdatedEvent();
        this.refreshIntentCalculation();
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
