package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.SunbowPower;

public class Sunbow extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Sunbow");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Sunbow.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Sunbow() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
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
        selfPower(new SunbowPower(p, this.magicNumber));
        if (needManager()) {
            for (AbstractMonster mo : AllyManager.allies.monsters) {
                if (mo instanceof SoulAlly) {
                    addToBot(new ApplyPowerAction(mo, p, new StrengthPower(mo, 1)));
                }
            }
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Sunbow();
    }
}
