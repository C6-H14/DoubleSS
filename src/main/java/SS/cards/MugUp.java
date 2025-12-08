package SS.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
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

import SS.Dice.DefendDice;
import SS.action.ChannelDiceAction;
import SS.action.DowngradeCardAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.CrimePower;

public class MugUp extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("MugUp");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/MugUp.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public MugUp() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.block = this.baseBlock = 11;
        this.magicNumber = this.baseMagicNumber = 3;
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Sloth);
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
            upgradeBlock(4);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void downgrade() {
        if (this.upgraded) {
            this.upgraded = false;
            this.name = NAME;
            upgradeBlock(-4);
            this.upgradedBlock = false;
            UpdateDescription();
            initializeDescription();
        }
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public boolean canDowngrade() {
        return !this.upgraded;
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new DefendDice(this.block, p)));
        if (p.getPower("Double:FiendStance") == null) {
            addToBot(new ApplyPowerAction(p, p, new CrimePower(p, this.magicNumber)));
        } else if (this.upgraded) {
            addToBot(new ApplyPowerAction(p, p, new CrimePower(p, this.magicNumber)));
        }
        if (!this.upgraded) {
            addToBot(new UpgradeSpecificCardAction(this));
        } else {
            addToBot(new DowngradeCardAction(this));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new MugUp();
    }
}