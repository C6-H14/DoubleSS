package SS.cards.Lost;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.EternalDefendDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.DyingPower;
import SS.power.HolyShieldPower;

public class SoulGuard extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("SoulGuard");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Lost/SoulGuard.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public SoulGuard() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.tags.add(AbstractCardEnum.Permanent);
        this.isEthereal = true;
        this.permanentBlock = this.basePermanentBlock = 4;
        this.permanentMagicNumber = this.basePermanentMagicNumber = 2;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradePermanentMagicNumber(-1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(p, p, new HolyShieldPower(p)));
        addToBot(new ReducePowerAction(p, p, new DyingPower(p, this.permanentMagicNumber), this.permanentMagicNumber));
        upgradePermanentMagicNumber(1);
        if (needManager()) {
            addToBot(new ChannelDiceAction(new EternalDefendDice(this.permanentBlock, p)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        SoulGuard c = new SoulGuard();
        c.copyPermanentFieldsFrom(this);
        return c;
    }
}
