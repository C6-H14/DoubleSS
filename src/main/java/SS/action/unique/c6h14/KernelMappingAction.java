package SS.action.unique.c6h14;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;

public class KernelMappingAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    private boolean isInitialized = false;
    private boolean upgraded; // 是否升级（决定是随机还是选择）
    private ArrayList<AbstractCard> tempInvalidCards = new ArrayList<>(); // 暂存不合规卡牌

    public KernelMappingAction(boolean upgraded) {
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
            // 找出所有符合条件的手牌 (费用在 0 到 2 之间)
            ArrayList<AbstractCard> validCards = new ArrayList<>();
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                if (c.cost >= 0 && c.cost <= 2) {
                    validCards.add(c);
                } else {
                    // 如果不符合条件，记录到暂存区
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

                // 打开选牌界面
                AbstractDungeon.handCardSelectScreen.open("选择一张牌进行转化", 1, false, false);

                this.isInitialized = true;
                this.tickDuration();
                return;
            } else {
                // 【未升级效果】：随机选择 1 张转化
                AbstractCard oldCard = validCards.get(AbstractDungeon.cardRandomRng.random(validCards.size() - 1));

                // 执行转化
                transformCardInHand(oldCard);

                this.isDone = true;
                return;
            }
        }

        // =================================================================
        // 阶段 2：选择完毕，追加转化并还原手牌 (后续帧执行)
        // =================================================================
        if (upgraded && !AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard oldCard : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                // 执行转化并放入手牌
                transformCardInHand(oldCard);
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
     * 将手牌中的旧卡替换为符合规则的随机新卡
     */
    private void transformCardInHand(AbstractCard oldCard) {
        int targetCost = oldCard.cost + 1;

        // 1. 尝试检索 目标费用+1 的卡池
        AbstractCard newCard = getRandomCardOfCost(targetCost);

        // 2. 【保底 1】如果没有这个费用的牌，寻找相同费用的牌
        if (newCard == null) {
            newCard = getRandomCardOfCost(oldCard.cost);
        }

        // 3. 【保底 2】如果依然没有，直接复制原卡本身
        if (newCard == null) {
            newCard = oldCard.makeCopy();
        }

        // 4. 继承原本卡牌的升级状态
        if (oldCard.upgraded) {
            newCard.upgrade();
        }

        // 从手牌（或选牌缓存）中移除旧卡
        if (upgraded) {
            // 选择模式下，旧卡在 selectedCards 里，直接不放回即可
        } else {
            // 随机模式下，需要手动从玩家手牌移除旧卡
            AbstractDungeon.player.hand.removeCard(oldCard);
        }

        // 5. 播放卡牌飞入手牌特效
        newCard.current_x = Settings.WIDTH / 2.0F;
        newCard.current_y = Settings.HEIGHT / 2.0F;
        AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(newCard));
    }

    /**
     * 动态检索符合特定费用的可用卡池
     */
    private AbstractCard getRandomCardOfCost(int targetCost) {
        ArrayList<AbstractCard> pool = new ArrayList<>();

        for (AbstractCard c : CardLibrary.cards.values()) {
            // 筛选条件：
            // 1. 费用相符
            // 2. 排除基础卡 (Strike/Defend) 和 特殊卡 (比如幻影)
            // 3. 排除状态牌和诅咒牌
            if (c.cost == targetCost
                    && c.rarity != AbstractCard.CardRarity.BASIC
                    && c.rarity != AbstractCard.CardRarity.SPECIAL
                    && c.type != AbstractCard.CardType.STATUS
                    && c.type != AbstractCard.CardType.CURSE) {

                // 4. 必须匹配玩家当前的职业颜色，或者是中立无色牌
                if (AbstractDungeon.commonCardPool.group.contains(c)
                        || AbstractDungeon.uncommonCardPool.group.contains(c)
                        || AbstractDungeon.rareCardPool.group.contains(c)) {
                    pool.add(c);
                }
            }
        }

        if (pool.isEmpty()) {
            return null;
        }

        // 随机返回一张复本
        return pool.get(AbstractDungeon.cardRandomRng.random(pool.size() - 1)).makeCopy();
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString("Double:ChooseHandCardAction");
        TEXT = uiStrings.TEXT;
    }
}