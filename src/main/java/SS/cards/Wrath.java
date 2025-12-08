package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.ChannelDiceAction;
import SS.action.IncreaseCostAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.CrimePower;

public class Wrath extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Wrath");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Wrath.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;
    private int playedTime = 0;

    public Wrath() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Wrath);
        this.damage = this.baseDamage = 6;
        this.magicNumber = this.baseMagicNumber = 3;
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(3 * (int) Math.pow(2, this.playedTime));
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        if (p.getPower("Double:FiendStance") != null) {
            addToBot(new ChannelDiceAction(new AttackDice(damage + this.magicNumber, m)));
        } else {
            addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        }
        upgradeDamage(this.baseDamage);
        addToBot(new IncreaseCostAction(this.uuid, 1));
        this.playedTime++;
        this.playedTime = Math.min(this.playedTime, 20);
        addToBot(new ApplyPowerAction(p, p, new CrimePower(p, 2)));
        UpdateDescription();
        initializeDescription();
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public void updateFiend() {
        for (int i = 1; i <= this.playedTime; ++i) {
            upgradeMagicNumber(this.magicNumber);
        }
        UpdateDescription();
        initializeDescription();
    }

    public void exitFiend() {
        upgradeMagicNumber(3 - this.magicNumber);
        UpdateDescription();
        initializeDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new Wrath();
    }
}