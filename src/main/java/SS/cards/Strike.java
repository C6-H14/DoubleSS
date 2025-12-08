package SS.cards;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Strike extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Strike");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Strike.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.BASIC;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Strike() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.damage = this.baseDamage = 6;
        this.tags.add(AbstractCard.CardTags.STARTER_STRIKE);
        this.tags.add(AbstractCard.CardTags.STRIKE);
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(3);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int setDamage = this.damage;
        /*
         * if (AbstractDungeon.player.getPower("Strength")!=null)
         * {
         * setDamage+=AbstractDungeon.player.getPower("Strength").amount;
         * }
         */
        addToBot(new ChannelDiceAction(new AttackDice(setDamage, m)));
    }

    public AbstractDoubleCard makeCopy() {
        return new Strike();
    }
}