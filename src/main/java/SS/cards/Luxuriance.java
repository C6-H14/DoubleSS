package SS.cards;

import java.util.HashMap;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import SS.Dice.AttackDice;
import SS.Dice.EmptyDiceSlot;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Luxuriance extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Luxuriance");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Luxuriance.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;
    private HashMap<String, Boolean> hsmap = new HashMap<String, Boolean>();

    public Luxuriance() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.damage = this.baseDamage = 7;
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(3);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        int amount = 0;
        hsmap.clear();
        for (AbstractOrb c : p.orbs) {
            if (!hsmap.containsKey(c.ID) && !(c instanceof EmptyDiceSlot) && !(c instanceof EmptyOrbSlot)) {
                ++amount;
                hsmap.put(c.ID, true);
            }
        }
        hsmap.clear();
        addToBot(new DrawCardAction(amount));
    }

    public AbstractDoubleCard makeCopy() {
        return new Luxuriance();
    }
}