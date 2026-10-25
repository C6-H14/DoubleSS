package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class Sermon extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Sermon");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Sermon.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Sermon() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
        this.exhaust = true;
        this.tags.add(CardTags.HEALING);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int sins = p.hasPower("Double:SinsPower") ? Math.max(0, p.getPower("Double:SinsPower").amount) : 0;
        int virtue = sins * magicNumber + (needManager() ? magicNumber : 0);
        if (virtue > 0) addVirtue(virtue);
    }

    @Override
    public void updateManager() {
        super.updateManager();
    }

    @Override
    public void exitManager() {
        super.exitManager();
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.CAST_PRAYER;
    }

    public AbstractDoubleCard makeCopy() {
        return new Sermon();
    }
}
