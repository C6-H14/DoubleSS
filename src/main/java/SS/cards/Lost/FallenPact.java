package SS.cards.Lost;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.FallenPactAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class FallenPact extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("FallenPact");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public FallenPact() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.tags.add(AbstractCardEnum.Permanent);
        this.exhaust = true;
        this.permanentMagicNumber = this.basePermanentMagicNumber = 2;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradePermanentMagicNumber(1);
            UpdateDescription();
            initializeDescription();
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

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new FallenPactAction(this.permanentMagicNumber));
    }

    public AbstractDoubleCard makeCopy() {
        return new FallenPact();
    }
}
