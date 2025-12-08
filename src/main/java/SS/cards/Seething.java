package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.dice.EvokeAllDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.CrimePower;

public class Seething extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Seething");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Seething.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.BASIC;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Seething() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Wrath);
        this.magicNumber = this.baseMagicNumber = 2;
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(0);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new EvokeAllDiceAction(this.magicNumber));
        addToBot(new ApplyPowerAction(p, p, new CrimePower(p, this.magicNumber)));
        if (needFiend()) {
            addToBot(new DrawCardAction(1));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Seething();
    }
}