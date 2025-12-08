package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import SS.power.FiendStance;

public class UpdateFiendStanceDescriptions extends AbstractGameAction {

    public UpdateFiendStanceDescriptions() {
    }

    public void update() {
        AbstractPlayer p = AbstractDungeon.player;
        boolean flag = false;
        if (p.getPower("Double:FiendStance") != null) {
            this.isDone = true;
            return;
        }
        addToTop(new UpdateFiendCardDescriptionsAction(p.getPower("Double:ManagerStance") != null));
        addToTop(new ApplyPowerAction(p, AbstractDungeon.player, new FiendStance(p)));
        if (p.getPower("Double:ManagerStance") != null) {
            addToTop(new RemoveSpecificPowerAction(p, AbstractDungeon.player,
                    "Double:ManagerStance"));
            flag = true;
        }

        this.isDone = true;
    }
}