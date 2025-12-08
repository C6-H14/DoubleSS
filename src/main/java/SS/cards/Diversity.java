package SS.cards;

import java.util.HashMap;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.Dice.IronwaveDice;
import SS.action.common.UpdateManagerStanceDescriptions;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Diversity extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Diversity");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Diversity.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;
    private HashMap<AbstractCard.CardColor, Boolean> hsmap = new HashMap<AbstractCard.CardColor, Boolean>();

    public Diversity() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.baseDamage = this.damage = 4;
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(2);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new UpdateManagerStanceDescriptions());
        int amount = 0;
        hsmap.clear();
        for (AbstractCard c : p.drawPile.group) {
            if (!hsmap.containsKey(c.color)) {
                ++amount;
                hsmap.put(c.color, true);
            }
        }
        for (AbstractCard c : p.hand.group) {
            if (!hsmap.containsKey(c.color)) {
                ++amount;
                hsmap.put(c.color, true);
            }
        }
        for (AbstractCard c : p.discardPile.group) {
            if (!hsmap.containsKey(c.color)) {
                ++amount;
                hsmap.put(c.color, true);
            }
        }
        hsmap.clear();
        for (int i = 0; i < amount; ++i) {
            addToBot(new ChannelDiceAction(new IronwaveDice(this.damage, m)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Diversity();
    }
}