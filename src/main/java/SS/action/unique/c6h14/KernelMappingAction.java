package SS.action.unique.c6h14;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;

public class KernelMappingAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    private boolean isInitialized = false;
    /**
     * 是否处于【群主身份】（决定是随机转化还是让玩家指定）。
     *
     * <p>注意：这与卡牌是否强化【无关】。KernelMapping 的强化只加能量（magicNumber），
     * 转化方式只由群主身份决定 —— 调用方 {@code KernelMapping.use()} 传的是
     * {@code needManager()}。之前这个字段叫 upgraded，极易被误读成强化状态。
     */
    private boolean manager;
    private ArrayList<AbstractCard> tempInvalidCards = new ArrayList<>(); // 暂存不合规卡牌

    public KernelMappingAction(boolean manager) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.manager = manager;
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

            if (manager) {
                // 【群主身份】：让玩家选择
                // 暂时将不合规的卡牌从玩家手牌中抽离，这样选牌界面就只能看到合规的牌了！
                AbstractDungeon.player.hand.group.removeAll(tempInvalidCards);

                // 打开选牌界面
                AbstractDungeon.handCardSelectScreen.open("选择一张牌进行转化", 1, false, false);

                this.isInitialized = true;
                this.tickDuration();
                return;
            } else {
                // 【非群主】：随机选择 1 张转化
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
        if (manager && !AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
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

        // 1. 检索 目标费用+1 的卡池（费用 3 的牌池存在，正常不会落空）
        AbstractCard newCard = getRandomCardOfCost(targetCost);

        // 2. 【保底】同费用重抽。
        //    刻意【不】退化成"复制原卡本身"：那会让玩家看到一张和自己刚打出的牌
        //    一模一样的牌，观感上等同于"什么都没发生"，与"变化为费用加1的随机牌"严重不符。
        //    同费用重抽至少保证换成的是另一张牌。真出现全费用无牌可抽才会走到复制。
        if (newCard == null) {
            newCard = getRandomCardOfCost(oldCard.cost);
        }

        // 3. 【最后保底】连同费用都没有：只能复制原卡，避免卡牌凭空消失
        if (newCard == null) {
            newCard = oldCard.makeCopy();
        }

        // 4. 继承原本卡牌的升级状态
        if (oldCard.upgraded) {
            newCard.upgrade();
        }

        // 从手牌（或选牌缓存）中移除旧卡
        if (manager) {
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
     * 动态检索符合特定费用的可用卡池。
     *
     * <p><b>必须直接遍历 CardGroup 本体，不能拿 CardLibrary 的实例去 contains 比对。</b>
     * 曾经写成：
     * <pre>
     *   for (AbstractCard c : CardLibrary.cards.values()) {
     *       if (AbstractDungeon.commonCardPool.group.contains(c)) { ... }
     *   }
     * </pre>
     * 这恒为 false —— {@code AbstractCard} 没有重写 equals/hashCode，所以
     * {@code List.contains} 用的是【引用相等】；而 {@code modcore.LoadData()} 往卡池里
     * 灌的是 {@code makeStatEquivalentCopy()} 出来的【副本】（见 modcore.java 卡池过滤那段），
     * 与 CardLibrary 里的原对象不是同一个实例。结果候选池永远为空，
     * 一路掉到"复制原卡"的兜底分支 —— 玩家看到的就是打出去的牌原样回到手里。
     */
    private AbstractCard getRandomCardOfCost(int targetCost) {
        ArrayList<AbstractCard> pool = new ArrayList<>();

        // 卡池即"本职业可用牌"的白名单，直接遍历它，顺便完成颜色过滤。
        // srcXxx 是未经过滤的原始池，这里要的是【过滤后】的池（modcore 已按卡包裁剪）。
        addFromPool(pool, AbstractDungeon.commonCardPool, targetCost);
        addFromPool(pool, AbstractDungeon.uncommonCardPool, targetCost);
        addFromPool(pool, AbstractDungeon.rareCardPool, targetCost);

        if (pool.isEmpty()) {
            return null;
        }

        // 随机返回一张复本
        return pool.get(AbstractDungeon.cardRandomRng.random(pool.size() - 1)).makeCopy();
    }

    /** 把某个 CardGroup 中费用相符、且不属于基础/特殊/状态/诅咒/HEAL 的牌收进候选池。 */
    private void addFromPool(ArrayList<AbstractCard> pool, CardGroup group, int targetCost) {
        if (group == null || group.group == null) {
            return;
        }
        for (AbstractCard c : group.group) {
            if (c == null || c.cost != targetCost) {
                continue;
            }
            // 1. 排除基础卡 (Strike/Defend) 和 特殊卡 (比如幻影)
            if (c.rarity == AbstractCard.CardRarity.BASIC || c.rarity == AbstractCard.CardRarity.SPECIAL) {
                continue;
            }
            // 2. 排除状态牌和诅咒牌
            if (c.type == AbstractCard.CardType.STATUS || c.type == AbstractCard.CardType.CURSE) {
                continue;
            }
            // 3. 排除 HEAL 标签牌（回血类发现牌不该被转化出来）
            if (c.hasTag(AbstractCard.CardTags.HEALING)) {
                continue;
            }
            pool.add(c);
        }
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString("Double:ChooseHandCardAction");
        TEXT = uiStrings.TEXT;
    }
}