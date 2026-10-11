package SS.action.monster;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.monster.AbstractCardMonster;
import SS.helper.MonsterCardContext;

public class AllyPlayCardAction extends AbstractGameAction {
    private AbstractCardMonster owner;
    private AbstractCard card;
    private AbstractCreature target;

    // 动画阶段控制
    private boolean hasMovedToLimbo = false;
    private boolean hasUsedCard = false;
    // 卡牌飞到中央后悬停多久才真正结算。原为 0.5F，加上 ACTION_DUR_MED(0.5) 后
    // 总时长 1.0 秒，而卡牌位置是 6.0/s 的 lerp，实际 0.4 秒左右就飞到位了，
    // 剩下的时间纯属干等。压到 0.1F 后总时长 0.6 秒，观感明显跟手。
    private float holdDuration = 0.1F;

    // 【新增】是否无视能量限制
    private boolean ignoreEnergy = false;

    // 默认构造函数 (正常打牌用)
    public AllyPlayCardAction(AbstractCardMonster owner, AbstractCard card, AbstractCreature target) {
        this(owner, card, target, false);
    }

    // 【新增】重载构造函数 (强制打牌用)
    public AllyPlayCardAction(AbstractCardMonster owner, AbstractCard card, AbstractCreature target,
            boolean ignoreEnergy) {
        this.owner = owner;
        this.card = card;
        this.target = target;
        this.ignoreEnergy = ignoreEnergy;
        this.duration = Settings.ACTION_DUR_MED + holdDuration;
        this.actionType = ActionType.USE;
    }

    @Override
    public void update() {
        // 0. 安全检查
        if (owner == null || card == null) {
            this.isDone = true;
            return;
        }

        // =============================================================
        // 阶段 1: 初始化与飞行 (第一帧执行)
        // =============================================================
        if (!hasMovedToLimbo) {
            // 【核心修改】能量判定逻辑
            if (!this.ignoreEnergy) {
                // 如果不无视能量，且能量不够，直接取消动作
                if (owner.energy < card.costForTurn && card.costForTurn >= 0) {
                    this.isDone = true;
                    return;
                }
            }
            // 扣除能量 (即使 ignoreEnergy=true，也要扣除相应能量)
            if (card.costForTurn > 0) {
                owner.energy -= card.costForTurn;
                // 防止能量扣成负数
                if (owner.energy < 0) {
                    owner.energy = 0;
                }
            } else if (card.costForTurn == -1) {
                // 如果是 X 费牌，清空所有能量
                owner.energy = 0;
            }

            // 移动到 Limbo (防止手牌渲染消失)
            if (owner.hand.contains(card)) {
                owner.hand.removeCard(card);
                owner.limbo.addToBottom(card);
            }

            // 设置目标位置：屏幕正中央
            // 你可以通过调整 Y 轴让它稍微靠上或靠下
            card.target_x = Settings.WIDTH / 2.0F;
            card.target_y = Settings.HEIGHT / 2.0F;

            // 【关键】设置缩放：变小，不挡住画面 (0.5倍大小)
            card.targetDrawScale = 0.5F;

            // 设为发光状态
            card.targetAngle = 0.0F;
            card.superFlash(com.badlogic.gdx.graphics.Color.GOLD.cpy());

            hasMovedToLimbo = true;
        }

        // 减少计时器
        this.duration -= com.badlogic.gdx.Gdx.graphics.getDeltaTime();

        // =============================================================
        // 阶段 2: 飞到中间后，执行效果 (在剩余时间 < holdDuration 时执行)
        // =============================================================
        if (this.duration < holdDuration && !hasUsedCard) {

            // 再次确保卡牌到了中间 (强制吸附，防止低帧率导致没飞到位)
            card.current_x = Settings.WIDTH / 2.0F;
            card.current_y = Settings.HEIGHT / 2.0F;
            // 【新增】智能重定向逻辑
            if (target != null && target instanceof AbstractMonster) {
                AbstractMonster m = (AbstractMonster) target;
                if (m.isDeadOrEscaped() || m.isDying) {
                    // 目标已死，随机找个活着的
                    AbstractMonster newTarget = AbstractDungeon.getMonsters().getRandomMonster(null, true,
                            AbstractDungeon.cardRandomRng);
                    if (newTarget != null) {
                        target = newTarget;
                        // 如果你是 AbstractAlly，顺便更新一下锁定目标
                        if (owner instanceof SS.monster.ally.AbstractAlly) {
                            ((SS.monster.ally.AbstractAlly) owner).lockTarget(newTarget); // 需在Ally里加个setter
                        }
                    }
                }
            }

            // 【核心】真实打出卡牌
            // 战斗统计：魂火的伤害/格挡按「唤魂牌贡献的魂火值」加权均分。
            // 钩点选在这里而非 MonsterCardContext——后者同时被意图模拟与每帧手牌
            // UI 刷新使用，挂那里会把模拟伤害也算进来。
            // 只标记事务开始（记录队列水位），清除交给 CardStats.update() 每帧检查：
            // 不能用 try/finally 包住 use()，因为 use() 只是入队 action，
            // 伤害结算在几帧后，那时窗口早已关闭。仅统计用途，不改战斗行为。
            if (owner instanceof SS.monster.ally.SoulAlly) {
                SS.stats.CardStats.beginSoulDamage();
            }
            MonsterCardContext.run(owner, () -> {
                if (target instanceof AbstractMonster) {
                    card.calculateCardDamage((AbstractMonster) target);
                }
                card.use(AbstractDungeon.player,
                        (target instanceof AbstractMonster) ? (AbstractMonster) target : null);
            });

            // 【守护者钝刺】原版 SharpHidePower.onUseCard 只监听玩家出牌，且伤害目标写死玩家，
            // 友军出牌完全绕过它。这里补一次等价的反伤。
            // 注意：原版是在 UseCardAction.update() 里遍历 AbstractDungeon.player.powers 触发的，
            // 友军的能力不在那条链上，所以不能用 patch onUseCard 的办法接。
            if (card.type == AbstractCard.CardType.ATTACK) {
                triggerGuardianSharpHide(owner);
            }

            // 标记已使用
            hasUsedCard = true;
        }

        // =============================================================
        // 阶段 3: 动作结束 (计时归零)
        // =============================================================
        if (this.duration <= 0.0F) {
            // 移出 Limbo
            if (owner.limbo.contains(card)) {
                owner.limbo.removeCard(card);

                // 【核心修复】逻辑分流
                if (card.type == AbstractCard.CardType.POWER) {
                    // 1. 如果是能力牌：
                    // 直接移除，不进入弃牌堆，也不进入消耗堆。
                    // (它们变成了 Buff 常驻在身上)

                    // 可选：如果你希望能力牌打出后算作"消耗"（某些遗物互动），
                    // 可以把它加到 exhaustPile，但原版逻辑通常是不加的。
                    // 这里我们什么都不做，让它自然消失。
                } else if (card.exhaust) {
                    // 2. 如果是消耗牌 (且不是能力牌)：
                    owner.exhaustPile.addToTop(card);
                } else {
                    // 3. 普通攻击/技能牌：
                    // 进入弃牌堆，等待洗牌循环
                    owner.discardPile.addToTop(card);
                }
            }

            // 重置卡牌状态 (缩小消失)
            card.stopGlowing();
            card.unhover();
            card.untip();
            card.shrink();

            // 视觉效果：
            // 如果是能力牌，通常会有个淡出效果，或者直接缩小
            card.target_x = owner.hb.cX;
            card.target_y = owner.hb.cY;
            owner.refreshIntentCalculation();
            this.isDone = true;
        }
    }

    /**
     * 友军打出攻击牌时，结算敌方身上的「钝刺」(第一幕 Boss 守护者的 SharpHidePower)。
     *
     * <p><b>为什么不用 patch {@code SharpHidePower.onUseCard}</b>：该方法是原版
     * {@code UseCardAction.update()} 在 duration==0.15f 时【遍历 AbstractDungeon.player.powers】
     * 调用的，友军的能力根本不在那条遍历链上，永远不会被调用；而且它内部把伤害目标
     * 硬编码为 {@code AbstractDungeon.player}。两条都得绕开，所以直接在友军出牌处补算。
     *
     * <p>与 {@link SS.patches.DiceDamageTypePatch} 覆盖的原版 {@code ThornsPower} 不同：
     * 守护者用的是 SharpHidePower，两者是各自独立的类。
     *
     * <p>反伤用 {@code DamageType.THORNS}，与原版一致 —— 该类型不会再触发荆棘自身，
     * 避免无限套娃。伤害目标是出牌的友军而【不是】玩家。
     */
    private static void triggerGuardianSharpHide(AbstractCardMonster actor) {
        if (actor == null || AbstractDungeon.getMonsters() == null) {
            return;
        }
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m.isDeadOrEscaped() || m.isDying) {
                continue;
            }
            com.megacrit.cardcrawl.powers.AbstractPower p = m.getPower(
                    com.megacrit.cardcrawl.powers.SharpHidePower.POWER_ID);
            if (p == null || !(p instanceof com.megacrit.cardcrawl.powers.SharpHidePower) || p.amount <= 0) {
                continue;
            }
            p.flash();
            DamageInfo info = new DamageInfo(p.owner, p.amount, DamageInfo.DamageType.THORNS);
            AbstractDungeon.actionManager.addToBottom(new com.megacrit.cardcrawl.actions.common.DamageAction(
                    actor, info, AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
        }
    }
}