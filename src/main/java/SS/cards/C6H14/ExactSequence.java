package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.unique.DiscardPileToTopOfDeckAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.Dice.DefendDice;
import SS.action.dice.ChannelDiceAction;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import basemod.helpers.CardModifierManager;

public class ExactSequence extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("ExactSequence");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public ExactSequence() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setDamage(8);
        setBlock(8);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
        CardModifierManager.addModifier(this, new PaintingModifier());
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        addToBot(new ChannelDiceAction(new DefendDice(this.block, p)));
        addToBot(new DrawCardAction(1));
        addToBot(new DiscardPileToTopOfDeckAction(p));
    }

    @Override
    public void updateManager() {
        super.updateManager();
        upgradeDamage(1);
        upgradeBlock(1);
    }

    @Override
    public void exitManager() {
        super.exitManager();
        upgradeDamage(-1);
        upgradeBlock(-1);
    }

    public AbstractDoubleCard makeCopy() {
        return new ExactSequence();
    }
}
