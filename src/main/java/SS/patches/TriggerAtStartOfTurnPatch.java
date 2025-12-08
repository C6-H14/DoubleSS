package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.power.DecomposePower;

public class TriggerAtStartOfTurnPatch {
    @SpirePatch(clz = AbstractCreature.class, method = "applyStartOfTurnPowers")
    public static class updatePatch {
        public static void Prefix(AbstractCreature __init) {
            if (__init.hasPower("Double:BurningEnvyPower") && __init.hasPower("Double:DecomposePower")) {
                for (AbstractPower p : __init.powers) {
                    if (p instanceof DecomposePower) {
                        continue;
                    }
                    p.atStartOfTurn();
                }
                for (AbstractPower p : __init.powers) {
                    if (p instanceof DecomposePower) {
                        p.atStartOfTurn();
                    }
                }
                return;
            }
        }
    }
}
