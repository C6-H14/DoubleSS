package SS.interfaces;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public interface OnAllyAttackSubscriber {
    void onAllyAttack(AbstractMonster minion, AbstractCreature target, int damageAmount);
}
