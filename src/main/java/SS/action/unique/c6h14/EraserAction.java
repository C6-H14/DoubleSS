package SS.action.unique.c6h14;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class EraserAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    private boolean isInitialized = false;
    private boolean upgraded; // 是否升级（决定是随机还是选择）
    private ArrayList<AbstractCard> tempInvalidCards = new ArrayList<>(); // 暂存不合规卡牌

    public EraserAction(boolean upgraded) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.upgraded = upgraded;
    }

    @Override
    public void update() {
        // 0. 安全检查：如果玩家手牌是空的，直接结束
        if (AbstractDungeon.player.hand.isEmpty()) {
            this.isDone = true;
            return;
        }

        // =================================================================
        // 阶段 1：筛选并初始化 (第一帧执行)
        // =================================================================
        if (!isInitialized) {
            // 找出所有符合条件的手牌 (当前耗能大于 0 且不是 X 费的牌)
            ArrayList<AbstractCard> validCards = new ArrayList<>();
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                // c.costForTurn > 0 自动过滤了原本就是 0 费、被减至 0 费、以及不可玩的牌（常设为 -2 费）
                // c.cost != -1 确保过滤掉 X 费牌
                if (c.costForTurn > 0 && c.cost != -1) {
                    validCards.add(c);
                } else {
                    // 如果不符合条件（例如已经是0费，或者是X费/不可玩牌），记录到暂存区
                    tempInvalidCards.add(c);
                }
            }

            // 如果没有符合条件的手牌，直接结束
            if (validCards.isEmpty()) {
                this.isDone = true;
                return;
            }

            if (upgraded) {
                // 【升级效果】：让玩家选择
                // 暂时将不合规的卡牌从玩家手牌中抽离，这样选牌界面就只能看到合规的牌了！
                AbstractDungeon.player.hand.group.removeAll(tempInvalidCards);

                // 打开选牌界面，使用本地化的提示文本 TEXT[0]
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false);

                this.isInitialized = true;
                this.tickDuration();
                return;
            } else {
                // 【未升级效果】：随机选择 1 张将其耗能归零
                AbstractCard targetCard = validCards.get(AbstractDungeon.cardRandomRng.random(validCards.size() - 1));

                // 执行耗能归零
                applyZeroCost(targetCard);

                this.isDone = true;
                return;
            }
        }

        // =================================================================
        // 阶段 2：选择完毕，将所选卡牌耗能归零并还原手牌 (后续帧执行)
        // =================================================================
        if (upgraded && !AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard selected : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                // 执行耗能归零
                applyZeroCost(selected);

                // 放回手牌
                AbstractDungeon.player.hand.addToTop(selected);
            }

            // 【关键】将我们之前隐藏的不合规卡牌重新放回手牌中
            for (AbstractCard c : tempInvalidCards) {
                AbstractDungeon.player.hand.addToTop(c);
            }

            // 清理选牌界面缓存，确保游戏继续
            AbstractDungeon.handCardSelectScreen.selectedCards.clear();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;

            this.isDone = true;
        }

        this.tickDuration();
    }

    /**
     * 将指定卡牌本回合的耗能设为 0 并播放闪烁特效
     */
    private void applyZeroCost(AbstractCard card) {
        card.setCostForTurn(0);
        card.isCostModifiedForTurn = true; // 使耗能数字变绿
        card.superFlash(); // 播放金光闪烁特效
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString("Double:ChooseHandCardAction");
        TEXT = uiStrings.TEXT;
    }
}