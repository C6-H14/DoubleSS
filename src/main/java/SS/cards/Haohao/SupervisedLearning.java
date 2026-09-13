package SS.cards.Haohao;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import SS.Dice.AbstractDice;
import SS.Dice.AttackHaoDice;
import SS.Dice.DefendHaoDice;
import SS.Dice.EmptyDiceSlot;
import SS.action.dice.RemoveDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.cards.Lost.AbstractLostCard;
import SS.cards.Shock.AbstractShockCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class SupervisedLearning extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("SupervisedLearning");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/SupervisedLearning.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ExhaustAction");
    public static final String[] TEXT = uiStrings.TEXT;

    public SupervisedLearning() {
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

    public boolean canUse(AbstractPlayer p, AbstractMonster m) {
        boolean canUse = super.canUse(p, m);
        if (!canUse) {
            return false;
        }
        if (needManager()) {
            for (AbstractOrb o : AbstractDungeon.player.orbs) {
                if (o instanceof AbstractDice && !(o instanceof EmptyDiceSlot)) {
                    return true;
                }
            }
            return false;
        }
        for (AbstractOrb o : AbstractDungeon.player.orbs) {
            if (o instanceof AttackHaoDice || o instanceof DefendHaoDice) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        ArrayList<String> s = new ArrayList<>();
        s.add("Double:AttackHaoDice");
        s.add("Double:DefendHaoDice");
        addToBot(new RemoveDiceAction(s, 1));
        ModHelper.atbLambda(() -> {
            ArrayList<AbstractCard> validCards = new ArrayList<>();
            for (AbstractCard c : CardLibrary.cards.values()) {
                if (c instanceof AbstractShockCard && !c.hasTag(AbstractCard.CardTags.HEALING)) {
                    validCards.add(c);
                }
            }

            if (!validCards.isEmpty()) {
                // 使用 cardRandomRng 保证随机数种子一致性
                AbstractCard chosenCard = validCards.get(
                        AbstractDungeon.cardRandomRng.random(validCards.size() - 1)).makeCopy();

                // 若处于 Manager 状态，则该卡牌本回合临时变为 0 费
                if (needManager()) {
                    chosenCard.setCostForTurn(0);
                }

                addToBot(new MakeTempCardInHandAction(chosenCard, 1));
            }
        });
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
        return new SupervisedLearning();
    }
}