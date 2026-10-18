package SS.action.unique.c6h14;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class KernelMappingAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    private boolean isInitialized = false;
    /**
     * 是否处于【群主身份】（决定是随机加费还是让玩家指定）。
     *
     * <p>注意：这与卡牌是否强化【无关】。转化方式只由群主身份决定 ——
     * 调用方 {@code KernelMapping.use()} 传的是 {@code needManager()}。
     * 之前这个字段叫 upgraded，极易被误读成强化状态。
     */
    private boolean manager;
    /**
     * 卡牌是否强化。强化只放宽费用限制：未强化只认 0 费牌，强化后认 0 费或 1 费牌。
     * 与 {@link #manager} 是两个独立维度。
     */
    private boolean upgraded;
    private ArrayList<AbstractCard> tempInvalidCards = new ArrayList<>(); // 暂存不合规卡牌

    /** 可以被本次效果加费的牌的费用上限（未强化 0，强化 1）。 */
    private int maxTargetCost() {
        return this.upgraded ? 1 : 0;
    }

    /**
     * 卡牌当前**实际显示/生效**的费用。
     *
     * <p>{@code costForTurn} 才是卡面读的那个数（回合开始会被重置为 cost，被改费时会单独变），
     * 所以 0 费的判断不能只看 {@code cost}。负数（X 费牌 / 不可打出的状态牌）直接原样返回，
     * 让上层用它去和 0 比较从而排除掉。
     */
    private int effectiveCost(AbstractCard c) {
        return c.costForTurn;
    }

    public KernelMappingAction(boolean manager, boolean upgraded) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.manager = manager;
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
            // 找出所有符合条件的手牌：未强化只认 0 费，强化后认 0/1 费。
            // 用 effectiveCost 而不是裸 cost：刚被别的效果改过费的牌，两者会不一致，
            // 按卡面显示的费用来判断才符合玩家预期。
            int limit = maxTargetCost();
            ArrayList<AbstractCard> validCards = new ArrayList<>();
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                int ec = effectiveCost(c);
                if (ec >= 0 && ec <= limit) {
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

            if (manager) {
                // 【群主身份】：让玩家选择
                // 暂时将不合规的卡牌从玩家手牌中抽离，这样选牌界面就只能看到合规的牌了！
                AbstractDungeon.player.hand.group.removeAll(tempInvalidCards);

                // 打开选牌界面
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false);

                this.isInitialized = true;
                this.tickDuration();
                return;
            } else {
                // 【非群主】：随机选择 1 张加费
                AbstractCard oldCard = validCards.get(AbstractDungeon.cardRandomRng.random(validCards.size() - 1));

                // 执行加费
                increaseCardCost(oldCard);

                this.isDone = true;
                return;
            }
        }

        // =================================================================
        // 阶段 2：选择完毕，追加转化并还原手牌 (后续帧执行)
        // =================================================================
        if (manager && !AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard oldCard : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                // 执行加费（牌留在手里，只是变得更贵）
                increaseCardCost(oldCard);
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
     * 给一张手牌加费：本场战斗永久 +1，牌本身留在手里。
     *
     * <p><b>不能用 {@code modifyCostForCombat(1)}</b>。反编译原版实现是这样的：
     * <pre>
     *   if (costForTurn &gt; 0) {           // ① 有本回合费用：改 costForTurn
     *       costForTurn += amount;
     *       cost = costForTurn;
     *   } else if (cost &gt;= 0) {          // ② 否则改 cost
     *       cost += amount;
     *       costForTurn = 0;             // ← 把本回合费用强制清零
     *   }
     * </pre>
     * 0 费牌 {@code costForTurn == 0}，永远走分支 ②：{@code cost} 确实 +1 了，
     * 但紧接着 {@code costForTurn} 被重置成 0。而<b>卡面渲染读的是 costForTurn</b>，
     * 所以玩家看到的是"费用还是 0"、本回合打出去也真的只花 0 费 —— 看起来就是完全没生效，
     * 要等下回合回合开始重置 costForTurn 时才显形。原版这个方法假设你只会给 &gt;0 费的牌加费，
     * 0 费场景是它的盲区。
     *
     * <p>这里手动两件都写：{@code cost} 是本场战斗的基准值（回合开始会同步到 costForTurn），
     * {@code costForTurn} 让改动<b>立刻可见可用</b>。
     */
    private void increaseCardCost(AbstractCard card) {
        card.cost += 1;
        card.costForTurn = card.cost;
        card.isCostModified = true;
        card.isCostModifiedForTurn = true;

        // 闪一下，让玩家看清"被选中的是哪张"。用 superFlash 而不是 flash：
        // 后者画的是白框，前者用牌自身的颜色，和游戏内其它"选中"反馈一致。
        card.superFlash();
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString("Double:KernelMapping");
        TEXT = uiStrings.TEXT;
    }
}