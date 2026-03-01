package SS.interfaces;

import com.megacrit.cardcrawl.cards.AbstractCard;

public interface OnPaintingSubscriber {
    void triggerOnPainting(AbstractCard c);
}
