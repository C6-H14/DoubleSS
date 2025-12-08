package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.MiserlyPower;

public class Miserly extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Miserly");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Miserly.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Miserly() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.magicNumber = this.baseMagicNumber = 5;
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Greed);
        this.tags.add(AbstractCard.CardTags.HEALING);
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
        initializeDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            this.upgraded = true;
            upgradeName();
            upgradeMagicNumber(3);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void updateFiend() {
        upgradeMagicNumber(2);
        UpdateDescription();
        initializeDescription();
    }

    public void exitFiend() {
        upgradeMagicNumber(-2);
        UpdateDescription();
        initializeDescription();
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(p, p, new MiserlyPower((AbstractCreature) p, this.magicNumber)));
    }

    public AbstractDoubleCard makeCopy() {
        return new Miserly();
    }
}