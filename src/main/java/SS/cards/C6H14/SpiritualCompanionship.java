package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.monster.EvokeSoulAction;
import SS.cards.AbstractDoubleCard;
import SS.cards.Indulgence;
import SS.helper.ModHelper;

public class SpiritualCompanionship extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("SpiritualCompanionship");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/SpiritualCompanionship.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.BASIC;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public SpiritualCompanionship() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 1;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
        this.cardsToPreview = new Indulgence();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            this.cardsToPreview.upgrade();
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new EvokeSoulAction(this.magicNumber));
        AbstractCard c = new Indulgence();
        if (this.upgraded)
            c.upgrade();
        addToBot(new MakeTempCardInHandAction(c));
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
        return new SpiritualCompanionship();
    }
}