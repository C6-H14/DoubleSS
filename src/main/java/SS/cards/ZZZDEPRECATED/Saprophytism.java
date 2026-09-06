package SS.cards.ZZZDEPRECATED;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.IronwaveDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import basemod.AutoAdd;

@AutoAdd.Ignore

public class Saprophytism extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Saprophytism");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Saprophytism.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Saprophytism() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        setDamage(5);
        setMagic(3);
        initializeDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 1; i >= 0; --i) {
            addToBot(new ChannelDiceAction(new IronwaveDice(this.damage, m)));
        }
    }

    public static int countCards() {
        return AbstractDungeon.player.exhaustPile.size() / 3;
    }

    @Override
    public void calculateCardDamage(final AbstractMonster mo) {
        final int realBaseDamage = this.baseDamage;
        this.baseDamage += this.magicNumber * countCards();
        super.calculateCardDamage(mo);
        this.baseDamage = realBaseDamage;
        this.isDamageModified = (this.damage != this.baseDamage);
    }

    @Override
    public void applyPowers() {
        final int realBaseDamage = this.baseDamage;
        this.baseDamage += this.magicNumber * countCards();
        super.applyPowers();
        this.baseDamage = realBaseDamage;
        this.isDamageModified = (this.damage != this.baseDamage);
    }

    public AbstractDoubleCard makeCopy() {
        return new Saprophytism();
    }
}