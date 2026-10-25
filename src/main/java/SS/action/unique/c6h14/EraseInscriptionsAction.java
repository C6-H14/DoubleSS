package SS.action.unique.c6h14;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class EraseInscriptionsAction extends AbstractGameAction {
    private final AbstractPlayer player;

    public EraseInscriptionsAction(AbstractPlayer player) { this.player = player; }

    private int erase(AbstractCreature owner, Set<AbstractCard> removed) {
        if (!(owner.getPower(InscribeCardPower.POWER_ID) instanceof InscribeCardPower)) return 0;
        InscribeCardPower power = (InscribeCardPower) owner.getPower(InscribeCardPower.POWER_ID);
        AbstractCard card = power.card;
        if (card == null) return 0;
        power.setCard(null);
        // Inscription temporarily changes costForTurn to zero. Refund its combat
        // cost instead; unplayable and X-cost cards have no fixed energy refund.
        return removed.add(card) ? Math.max(0, card.cost) : 0;
    }

    @Override
    public void update() {
        Set<AbstractCard> removed = Collections.newSetFromMap(new IdentityHashMap<AbstractCard, Boolean>());
        int energy = erase(player, removed);
        for (AbstractMonster ally : AllyManager.allies.monsters) {
            if (ally instanceof SoulAlly && !ally.isDeadOrEscaped()) energy += erase(ally, removed);
        }
        if (energy > 0) addToTop(new GainEnergyAction(energy));
        isDone = true;
    }
}
