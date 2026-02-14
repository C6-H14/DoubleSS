package SS.action.common;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import SS.monster.ally.AbstractAlly;

public class AllyTurnPlannerAction extends AbstractGameAction {
    private AbstractAlly ally;

    public AllyTurnPlannerAction(AbstractAlly ally) {
        this.ally = ally;
    }

    @Override
    public void update() {
        // 0. 安全检查
        if (ally == null || ally.isDeadOrEscaped()) {
            this.isDone = true;
            return;
        }

        // 1. 寻找第一张能打出的牌 (贪婪逻辑：从左到右)
        AbstractCard cardToPlay = null;

        // 使用副本遍历，防止并发修改
        ArrayList<AbstractCard> hand = ally.hand.group;
        for (AbstractCard c : hand) {
            if (ally.energy >= c.costForTurn) {
                cardToPlay = c;
                break; // 找到一张就停，打完这张再说
            }
        }

        if (cardToPlay != null) {
            // =========================================================
            // 情况 A: 有牌可打
            // =========================================================

            // 2.1 确定目标
            AbstractCreature target = null;
            if (cardToPlay.target == AbstractCard.CardTarget.ENEMY
                    || cardToPlay.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                target = ally.getTarget();
            } else if (cardToPlay.target == AbstractCard.CardTarget.ALL_ENEMY) {
                target = null;
            } else {
                target = ally;
            }
            // 2.2 加入"打牌动作"到队列前端
            addToBot(new AllyPlayCardAction(ally, cardToPlay, target));

            // 2.3 【关键】把"规划器"再次加入队列前端 (放在打牌动作之后)
            // 这样等牌打完后，会再次运行这个 Planner，检查有没有新抽上来的牌
            addToBot(new AllyTurnPlannerAction(ally));

        } else {
            // =========================================================
            // 情况 B: 没牌打/没能量 -> 回合结束
            // =========================================================

            // 加入清理动作
            addToTop(new AbstractGameAction() {
                @Override
                public void update() {
                    ally.endTurnDeckLogic(); // 丢弃手牌
                    ally.lockedTarget = null; // 重置索敌
                    this.isDone = true;
                }
            });
        }

        this.isDone = true;
    }
}