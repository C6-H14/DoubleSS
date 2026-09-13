package SS.cards.Haohao;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.AbstractDoubleCard;
import SS.cards.Lost.AbstractLostCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Kindred extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("Kindred");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/Kindred.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ExhaustAction");
    public static final String[] TEXT = uiStrings.TEXT;

    public Kindred() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(0);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        ModHelper.atbLambda(() -> {
            // 1. 筛选手牌中符合 AbstractHaoCard 的卡牌
            ArrayList<AbstractCard> validCards = new ArrayList<>();
            ArrayList<AbstractCard> cannotPickCards = new ArrayList<>();

            for (AbstractCard c : p.hand.group) {
                if (c instanceof AbstractHaoCard && c != this) {
                    validCards.add(c);
                } else {
                    cannotPickCards.add(c);
                }
            }

            // 【特判 1】：合法牌数量为 0，直接 return
            if (validCards.isEmpty()) {
                return;
            }

            // 【特判 2】：合法牌数量恰好为 1，直接默认选择该牌，不打开界面
            if (validCards.size() == 1) {
                processChosenCard(validCards.get(0));
                return;
            }

            // 【合法牌 > 1】：临时移除非目标牌，打开手牌选择界面
            p.hand.group.removeAll(cannotPickCards);
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false, false, false);

            ModHelper.atbLambda(() -> {
                // 将被临时过滤的卡牌恢复回手牌
                for (AbstractCard c : cannotPickCards) {
                    p.hand.addToTop(c);
                }

                if (!AbstractDungeon.handCardSelectScreen.selectedCards.isEmpty()) {
                    AbstractCard chosenCard = AbstractDungeon.handCardSelectScreen.selectedCards.group.get(0);
                    // 放回手牌以便后续统一从手牌移入消耗堆
                    p.hand.addToTop(chosenCard);
                    AbstractDungeon.handCardSelectScreen.selectedCards.clear();

                    // 执行消耗与 Permanent 升级
                    processChosenCard(chosenCard);
                }
            });
        });
    }

    /**
     * 核心逻辑：消耗选定卡牌及全场同名牌，并将总张数作为增益赋给 Permanent 牌
     */
    private void processChosenCard(AbstractCard chosenCard) {
        AbstractPlayer p = AbstractDungeon.player;
        String targetCardId = chosenCard.cardID;
        int amount = 0;

        // 1. 消耗手牌中的所有同名牌
        ArrayList<AbstractCard> toExhaustHand = new ArrayList<>();
        for (AbstractCard c : p.hand.group) {
            if (c.cardID.equals(targetCardId)) {
                toExhaustHand.add(c);
            }
        }
        for (AbstractCard c : toExhaustHand) {
            p.hand.moveToExhaustPile(c);
            amount++;
        }

        // 2. 消耗抽牌堆中的所有同名牌
        ArrayList<AbstractCard> toExhaustDraw = new ArrayList<>();
        for (AbstractCard c : p.drawPile.group) {
            if (c.cardID.equals(targetCardId)) {
                toExhaustDraw.add(c);
            }
        }
        for (AbstractCard c : toExhaustDraw) {
            p.drawPile.moveToExhaustPile(c);
            amount++;
        }

        // 3. 消耗弃牌堆中的所有同名牌
        ArrayList<AbstractCard> toExhaustDiscard = new ArrayList<>();
        for (AbstractCard c : p.discardPile.group) {
            if (c.cardID.equals(targetCardId)) {
                toExhaustDiscard.add(c);
            }
        }
        for (AbstractCard c : toExhaustDiscard) {
            p.discardPile.moveToExhaustPile(c);
            amount++;
        }

        p.hand.refreshHandLayout();

        // 4. 将数值升级逻辑追加到动作队列
        if (amount > 0) {
            final int finalAmount = amount;
            ModHelper.atbLambda(() -> {
                for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
                    if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                        ((AbstractLostCard) c).upgradePermanentDamage(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentBlock(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentMagicNumber(finalAmount);
                    }
                }
                for (AbstractCard c : AbstractDungeon.player.hand.group) {
                    if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                        ((AbstractLostCard) c).upgradePermanentDamage(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentBlock(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentMagicNumber(finalAmount);
                    }
                }
                for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
                    if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                        ((AbstractLostCard) c).upgradePermanentDamage(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentBlock(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentMagicNumber(finalAmount);
                    }
                }
                for (AbstractCard c : AbstractDungeon.player.exhaustPile.group) {
                    if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                        ((AbstractLostCard) c).upgradePermanentDamage(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentBlock(finalAmount);
                        ((AbstractLostCard) c).upgradePermanentMagicNumber(finalAmount);
                    }
                }
            });
        }
    }

    public void updateManager() {
        this.selfRetain = true;
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        this.selfRetain = false;
        UpdateDescription();
        initializeDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new Kindred();
    }
}