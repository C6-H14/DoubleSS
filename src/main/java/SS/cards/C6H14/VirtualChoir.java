package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.action.unique.GainVirtueAction;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import basemod.helpers.CardModifierManager;

public class VirtualChoir extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("VirtualChoir");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public VirtualChoir() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setDamage(4);
        setMagic(2);
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
            upgradeMagicNumber(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        addToBot(new GainVirtueAction(p, (needManager() ? 2 : 0) + magicNumber));
    }

    public AbstractDoubleCard makeCopy() {
        return new VirtualChoir();
    }
}
