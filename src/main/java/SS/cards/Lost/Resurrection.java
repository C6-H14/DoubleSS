package SS.cards.Lost;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.ResurrectionPower;

public class Resurrection extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("Resurrection");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_power.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Resurrection() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.permanentMagicNumber = this.basePermanentMagicNumber = 40;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradePermanentMagicNumber(20);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(p, p, new ResurrectionPower(p, this.permanentMagicNumber)));
        if (needManager()) {
            addToBot(new HealAction(p, p, 5));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Resurrection();
    }
}
