package SS.cards.BlessCard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.MetallicizePower;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.modcore.modcore;
import SS.path.AbstractCardEnum;

public class BlessDefend extends AbstractBlessCard {
    public static final String ID = ModHelper.makePath("BlessDefend");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Defend.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.BASIC;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    static {
        blessCardFrom = "Double:Defend";
    }

    public BlessDefend() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.block = this.baseBlock = 12;
        this.magicNumber = this.baseMagicNumber = 6;
        this.tags.add(AbstractCard.CardTags.STARTER_DEFEND);
        modcore.blessMap.put(this.blessCardFrom, this.cardID);
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBlock(4);
            upgradeMagicNumber(3);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int setBlock = this.block;
        addToBot(new ChannelDiceAction(new AttackDice(setBlock, m)));
        addToBot(new ApplyPowerAction(p, p, new MetallicizePower(p, this.magicNumber)));
    }

    public AbstractDoubleCard makeCopy() {
        return new BlessDefend();
    }
}