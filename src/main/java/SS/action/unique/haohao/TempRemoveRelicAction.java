package SS.action.unique.haohao;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import SS.helper.TempRelicManager;

public class TempRemoveRelicAction extends AbstractGameAction {
    private final AbstractRelic relic;

    public TempRemoveRelicAction(AbstractRelic relic) {
        this.relic = relic;
        this.actionType = ActionType.SPECIAL;
    }

    @Override
    public void update() {
        if (this.relic != null) {
            TempRelicManager.removeRelicForCombat(this.relic);
        }
        this.isDone = true;
    }
}