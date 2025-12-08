package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.helper.ModHelper;
import SS.power.VirtuesPower;

public class Indulgence extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Indulgence");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Indulgence.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCard.CardColor.COLORLESS;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Indulgence() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.exhaust = true;
        initializeDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        boolean flag = false;
        if (p.hasPower("Double:CrimePower") || p.hasPower("Double:SinsPower")) {
            flag = true;
        }
        addToBot(new RemoveSpecificPowerAction(p, p, "Double:CrimePower"));
        addToBot(new RemoveSpecificPowerAction(p, p, "Double:SinsPower"));
        if (flag && this.upgraded) {
            addToBot(new ApplyPowerAction(p, p, new VirtuesPower(p, 1)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Indulgence();
    }
}