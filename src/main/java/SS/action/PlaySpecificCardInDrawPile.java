package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.AbstractGameAction.ActionType;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.actions.utility.UnlimboAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class PlaySpecificCardInDrawPile extends AbstractGameAction {
    private boolean exhaustCards;
    private AbstractCard card;

    public PlaySpecificCardInDrawPile(AbstractCreature target, AbstractCard c, boolean exhausts) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
        this.source = AbstractDungeon.player;
        this.target = target;
        this.exhaustCards = exhausts;
        this.card = c;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (AbstractDungeon.player.drawPile.isEmpty()) {
                this.isDone = true;
                return;
            }

            if (!AbstractDungeon.player.drawPile.isEmpty() && AbstractDungeon.player.drawPile.group.contains(card)
                    && canUseCard(card)) {
                AbstractDungeon.player.drawPile.group.remove(card);
                AbstractDungeon.getCurrRoom().souls.remove(card);
                card.exhaustOnUseOnce = this.exhaustCards;
                AbstractDungeon.player.limbo.group.add(card);
                card.current_y = -200.0F * Settings.scale;
                card.target_x = Settings.WIDTH / 2.0F + 200.0F * Settings.xScale;
                card.target_y = Settings.HEIGHT / 2.0F;
                card.targetAngle = 0.0F;
                card.lighten(false);
                card.drawScale = 0.12F;
                card.targetDrawScale = 0.75F;
                card.applyPowers();
                addToBot(new WaitAction(Settings.ACTION_DUR_FASTER));
                addToBot(new UnlimboAction(card));
                addToBot(new NewQueueCardAction(card, this.target, false, true));
            }

            this.isDone = true;
        }
    }

    private boolean canUseCard(AbstractCard c) {
        if (c.type == AbstractCard.CardType.STATUS && c.costForTurn < -1)
            return AbstractDungeon.player.hasRelic("Medical Kit");
        if (c.type == AbstractCard.CardType.CURSE && c.costForTurn < -1)
            return AbstractDungeon.player.hasRelic("Blue Candle");
        return true;
    }
}
