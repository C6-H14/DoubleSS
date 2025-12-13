package SS.cards.Lost;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.EternalAttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.DyingPower;

public class Godhead extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("Godhead");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Godhead() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.isEthereal = true;
        this.tags.add(AbstractCardEnum.Permanent);
        this.permanentDamage = this.basePermanentDamage = 9;
        this.permanentMagicNumber = this.basePermanentMagicNumber = 1;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradePermanentDamage(3);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.permanentMagicNumber; ++i) {
            addToBot(new ChannelDiceAction(new EternalAttackDice(this.permanentDamage, p)));
        }
        int amount = 0;
        if (p.hasPower("Double:DyingPower")) {
            amount = p.getPower("Double:DyingPower").amount - 1;
        }
        addToBot(new ReducePowerAction(p, p, "Double:DyingPower", amount));
        if (needManager()) {
            addToBot(new ApplyPowerAction(p, p, new DyingPower(p, this.upgraded ? 2 : 1)));
        }
        upgradePermanentMagicNumber(amount);
    }

    public AbstractDoubleCard makeCopy() {
        return new Godhead();
    }
}
