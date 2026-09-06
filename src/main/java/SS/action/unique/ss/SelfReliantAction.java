package SS.action.unique.ss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;

public class SelfReliantAction extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Double:ChooseHandCardAction");
    public static final String[] TEXT = uiStrings.TEXT;
    private AbstractPlayer p;

    public SelfReliantAction(int amount) {
        this.p = AbstractDungeon.player;
        this.amount = amount;
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.CARD_MANIPULATION;
    }

    /**
     * 判断两张牌是否为同色：
     * 1. 若两张都是 SS_Yellow，需属于 AbstractDoubleCard 且 packagetype 相同
     * 2. 否则直接比对 card.color
     */
    private boolean isSameColor(AbstractCard a, AbstractCard b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.color == AbstractCardEnum.SS_Yellow && b.color == AbstractCardEnum.SS_Yellow) {
            if (a instanceof AbstractDoubleCard && b instanceof AbstractDoubleCard) {
                return ((AbstractDoubleCard) a).packagetype == ((AbstractDoubleCard) b).packagetype;
            }
        }
        return a.color == b.color;
    }

    @Override
    public void update() {
        // 第一阶段：打开选牌界面
        if (this.duration == this.startDuration) {
            if (this.p.hand.isEmpty()) {
                this.isDone = true;
                return;
            }

            // 打开手牌选择界面，提示玩家选择一张牌
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false);
            this.tickDuration();
            return;
        }

        // 第二阶段：获取选中的牌并执行抽牌堆/弃牌堆检索
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            if (!AbstractDungeon.handCardSelectScreen.selectedCards.isEmpty()) {
                AbstractCard selectedCard = AbstractDungeon.handCardSelectScreen.selectedCards.group.get(0);
                this.p.hand.addToTop(selectedCard);

                // 1. 收集抽牌堆和弃牌堆中所有同色牌
                ArrayList<AbstractCard> validCards = new ArrayList<>();
                for (AbstractCard c : this.p.drawPile.group) {
                    if (isSameColor(c, selectedCard)) {
                        validCards.add(c);
                    }
                }
                for (AbstractCard c : this.p.discardPile.group) {
                    if (isSameColor(c, selectedCard)) {
                        validCards.add(c);
                    }
                }

                // 2. 随机抽取 amount 张牌移入玩家手牌
                if (!validCards.isEmpty()) {
                    Collections.shuffle(validCards, new Random(AbstractDungeon.cardRandomRng.randomLong()));
                    int count = Math.min(this.amount, validCards.size());

                    for (int i = 0; i < count; i++) {
                        AbstractCard c = validCards.get(i);
                        if (this.p.hand.size() < 10) {
                            if (this.p.drawPile.contains(c)) {
                                this.p.drawPile.moveToHand(c, this.p.drawPile);
                            } else if (this.p.discardPile.contains(c)) {
                                this.p.discardPile.moveToHand(c, this.p.discardPile);
                            }
                        } else {
                            // 手牌已满提示
                            this.p.createHandIsFullDialog();
                        }
                    }
                }

                AbstractDungeon.handCardSelectScreen.selectedCards.clear();
                this.p.hand.refreshHandLayout();
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }

        this.tickDuration();
    }
}