package SS.action.monster;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

import SS.monster.AbstractCardMonster;
import SS.monster.ally.AbstractAlly;

public class AllyPlayLeftmostCardAction extends AbstractGameAction {
    private AbstractCardMonster owner;

    public AllyPlayLeftmostCardAction(AbstractCardMonster owner) {
        this.owner = owner;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        // 1. 安全检查：友军不存在或手牌为空
        if (owner == null || owner.hand.isEmpty()) {
            this.isDone = true;
            return;
        }

        // 2. 获取最左侧的牌 (Index 0)
        AbstractCard cardToPlay = owner.hand.group.get(0);

        // 3. 能量判定
        if (owner.energy >= cardToPlay.costForTurn) {

            // 4. 确定目标 (复用之前的逻辑)
            AbstractCreature target = null;
            if (owner instanceof AbstractAlly) {
                AbstractAlly ally = (AbstractAlly) owner;

                if (cardToPlay.target == AbstractCard.CardTarget.ENEMY ||
                        cardToPlay.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                    target = ally.getTarget(); // 获取锁定目标
                } else if (cardToPlay.target == AbstractCard.CardTarget.ALL_ENEMY) {
                    target = null; // AOE
                } else {
                    target = ally; // Buff/防御打自己
                }
            }

            // 5. 加入打牌动作 (插队到最前面，立即执行)
            // 使用 addToTop 确保指令优先级最高
            addToTop(new AllyPlayCardAction(owner, cardToPlay, target));

        } else {
            // (可选) 如果能量不足，可以在这里加个提示，比如头顶冒字 "No Energy!"
            // AbstractDungeon.effectList.add(new ThoughtBubble(owner.hb.cX, owner.hb.cY,
            // 3.0F, "我没能量了！", true));
        }

        this.isDone = true;
    }
}