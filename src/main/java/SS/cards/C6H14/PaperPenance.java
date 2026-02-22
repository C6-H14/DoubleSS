package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.DefendDice;
import SS.action.dice.ChannelDiceAction;
import SS.action.unique.c6h14.PaperPenanceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class PaperPenance extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("PaperPenance");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public PaperPenance() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setBlock(4);
        setMagic(4);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBlock(2);
            upgradeMagicNumber(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new DefendDice(block, p)));
        addToBot(new ChannelDiceAction(new DefendDice(block, p)));
        addVirtue(magicNumber);
        if (needManager()) {
            ModHelper.atbLambda(() -> {
                addToBot(new PaperPenanceAction());
            });
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new PaperPenance();
    }
}
