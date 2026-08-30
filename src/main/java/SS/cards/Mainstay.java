package SS.cards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cardmodifiers.FindFather;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Mainstay extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Mainstay");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Mainstay.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Mainstay() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, false, false);
        this.exhaust = true;
        this.cardsToPreview = new MultiFacial();
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            this.selfRetain = true;
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        ModHelper.atbLambda(() -> {
            if (p.hand.isEmpty()) {
                return;
            }

            // 1. 按照定义的“颜色”对手牌进行分组
            Map<Object, List<AbstractCard>> colorMap = new HashMap<>();
            for (AbstractCard card : p.hand.group) {
                if (card == this) {
                    continue; // 排除当前这张牌自身
                }

                Object colorKey;
                if (card.color == AbstractCardEnum.SS_Yellow && card instanceof AbstractDoubleCard) {
                    colorKey = ((AbstractDoubleCard) card).packagetype;
                } else {
                    colorKey = card.color;
                }

                colorMap.computeIfAbsent(colorKey, k -> new ArrayList<>()).add(card);
            }

            // 2. 每种颜色组中随机挑选 1 张卡牌
            List<AbstractCard> selectedCards = new ArrayList<>();
            for (List<AbstractCard> cardGroup : colorMap.values()) {
                if (!cardGroup.isEmpty()) {
                    int randomIndex = AbstractDungeon.cardRandomRng.random(cardGroup.size() - 1);
                    selectedCards.add(cardGroup.get(randomIndex));
                }
            }

            // 3. 将所有挑选出的卡牌串联绑定起来（至少需要2张不同颜色的牌）
            if (selectedCards.size() >= 2) {
                for (int i = 0; i < selectedCards.size() - 1; i++) {
                    AbstractCard c1 = selectedCards.get(i);
                    AbstractCard c2 = selectedCards.get(i + 1);
                    FindFather.ConjugateCard(c1, c2);
                }
            }
        });
        addToBot(new MakeTempCardInHandAction(this.cardsToPreview, 1));
    }

    public AbstractDoubleCard makeCopy() {
        return new Mainstay();
    }
}