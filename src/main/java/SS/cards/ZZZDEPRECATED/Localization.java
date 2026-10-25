package SS.cards.ZZZDEPRECATED;

import SS.cards.C6H14.AbstractC6H14Card;
import basemod.AutoAdd;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.unique.c6h14.LocalizationAction;
import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

@AutoAdd.Ignore
public class Localization extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Localization");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Localization.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Localization() {
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
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new LocalizationAction(this.upgraded));
        addVirtue(magicNumber);
        if (needManager()) {
            addToBot(new GainEnergyAction(1));
        }
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.COMMAND_ALLY;
    }

    public AbstractDoubleCard makeCopy() {
        return new Localization();
    }
}
