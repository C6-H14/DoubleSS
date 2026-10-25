package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.animation.CharacterAnimationController;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import basemod.helpers.CardModifierManager;

public class Eraser extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Eraser");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Eraser.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Eraser() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(0);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new SS.action.unique.c6h14.EraseInscriptionsAction(p));
    }

    @Override
    public void updateManager() {
        super.updateManager();
        if (!CardModifierManager.hasModifier(this, PaintingModifier.ID))
            CardModifierManager.addModifier(this, new PaintingModifier());
    }

    @Override
    public void exitManager() {
        super.exitManager();
        CardModifierManager.removeModifiersById(this, PaintingModifier.ID, false);
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.COMMAND_ALLY;
    }

    public AbstractDoubleCard makeCopy() {
        return new Eraser();
    }
}
