package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.unique.c6h14.BrainStormingAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.power.MinuteOfDeathPower;

public class BrainStorming extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("BrainStorming");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public BrainStorming() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.exhaust = true;
        setMagic(1);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(0);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new BrainStormingAction(this));
        if (countPower("Double:MinuteOfDeathPower") <= 0) {
            if (needManager()) {
                selfPower(new MinuteOfDeathPower(p, this.magicNumber * 60 + 240));
            } else {
                selfPower(new MinuteOfDeathPower(p, this.magicNumber * 60));
            }
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new BrainStorming();
    }
}
