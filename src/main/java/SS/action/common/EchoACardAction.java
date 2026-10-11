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
        AbstractCard card = buildEcho(this.cardToEcho, this.free);
        addToTop(new MakeTempCardInHandAction(card, this.amount));
        this.isDone = true;
    }

    /**
     * 生成一张牌的回响版：虚无 + 消耗 + Echo 标签。
     *
     * <p>抽成静态方法是为了让【牌面预览】和【实际入手的牌】走同一段构造逻辑。
     * 之前预览各自 new 一张原牌，结果改了实际效果后预览没跟着变，
     * 玩家看到的和拿到的不是同一张牌。
     *
     * @param source 被回响的原牌（不会被修改，内部复制）
     * @param free   回响牌是否带"本回合免费打出"
     */
    public static AbstractCard buildEcho(AbstractCard source, boolean free) {
        AbstractCard card = source.makeStatEquivalentCopy();
        CardModifierManager.addModifier(card, new EtherealMod());
        CardModifierManager.addModifier(card, new ExhaustMod());
        CardModifierManager.addModifier(card, new EchoTagModifier());
        if (free) {
            card.freeToPlayOnce = true;
        }
        card.tags.add(AbstractCardEnum.Echo);
        return card;
    }
}
