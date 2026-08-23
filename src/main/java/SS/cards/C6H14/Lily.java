package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.DiscardToHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.interfaces.OnPaintingSubscriber;
import SS.power.PaintingPower;

public class Lily extends AbstractC6H14Card implements OnPaintingSubscriber {
    public static final String ID = ModHelper.makePath("Lily");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Lily.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;
    private boolean played = false;

    public Lily() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setDamage(4);
        if (needManager()) {
            updateManager();
        }
        played = false;
        UpdateDescription();
    }

    public void atTurnStart() {
        played = false;
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
        addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        if (needManager() && !played) {
            addToBot(new ApplyPowerAction(p, p, new PaintingPower(p, 1)));
        }
        played = true;
    }

    @Override
    public void triggerOnPainting(AbstractCard c) {
        addToBot(new DiscardToHandAction(this));
    }

    public AbstractDoubleCard makeCopy() {
        return new Lily();
    }
}
