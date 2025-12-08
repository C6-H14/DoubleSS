package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.Dice.WitherDice;
import SS.action.ChannelDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.CrimePower;

public class DaggerInSmile extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("DaggerInSmile");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/DaggerInSmile.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public DaggerInSmile() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Envy);
        this.damage = this.baseDamage = 2;
        this.magicNumber = this.baseMagicNumber = 3;
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(2);
            upgradeMagicNumber(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        addToBot(new ChannelDiceAction(new WitherDice(this.magicNumber, m)));
        addToBot(new ApplyPowerAction(p, p, new CrimePower(p, 1)));
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public void updateFiend() {
        upgradeMagicNumber(1);
        UpdateDescription();
        initializeDescription();
    }

    public void exitFiend() {
        upgradeMagicNumber(-1);
        UpdateDescription();
        initializeDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new DaggerInSmile();
    }
}