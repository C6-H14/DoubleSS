package SS.cards.Haohao;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.Dice.DefendHaoDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class ScrapeBy extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("ScrapeBy");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/ScrapeBy.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public ScrapeBy() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 15;
        this.block = this.baseBlock = 3;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBlock(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void updateManager() {
        upgradeMagicNumber(-3);
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        upgradeMagicNumber(3);
        UpdateDescription();
        initializeDescription();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int others = (p.maxHealth - p.currentHealth) / this.magicNumber;
        for (int i = 0; i < 2 + others; ++i) {
            addToBot(new ChannelDiceAction(new DefendHaoDice(this.block, p)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new ScrapeBy();
    }
}
