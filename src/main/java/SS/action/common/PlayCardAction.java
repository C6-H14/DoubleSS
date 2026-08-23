package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.actions.utility.UnlimboAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class PlayCardAction extends AbstractGameAction {
    private boolean exhaustCards;
    private AbstractCard card;

    public PlayCardAction(AbstractCreature target, AbstractCard c, boolean exhausts) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
        this.source = AbstractDungeon.player;
        this.target = target;
        this.exhaustCards = exhausts;
        this.card = c;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (canUseCard(card)) {
                // 1. 移除可能附带的灵魂/移动特效
                AbstractDungeon.getCurrRoom().souls.remove(card);

                // 2. 加入 limbo
                AbstractDungeon.player.limbo.group.add(card);
                card.exhaustOnUseOnce = this.exhaustCards;
                card.dontTriggerOnUseCard = false; // 正常打出牌

                // 3. 【关键修复】重置卡牌状态，解决消耗堆牌隐形/透明问题
                card.untip();
                card.unhover();
                card.lighten(true); // 强制将透明度 Alpha 恢复为 1.0 (完全可见)
                card.fadingOut = false; // 取消淡出渐隐状态
                card.transparency = 1.0F; // 恢复完全不透明

                // 4. 设置屏幕中间的初始与目标渲染位置及缩放
                card.current_x = (float) Settings.WIDTH / 2.0F - 300.0F * Settings.scale;
                card.current_y = (float) Settings.HEIGHT / 2.0F;
                card.target_x = (float) Settings.WIDTH / 2.0F - 300.0F * Settings.scale;
                card.target_y = (float) Settings.HEIGHT / 2.0F;
                card.drawScale = 0.12F;
                card.targetDrawScale = 0.75F;
                card.angle = 0.0F;
                card.targetAngle = 0.0F;

                // 5. 计算数值与伤害
                card.applyPowers();

                // 6. 执行出牌队列
                addToBot(new WaitAction(Settings.ACTION_DUR_FASTER));
                addToBot(new UnlimboAction(card));
                addToBot(new NewQueueCardAction(card, this.target, false, true));
            }

            this.isDone = true;
        }
    }

    private boolean canUseCard(AbstractCard c) {
        if (c.type == AbstractCard.CardType.STATUS && c.costForTurn < -1)
            return AbstractDungeon.player.hasRelic("Medical Kit");
        if (c.type == AbstractCard.CardType.CURSE && c.costForTurn < -1)
            return AbstractDungeon.player.hasRelic("Blue Candle");
        return true;
    }
}