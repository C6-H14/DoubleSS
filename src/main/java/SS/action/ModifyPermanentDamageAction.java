package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.GetAllInBattleInstances;

import SS.cards.Lost.AbstractLostCard;

import java.util.Iterator;
import java.util.UUID;

public class ModifyPermanentDamageAction extends AbstractGameAction {
    private UUID uuid;

    public ModifyPermanentDamageAction(UUID targetUUID, int amount) {
        this.setValues(this.target, this.source, amount);
        this.actionType = ActionType.CARD_MANIPULATION;
        this.uuid = targetUUID;
    }

    public void update() {
        Iterator var1 = GetAllInBattleInstances.get(this.uuid).iterator();

        while (var1.hasNext()) {
            AbstractCard c = (AbstractCard) var1.next();
            if (c instanceof AbstractLostCard) {
                ((AbstractLostCard) c).basePermanentDamage += this.amount;
                if (((AbstractLostCard) c).basePermanentDamage < 0) {
                    ((AbstractLostCard) c).basePermanentDamage = 0;
                }
            }
        }

        this.isDone = true;
    }
}
