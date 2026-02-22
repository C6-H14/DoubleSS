package SS.action.unique.c6h14;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.monster.AllyPlayCardAction;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;

public class CommandAllSoulsAction extends AbstractGameAction {

    public CommandAllSoulsAction() {
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        // 【关键技巧】为了让友军从左到右依次打出，我们使用倒序遍历配合 addToTop
        // 因为 addToTop 是插队逻辑，后加入的会先执行。
        // 所以倒序遍历能保证第一个友军的动作最后被加入，从而最先被执行。
        for (int i = AllyManager.allies.monsters.size() - 1; i >= 0; i--) {
            AbstractMonster m = AllyManager.allies.monsters.get(i);

            // 判断是否是活着的魂火
            if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                SoulAlly ally = (SoulAlly) m;

                // 确保手牌不为空
                if (!ally.hand.isEmpty()) {
                    // 获取最左侧的牌 (第 0 张)
                    AbstractCard cardToPlay = ally.hand.group.get(0);

                    // 确定目标
                    AbstractCreature target = null;
                    if (cardToPlay.target == AbstractCard.CardTarget.ENEMY
                            || cardToPlay.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                        target = ally.getTarget();
                    } else if (cardToPlay.target == AbstractCard.CardTarget.ALL_ENEMY) {
                        target = null; // AOE 不需具体目标
                    } else {
                        target = ally; // 给自己的增益
                    }

                    // 【核心】加入强制打牌动作，传入 true 表示无视能量
                    addToTop(new AllyPlayCardAction(ally, cardToPlay, target, true));
                }
            }
        }

        this.isDone = true;
    }
}