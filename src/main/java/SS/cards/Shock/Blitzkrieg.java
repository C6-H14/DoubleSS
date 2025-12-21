package SS.cards.Shock;

import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.ImmolateDice;
import SS.action.dice.ChannelDiceAction;
import SS.action.dice.EvokeAllDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class Blitzkrieg extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("Blitzkrieg");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public Blitzkrieg() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.damage = this.baseDamage = 6;
        this.magicNumber = this.baseMagicNumber = 1;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(2);
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new ImmolateDice(this.damage, m)));
        addToBot(new ChannelDiceAction(new ImmolateDice(this.damage, m)));
        if (needManager()) {
            addToBot(new EvokeAllDiceAction(this.magicNumber));
        }
    }

    public void calculateCardDamage(AbstractMonster mo) {
        int tmp = this.baseDamage;
        this.baseDamage -= GameActionManager.turn - 1;
        super.calculateCardDamage(mo);
        if (this.damage != tmp)
            this.isDamageModified = true;
        this.baseDamage = tmp;
        UpdateDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new Blitzkrieg();
    }
}
