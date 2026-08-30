package SS.cards;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.action.unique.ss.RevengeAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.SinsPower;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Revenge extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Revenge");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Revenge.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Revenge() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Wrath);
        this.tags.add(AbstractCard.CardTags.HEALING);
        this.damage = this.baseDamage = 7;
        this.magicNumber = this.baseMagicNumber = 2;
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
        initializeDescription();
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
        DamageInfo info = new DamageInfo(p, this.damage, this.damageTypeForTurn);
        if (p.getPower("Double:FiendStance") != null) {
            addToBot(new RevengeAction(m, info, this.magicNumber));
        } else {
            addToBot(new DamageAction(m, info, this.magicNumber));
        }
        int temp = (p.maxHealth - p.currentHealth) * 10 / p.maxHealth / 3;
        for (int i = 0; i < temp; ++i) {
            addToBot(new ChannelDiceAction(new AttackDice(magicNumber, m)));
        }
        addToBot(new ApplyPowerAction(p, p, new SinsPower(p, temp)));
    }

    public AbstractDoubleCard makeCopy() {
        return new Revenge();
    }
}