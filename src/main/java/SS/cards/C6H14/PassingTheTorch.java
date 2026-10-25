package SS.cards.C6H14;

import SS.action.unique.c6h14.PassingTheTorchAction;
import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class PassingTheTorch extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("PassingTheTorch");
    private static final CardStrings STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);

    public PassingTheTorch() {
        super(ID, STRINGS.NAME, "img/cards/C6H14/Localization.png", -1,
                STRINGS.DESCRIPTION, CardType.SKILL, CardRarity.UNCOMMON, CardTarget.SELF,
                STRINGS, STRINGS.EXTENDED_DESCRIPTION);
        exhaust = true;
        UpdateDescription();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new PassingTheTorchAction(p, freeToPlayOnce, energyOnUse, upgraded, needManager()));
    }

    @Override
    public void upgrade() {
        if (!upgraded) {
            upgradeName();
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public AbstractDoubleCard makeCopy() { return new PassingTheTorch(); }

    @Override
    public String getDefaultCharacterAnimation() { return CharacterAnimationController.COMMAND_ALLY; }
}
