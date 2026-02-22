package SS.action.monster;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import SS.monster.AbstractCardMonster;
import SS.monster.ally.AbstractAlly;

public class AllyPlaySpecificCardAction extends AbstractGameAction {
    private AbstractCardMonster owner;
    private AbstractCard cardToPlay;

    public AllyPlaySpecificCardAction(AbstractCardMonster owner, AbstractCard card) {
        this.owner = owner;
        this.cardToPlay = card;
    }

    @Override
    public void update() {
        if (owner == null || cardToPlay == null || !owner.hand.contains(cardToPlay)) {
            this.isDone = true;
            return;
        }

        // 确定目标
        AbstractCreature target = null;
        if (owner instanceof AbstractAlly) {
            AbstractAlly ally = (AbstractAlly) owner;
            if (cardToPlay.target == AbstractCard.CardTarget.ENEMY
                    || cardToPlay.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                target = ally.getTarget();
            } else {
                target = ally;
            }
        }

        // 加入打牌动作 (复用 Phase 3 写的 Action)
        addToTop(new AllyPlayCardAction(owner, cardToPlay, target));

        this.isDone = true;
    }
}