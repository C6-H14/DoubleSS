package SS.cards.Shock;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.power.DyingPower;

public class Redirect extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("Redirect");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Shock/Redirect.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL;

    public Redirect() {
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
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int amount = 0;
        amount += getShock(p);
        if (getShock(p) > 0) {
            addToBot(new ReducePowerAction(p, p, "Vulnerable", getShock(p)));
            addToBot(new ReducePowerAction(p, p, "Weakened", getShock(p)));
        }
        for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            amount += getShock(mo);
            if (!needManager() && getShock(mo) > 0) {
                addToBot(new ReducePowerAction(mo, p, "Vulnerable", getShock(mo)));
                addToBot(new ReducePowerAction(mo, p, "Weakened", getShock(mo)));
            }
        }
        addToBot(new ApplyPowerAction(p, p, new DyingPower(p, amount)));
    }

    public AbstractDoubleCard makeCopy() {
        return new Redirect();
    }
}
