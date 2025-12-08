package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;

public class UpdateManagerCardDescriptionsAction extends AbstractGameAction {
    private boolean flag = false;

    public UpdateManagerCardDescriptionsAction(boolean Flag) {
        this.flag = Flag;
    }

    public void update() {
        AbstractPlayer p = AbstractDungeon.player;

        for (AbstractCard c : p.discardPile.group) {
            if (c.hasTag(AbstractCardEnum.Fiend) && flag) {
                ((AbstractDoubleCard) c).exitFiend();
            }
            if (c.hasTag(AbstractCardEnum.Manager)) {
                ((AbstractDoubleCard) c).updateManager();
            }
        }

        for (AbstractCard c : p.drawPile.group) {
            if (c.hasTag(AbstractCardEnum.Fiend) && flag) {
                ((AbstractDoubleCard) c).exitFiend();
            }
            if (c.hasTag(AbstractCardEnum.Manager)) {
                ((AbstractDoubleCard) c).updateManager();
            }
        }

        for (AbstractCard c : p.hand.group) {
            if (c.hasTag(AbstractCardEnum.Fiend) && flag) {
                ((AbstractDoubleCard) c).exitFiend();
            }
            if (c.hasTag(AbstractCardEnum.Manager)) {
                ((AbstractDoubleCard) c).updateManager();
            }
        }

        for (AbstractCard c : p.exhaustPile.group) {
            if (c.hasTag(AbstractCardEnum.Fiend) && flag) {
                ((AbstractDoubleCard) c).exitFiend();
            }
            if (c.hasTag(AbstractCardEnum.Manager)) {
                ((AbstractDoubleCard) c).updateManager();
            }
        }

        this.isDone = true;
    }
}