package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.monster.EvokeSoulAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.power.SermonPower;

public class Sermon extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Sermon");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Sermon() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            this.selfRetain = true;
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        selfPower(new SermonPower(p));
        if (needManager()) {
            addToBot(new EvokeSoulAction(1));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Sermon();
    }
}
