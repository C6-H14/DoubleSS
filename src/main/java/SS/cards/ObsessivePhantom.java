package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.DefendDice;
import SS.action.ChannelDiceAction;
import SS.action.ConjugateCardinDrawPileAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.CrimePower;

public class ObsessivePhantom extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("ObsessivePhantom");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/ObsessivePhantom.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public ObsessivePhantom() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.block = this.baseBlock = 7;
        this.magicNumber = this.baseMagicNumber = 2;
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Lust);
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBlock(3);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new DefendDice(this.block, p)));
        addToBot(new ConjugateCardinDrawPileAction(p, m, this.magicNumber));
        addToBot(new ApplyPowerAction(p, p, new CrimePower(p, 2)));
        UpdateExhaustiveDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new ObsessivePhantom();
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
}