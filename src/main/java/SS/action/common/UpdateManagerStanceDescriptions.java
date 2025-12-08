package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import SS.power.ManagerStance;

public class UpdateManagerStanceDescriptions extends AbstractGameAction {

    public UpdateManagerStanceDescriptions() {
    }

    public void update() {
        AbstractPlayer p = AbstractDungeon.player;
        boolean flag = false;
        if (p.getPower("Double:ManagerStance") != null) {
            this.isDone = true;
            return;
        }

        addToTop(new UpdateManagerCardDescriptionsAction(p.getPower("Double:FiendStance") != null));
        addToTop(new ApplyPowerAction(p, AbstractDungeon.player, new ManagerStance(p)));
        if (p.getPower("Double:FiendStance") != null) {
            addToTop(new RemoveSpecificPowerAction(p, AbstractDungeon.player,
                    "Double:FiendStance"));
            flag = true;
        }

        this.isDone = true;
    }
}