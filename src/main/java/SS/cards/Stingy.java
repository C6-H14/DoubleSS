package SS.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.UpgradeSpecificCardAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Lightning;

import SS.Dice.AttackDice;
import SS.Dice.DefendDice;
import SS.action.ChannelDiceAction;
import SS.action.DowngradeCardAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.CrimePower;
import SS.power.MiserlyPower;

public class Stingy extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Stingy");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Stingy.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Stingy() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.damage = this.baseDamage = 5;
        this.magicNumber = this.baseMagicNumber = 15;
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Greed);
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
        initializeDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            this.upgraded = true;
            upgradeName();
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractDungeon.player.loseGold(this.magicNumber);
        for (int i = 0; i < this.magicNumber; ++i) {
            addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        }
        if (needFiend()) {
            addToBot(new DrawCardAction(1));
        }
        addToBot(new ApplyPowerAction(p, p, new CrimePower(p, 2)));
    }

    public AbstractDoubleCard makeCopy() {
        return new Stingy();
    }
}