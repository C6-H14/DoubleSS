package SS.cards.Shock;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class Absorb extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("Absorb");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Absorb() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            this.exhaust = false;
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        if (getShock(m) >= 1) {
            addToBot(new ReducePowerAction(m, p, "Vulnerable", getShock(m)));
            addToBot(new ReducePowerAction(m, p, "Weakened", getShock(m)));
            addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, getShock(m))));
        }
        if (needManager()) {
            addToBot(new ApplyPowerAction(m, p, new WeakPower(m, 1, false)));
            addToBot(new ApplyPowerAction(m, p, new VulnerablePower(m, 1, false)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Absorb();
    }
}
