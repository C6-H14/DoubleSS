package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;

import SS.modcore.modcore;

public class AddCardRewardAction extends AbstractGameAction {

    public AddCardRewardAction(int amount) {
        this.amount = amount;
    }

    public void update() {
        if (modcore.combatReward != null) {
            modcore.combatReward.addCardReward(this.amount);
        }
        this.isDone = true;
    }
}