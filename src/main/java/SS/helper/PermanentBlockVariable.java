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
        // 只有升级过（upgrade/Perseverance 强化）数值才变绿；
        // 之前写成 tags.contains(Permanent) 恒为 true，导致未升级也永远渲染成绿色
        if (card instanceof AbstractLostCard) {
            return ((AbstractLostCard) card).upgradePermanentBlock;
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