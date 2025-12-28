package SS.cards.Shock;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.NoDrawPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.action.unique.GainVirtueAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class DonQuixoticCharge extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("DonQuixoticCharge");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public DonQuixoticCharge() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.damage = this.baseDamage = 10;
        this.magicNumber = this.baseMagicNumber = 2;
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
        for (int i = 0; i < this.magicNumber; ++i) {
            addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        }
        addToBot(new ApplyPowerAction(p, p, new VulnerablePower(p, 2, false)));
        addToBot(new ApplyPowerAction(p, p, new FrailPower(p, 2, false)));
        addToBot(new ApplyPowerAction(p, p, new NoDrawPower(p)));
        if (needManager()) {
            int amount = 0;
            for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                if (mo.isDying || mo.isDead) {
                    continue;
                }
                ++amount;
            }
            addToBot(new GainVirtueAction(p, amount));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new DonQuixoticCharge();
    }
}
