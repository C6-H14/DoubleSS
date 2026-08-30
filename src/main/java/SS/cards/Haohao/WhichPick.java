package SS.cards.Haohao;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.common.EchoACardAction;
import SS.cardmodifiers.EchoTagModifier; // 请根据你的实际路径确认 EchoTagModifier 的引用
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import basemod.helpers.CardModifierManager;

public class WhichPick extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("WhichPick");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/WhichPick.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public WhichPick() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 3;
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        ModHelper.atbLambda(() -> {
            ArrayList<AbstractCard> validCards = new ArrayList<>();
            String echoTagModifierId = (new EchoTagModifier()).ID;

            for (AbstractCard card : p.hand.group) {
                // 排除当前卡牌自身
                if (card == this) {
                    continue;
                }
                // 排除状态牌和诅咒牌
                if (card.type == AbstractCard.CardType.STATUS || card.type == AbstractCard.CardType.CURSE) {
                    continue;
                }
                // 排除已拥有 EchoTagModifier 的牌
                if (CardModifierManager.hasModifier(card, echoTagModifierId)) {
                    continue;
                }

                validCards.add(card);
            }

            // 若存在符合条件的手牌，随机挑选 1 张执行 EchoACardAction
            if (!validCards.isEmpty()) {
                AbstractCard chosenCard = validCards.get(AbstractDungeon.cardRandomRng.random(validCards.size() - 1));
                if (needManager()) {
                    chosenCard.upgrade();
                }
                addToBot(new EchoACardAction(chosenCard, this.magicNumber));
            }
        });
    }

    @Override
    public AbstractDoubleCard makeCopy() {
        return new WhichPick();
    }
}