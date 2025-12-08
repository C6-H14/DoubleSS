package SS.cards;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.action.TaotieRiteAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class TaotieRite extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("TaotieRite");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/TaotieRite.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public TaotieRite() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.magicNumber = this.baseMagicNumber = 2;
        this.exhaust = true;
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Gluttony);
        this.tags.add(AbstractCard.CardTags.HEALING);
        this.cardsToPreview = new PeptideStrike();
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new TaotieRiteAction(this.upgraded, this.magicNumber));
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(-1);
            AbstractCard c = new PeptideStrike();
            c.upgrade();
            this.cardsToPreview = c;
            UpdateDescription();
            initializeDescription();
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new TaotieRite();
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }
}