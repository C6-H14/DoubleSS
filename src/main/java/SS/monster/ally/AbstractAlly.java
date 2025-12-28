package SS.monster.ally;

import java.util.ArrayList;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.helper.AllyTriggerHelper;
import basemod.ReflectionHacks;
import basemod.abstracts.CustomMonster;

public abstract class AbstractAlly extends CustomMonster {

    public enum TauntType {
        NONE, SOLID, OVERFLOW
    }

    public TauntType tauntType = TauntType.NONE;
    // 【新增 1】 目标锁定变量
    protected AbstractMonster lockedTarget = null;

    // 【新增 2】 标记当前意图是否为 AOE (需要在子类 updateIntent 中设置)
    protected boolean isAOE = false;

    public AbstractAlly(String name, String id, int maxHealth, String imgUrl, TauntType type) {
        this(name, id, maxHealth, imgUrl, type, MathUtils.random(-200.0F, 200.0F) * Settings.scale,
                MathUtils.random(50.0F, 300.0F) * Settings.scale, 150F, 150F);
    }

    public AbstractAlly(String name, String id, int maxHealth, String imgUrl, TauntType type, float bx, float by) {
        this(name, id, maxHealth, imgUrl, type, MathUtils.random(-200.0F, 200.0F) * Settings.scale,
                MathUtils.random(50.0F, 300.0F) * Settings.scale, bx, by);
    }

    public AbstractAlly(String name, String id, int maxHealth, String imgUrl, TauntType type, float x, float y,
            float bx, float by) {
        // 1. 调用父类构造
        // 位置先传 0,0，稍后覆盖。HB大小设为 150x150 (标准大小)
        super(name, id, maxHealth, 0, 0, bx, by, imgUrl, x, y);

        this.drawX = AbstractDungeon.player.drawX + x;
        this.drawY = AbstractDungeon.player.drawY + y;

        this.tauntType = type;

        // 3. 修正 Hitbox 位置 (关键)
        this.hb.move(this.drawX, this.drawY + this.hb.height / 2.0F);

        // 4. 修正血条和意图位置 (关键)
        this.refreshHitboxLocation();
        // 血条在头顶
        this.healthHb.move(this.hb.cX, this.hb.cY + this.hb.height / 2.0F + 10.0F * Settings.scale);
        // 意图在血条上面
        this.intentHb.move(this.hb.cX, this.hb.cY + this.hb.height / 2.0F + 40.0F * Settings.scale);

        // 5. 启动血条
        this.showHealthBar();
        this.healthBarUpdatedEvent();
    }

    @Override
    public void update() {
        // 让原版逻辑处理所有动画、Buff图标位置、受伤闪烁
        super.update();
        this.hb.update();
        this.healthHb.update();
        this.intentHb.update();
        this.hbAlpha = 1.0F;
    }

    @Override
    public void render(SpriteBatch sb) {
        // 【暴力修复 1】
        // 在调用 super.render 之前，不管 update 有没有跑，
        // 我强制把透明度设为 1，把偏移量设为 0。
        // 这样每一帧画图时，系统都认为"血条是满透明度的"且"在正确位置"。
        this.hbAlpha = 1.0F;
        ReflectionHacks.setPrivate(this, AbstractCreature.class, "hbYOffset", 0.0F);

        // 调用父类渲染 (画图片、画血条、画意图)
        super.render(sb);
    }

    // 必须保留：防误伤逻辑
    @Override
    public void damage(DamageInfo info) {
        if (info.owner != null && info.owner != AbstractDungeon.player) {
            super.damage(info);
        }
    }

    // 必须保留：死亡逻辑
    @Override
    public void die(boolean triggerRelics) {
        super.die(triggerRelics);
    }

    // 业务逻辑接口
    public void setTauntType(TauntType type) {
        this.tauntType = type;
    }

    @Override
    public void getMove(int num) {
        this.updateIntent(num);
    }

    @Override
    public void takeTurn() {
    }

    public abstract void updateIntent(int num);

    public void atStartOfTurn() {
    }

    // 在回合结束时清除锁定，以便下回合重新索敌
    public void atEndOfTurn() {
        this.lockedTarget = null;
    }

    // 索敌与攻击辅助方法
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

    protected void attack(AbstractMonster target, int damage, AbstractGameAction.AttackEffect effect) {
        if (target == null)
            return;
        DamageInfo info = new DamageInfo(this, damage, DamageInfo.DamageType.NORMAL);
        info.applyPowers(this, target);
        AbstractDungeon.actionManager.addToBottom(new DamageAction(target, info, effect));
        AbstractDungeon.actionManager.addToBottom(new AbstractGameAction() {
            @Override
            public void update() {
                AllyTriggerHelper.triggerOnAllyAttack(AbstractAlly.this, target, info.output);
                this.isDone = true;
            }
        });
    } // =================================================================
      // 核心逻辑：目标管理
      // =================================================================

    // 获取当前攻击目标 (带锁定逻辑)
    public AbstractMonster getTarget() {
        // 1. 如果没有锁定目标，或者锁定的目标已经死了/逃跑了
        if (lockedTarget == null || lockedTarget.isDeadOrEscaped()) {
            // 重新获取一个随机目标
            lockedTarget = this.getRandomTarget();
        }
        // 2. 返回当前锁定的目标
        return lockedTarget;
    }

    // =================================================================
    // 核心逻辑：响应 Power 变化并刷新意图
    // =================================================================

    @Override
    public void applyPowers() {
        // 1. 确保当前有有效目标
        AbstractMonster target = getTarget();

        // 2. 使用反射获取父类的 intentBaseDmg
        int baseDmg = ReflectionHacks.getPrivate(this, AbstractMonster.class, "intentBaseDmg");

        // 3. 如果当前有意图伤害 (baseDmg > -1)，重新计算
        if (baseDmg > -1) {
            calculateDamage(baseDmg);
        }

        // 4. 刷新意图图标位置和数字
        this.refreshIntentHbLocation();
    }

    protected void calculateDamage(int dmg) {
        // 1. 确定目标 (AOE 则视为无目标/白板)
        AbstractMonster target = isAOE ? null : getTarget();

        float tmp = (float) dmg;

        // [原版逻辑 1] 无尽模式荒疫 (DeadlyEnemies)
        // 原版检查玩家是否有荒疫导致怪物伤害增加。
        // 友军不是怪物，且友军没有荒疫槽位，所以这段逻辑跳过。
        // if (Settings.isEndless && ...) { ... }

        // [原版逻辑 2] 攻击者(友军) Powers - atDamageGive
        // (例如: 力量 Strength, 活力 Vigor)
        for (AbstractPower p : this.powers) {
            tmp = p.atDamageGive(tmp, DamageInfo.DamageType.NORMAL);
        }

        // [原版逻辑 3] 受击者(敌人) Powers - atDamageReceive
        // (例如: 易伤 Vulnerable, 无实体 Intangible)
        if (target != null) {
            for (AbstractPower p : target.powers) {
                tmp = p.atDamageReceive(tmp, DamageInfo.DamageType.NORMAL);
            }
        }

        // [原版逻辑 5] 背袭判定 (Back Attack)
        // 原版: if (this.applyBackAttack()) { tmp = (float)((int)(tmp * 1.5F)); }
        // 友军通常没有"背袭"逻辑。而且以撒Mod的Patch明确禁止了友军背袭。
        // 如果你希望友军能触发夹击，可以保留，否则建议忽略。这里为了"完整性"我先注释掉。
        // if (this.applyBackAttack()) {
        // tmp = (float)((int)(tmp * 1.5F));
        // }

        // [原版逻辑 6] 攻击者(友军) Powers - atDamageFinalGive
        // (例如: 钢笔尖 Pen Nib)
        for (AbstractPower p : this.powers) {
            tmp = p.atDamageFinalGive(tmp, DamageInfo.DamageType.NORMAL);
        }

        // [原版逻辑 7] 受击者(敌人) Powers - atDamageFinalReceive
        if (target != null) {
            for (AbstractPower p : target.powers) {
                tmp = p.atDamageFinalReceive(tmp, DamageInfo.DamageType.NORMAL);
            }
        }

        // [原版逻辑 8] 结算取整
        dmg = MathUtils.floor(tmp);
        if (dmg < 0) {
            dmg = 0;
        }

        ReflectionHacks.setPrivate(this, AbstractMonster.class, "intentDmg", dmg);
    }
}