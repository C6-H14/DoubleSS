package SS.action.monster;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.cards.C6H14.Covenant;

public class ForceAllAlliesExecuteIntentAction extends AbstractGameAction {
    public ForceAllAlliesExecuteIntentAction() {
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        // 为了让友军从左到右依次打出，采用倒序遍历 + addToTop 注入
        for (int i = AllyManager.allies.monsters.size() - 1; i >= 0; i--) {
            AbstractMonster m = AllyManager.allies.monsters.get(i);

            if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                SoulAlly ally = (SoulAlly) m;
                AbstractCard cardToPlay = null;
                AbstractCreature target = ally.getTarget();

                // 1. 判断该友军当前执行什么意图
                if (ally.stateColor != SoulAlly.SoulColor.WHITE) {
                    // 彩色：强制打出《圣约》伤害
                    cardToPlay = new Covenant();
                    cardToPlay.freeToPlayOnce = true;
                } else {
                    // 白色：打出它当前手牌最左侧的牌
                    if (!ally.hand.isEmpty()) {
                        cardToPlay = ally.hand.group.get(0);
                    }
                }

                // 2. 强制执行 (无视能量限制 true)
                if (cardToPlay != null) {
                    addToTop(new AllyPlayCardAction(ally, cardToPlay, target, true));
                }
            }
        }

        this.isDone = true;
    }
}