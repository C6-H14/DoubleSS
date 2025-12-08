package SS.helper;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.cards.Lost.AbstractLostCard;
import SS.path.AbstractCardEnum;
import basemod.abstracts.DynamicVariable;

public class PermanentBlockVariable extends DynamicVariable {
    @Override
    public String key() {
        return "Double:PermanentBlock";
        // What you put in your localization file between ! to show your value. Eg,
        // !myKey!.
    }

    @Override
    public boolean isModified(AbstractCard card) {
        if (card instanceof AbstractLostCard) {
            return card.tags.contains(AbstractCardEnum.Permanent);
        }
        return false;
        // Set to true if the value is modified from the base value.
    }

    @Override
    public int value(AbstractCard card) {
        if (card instanceof AbstractLostCard) {
            AbstractLostCard c = (AbstractLostCard) card;
            return c.permanentBlock;
        }
        return 0;
        // What the dynamic variable will be set to on your card. Usually uses some kind
        // of int you store on your card.
    }

    @Override
    public int baseValue(AbstractCard card) {
        if (card instanceof AbstractLostCard) {
            AbstractLostCard c = (AbstractLostCard) card;
            return c.basePermanentBlock;
        }
        return 0;
        // Should generally just be the above.
    }

    @Override
    public boolean upgraded(AbstractCard card) {
        if (card instanceof AbstractLostCard) {
            AbstractLostCard c = (AbstractLostCard) card;
            return c.upgradePermanentBlock;
        }
        return false;
        // Should return true if the card was upgraded and the value was changed
    }
}