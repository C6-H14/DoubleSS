package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ChangeStanceAction extends AbstractGameAction {

    public ChangeStanceAction() {
    }

    public void update() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p.getPower("Double:FiendStance") != null) {
            addToBot(new UpdateManagerStanceDescriptions());
            this.isDone = true;
            return;
        } else {
            if (p.getPower("Double:ManagerStance") != null) {
                addToBot(new UpdateFiendStanceDescriptions());
                this.isDone = true;
                return;
            }
        }
        this.isDone = true;
    }
}