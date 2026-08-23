package SS.cardmodifiers;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;

import SS.helper.ModHelper;
import basemod.abstracts.AbstractCardModifier;

public class EchoTagModifier extends AbstractCardModifier {
    public static String ID = ModHelper.makePath("EchoTagModifier");
    private static final UIStrings STRINGS = CardCrawlGame.languagePack.getUIString(ID);

    @Override
    public AbstractCardModifier makeCopy() {
        return new EchoTagModifier();
    }

    public EchoTagModifier() {
    }

    @Override
    public String identifier(AbstractCard card) {
        return ID;
    }
}
