package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.watcher.PressEndTurnButtonAction;

public class FartAction extends AbstractGameAction {

    public FartAction() {
    }

    public void update() {
        addToBot(new PressEndTurnButtonAction());
        this.isDone = true;
    }
}