package SS.action.common;

import basemod.cardmods.EtherealMod;
import basemod.cardmods.ExhaustMod;
import basemod.helpers.CardModifierManager;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.cardmodifiers.EchoTagModifier;
import SS.path.AbstractCardEnum;

public class EchoACardAction extends AbstractGameAction {
    private final AbstractCard cardToEcho;

    public EchoACardAction(AbstractCard cardToEcho, int amount) {
        this.cardToEcho = cardToEcho;
        this.amount = amount;
    }

    private boolean free = false;
    private boolean permitEcho = false;

    public EchoACardAction(AbstractCard cardToEcho) {
        this(cardToEcho, 1);
    }

    public EchoACardAction(AbstractCard cardToEcho, boolean freeToUse) {
        this(cardToEcho, 1);
        this.free = freeToUse;
    }

    public EchoACardAction(AbstractCard cardToEcho, boolean freeToUse, boolean permitEcho) {
        this(cardToEcho, 1);
        this.free = freeToUse;
        this.permitEcho = permitEcho;
    }

    public void update() {
        if ((this.cardToEcho.hasTag(AbstractCardEnum.Echo) && !this.permitEcho)
                || CardModifierManager.hasModifier(this.cardToEcho, (new EchoTagModifier()).ID)) {
            this.isDone = true;
            return;
        }
        AbstractCard card = this.cardToEcho.makeStatEquivalentCopy();
        CardModifierManager.addModifier(card, new EtherealMod());
        CardModifierManager.addModifier(card, new ExhaustMod());
        CardModifierManager.addModifier(card, new EchoTagModifier());
        if (this.free) {
            card.freeToPlayOnce = true;
        }
        card.tags.add(AbstractCardEnum.Echo);
        addToTop(new MakeTempCardInHandAction(card, this.amount));
        this.isDone = true;
    }
}
