package SS.action.monster;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;

import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower; // 假设你的能力类路径
import SS.vfx.AllyAbsorbCardEffect;

public class SoulColorCycleAction extends AbstractGameAction {
    private SoulAlly ally;
    private AbstractCard.CardType targetType;

    public SoulColorCycleAction(SoulAlly ally, AbstractCard.CardType targetType) {
        this.ally = ally;
        this.targetType = targetType;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (ally == null || ally.isDeadOrEscaped()) {
            this.isDone = true;
            return;
        }

        // 1. 处理旧牌 (如果有)
        if (ally.hasPower(InscribeCardPower.POWER_ID)) {
            InscribeCardPower power = (InscribeCardPower) ally.getPower(InscribeCardPower.POWER_ID);

            // 如果 power 里有存牌 (假设你有 getCard 方法或者 card 字段可见)
            if (power.card != null) {
                AbstractCard oldCard = power.card;

                // 视觉：让旧牌从友军头顶闪现一下，表示打出
                // 注意：这里只是视觉，真实的打出逻辑在 power.playCard() 里
                AbstractCard visualCard = oldCard.makeStatEquivalentCopy();
                visualCard.current_x = ally.hb.cX;
                visualCard.current_y = ally.hb.cY;
                AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(visualCard));

                // 逻辑：打出旧牌
                power.playCard();

                // 插入一个短暂等待，防止动画重叠
                addToTop(new WaitAction(0.3f));
            }
        }

        // 2. 搜索新牌 (从玩家抽牌堆)
        AbstractCard foundCard = null;
        ArrayList<AbstractCard> validCards = new ArrayList<>();

        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (c.type == this.targetType) {
                validCards.add(c);
            } else if (targetType == CardType.STATUS && c.type == CardType.CURSE) {
                validCards.add(c);
            }
        }

        if (!validCards.isEmpty()) {
            // 随机抽取一张
            foundCard = validCards.get(AbstractDungeon.cardRandomRng.random(validCards.size() - 1));

            // 从玩家抽牌堆移除
            AbstractDungeon.player.drawPile.removeCard(foundCard);

            // 视觉：播放吸入动画 (Action -> Effect)
            // 注意：我们把动画放在 Top，这样它会先播放
            AbstractDungeon.topLevelEffects.add(new AllyAbsorbCardEffect(foundCard, ally.hb.cX, ally.hb.cY));

            // 逻辑：存入能力
            // 我们需要给 InscribeCardPower 加一个 setCard 方法
            if (ally.hasPower(InscribeCardPower.POWER_ID)) {
                InscribeCardPower power = (InscribeCardPower) ally.getPower(InscribeCardPower.POWER_ID);

                // 设置费用为0，保留，等等
                foundCard.setCostForTurn(0);
                foundCard.retain = true; // 确保不被弃牌

                power.setCard(foundCard); // 你需要实现这个方法
            }
        } else {
            // 没有找到对应牌，可以在头顶冒个字 "No Card!"
            // AbstractDungeon.effectList.add(new ThoughtBubble(ally.hb.cX, ally.hb.cY,
            // 3.0f, "没有对应卡牌!", true));
        }

        this.isDone = true;
    }
}