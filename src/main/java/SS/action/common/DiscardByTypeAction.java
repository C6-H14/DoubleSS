package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DiscardSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.Iterator;

public class DiscardByTypeAction extends AbstractGameAction {
    private CardType typeToDiscard;

    public DiscardByTypeAction(AbstractCreature source, CardType ct) {
        this.source = source;
        this.duration = Settings.ACTION_DUR_FAST;
        this.typeToDiscard = ct;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            Iterator var1 = AbstractDungeon.player.hand.group.iterator();

            while (var1.hasNext()) {
                AbstractCard c = (AbstractCard) var1.next();
                if (c.type == typeToDiscard) {
                    this.addToTop(new DiscardSpecificCardAction(c));
                }
            }

            this.isDone = true;
        }

    }
}
