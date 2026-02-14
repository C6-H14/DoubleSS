package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
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
    private float holdDuration = 0.5F; // 打出后悬停等待的时间 (0.5秒)

    public AllyPlayCardAction(AbstractCardMonster owner, AbstractCard card, AbstractCreature target) {
        this.owner = owner;
        this.card = card;
        this.target = target;
        // 设置动作总时长 (移动时间 + 悬停时间)
        // Settings.ACTION_DUR_FAST 大约是 0.25s
        this.duration = Settings.ACTION_DUR_MED;
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
            // 扣除能量
            if (owner.energy < card.costForTurn) {
                this.isDone = true;
                return;
            }
            owner.energy -= card.costForTurn;

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
            MonsterCardContext.run(owner, () -> {
                if (target instanceof AbstractMonster) {
                    card.calculateCardDamage((AbstractMonster) target);
                }
                card.use(AbstractDungeon.player, (target instanceof AbstractMonster) ? (AbstractMonster) target : null);
            });

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
}