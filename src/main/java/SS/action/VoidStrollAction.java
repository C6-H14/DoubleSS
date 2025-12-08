package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import basemod.BaseMod;
import basemod.cardmods.EtherealMod;
import basemod.helpers.CardModifierManager;

public class VoidStrollAction extends AbstractGameAction {
    private AbstractPlayer p;
    private int num, cnt;

    public VoidStrollAction(int amount, int num) {
        this.p = AbstractDungeon.player;
        this.cnt = amount;
        this.num = num;
        setValues((AbstractCreature) this.p, (AbstractCreature) this.p);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_MED;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            if ((this.p.drawPile.isEmpty() && this.p.discardPile.isEmpty())
                    || this.p.hand.size() >= BaseMod.MAX_HAND_SIZE || this.cnt == 0) {
                this.isDone = true;

                return;
            }
            if (AbstractDungeon.player.hasPower("No Draw")) {

                AbstractDungeon.player.getPower("No Draw").flash();
                this.isDone = true;

                return;
            }
            if (!this.p.drawPile.isEmpty()) {
                AbstractCard c = AbstractDungeon.player.drawPile.group.get(AbstractDungeon.player.drawPile.size() - 1);
                if (this.num > 0 && !c.retain) {
                    CardModifierManager.addModifier(c, new EtherealMod());
                    this.num = Math.max(0, this.num - 1);
                }
                addToTop(new VoidStrollAction(this.cnt - 1, this.num));
                addToTop(new DrawCardAction(1));
            } else {
                addToTop(new VoidStrollAction(this.cnt, Math.max(0, this.num)));
                addToTop(new EmptyDeckShuffleAction());
            }

            this.isDone = true;

            return;
        }
        tickDuration();
    }
}
