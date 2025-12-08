package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.AggressionPower;
import SS.power.CrimePower;

public class Aggression extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Aggression");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Aggression.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Aggression() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Pride);
        this.damage = this.baseDamage = 4;
        this.magicNumber = this.baseMagicNumber = 1;
        if (needFiend()) {
            updateFiend();
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
        int M = this.magicNumber;
        for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            if (mo == m) {
                continue;
            }
            addToBot(new ReducePowerAction(mo, p, new AggressionPower(m, M), M));
        }
        if (p.getPower("Double:FiendStance") != null) {
            ++M;
        }
        addToBot(new ApplyPowerAction(m, p, new AggressionPower(m, M), M));
        addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        addToBot(new ApplyPowerAction(p, p, new CrimePower(p, 4)));
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public AbstractDoubleCard makeCopy() {
        return new Aggression();
    }
}