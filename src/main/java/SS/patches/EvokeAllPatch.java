package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import SS.Dice.EmptyDiceSlot;
import com.megacrit.cardcrawl.actions.defect.TriggerEndOfTurnOrbsAction;

public class EvokeAllPatch {
    @SpirePatch(clz = TriggerEndOfTurnOrbsAction.class, method = "update")
    public static class updatePatch {
        public static void Postfix(TriggerEndOfTurnOrbsAction __init) {
            if (true) {
                for (int i = 0; i < AbstractDungeon.player.orbs.size(); i++) {
                    if (((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyDiceSlot)
                            || ((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyOrbSlot)) {
                        continue;
                    }
                    ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).triggerEvokeAnimation();
                    ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).onEvoke();
                    AbstractDungeon.player.orbs.set(i, new EmptyDiceSlot());
                    ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).setSlot(i, AbstractDungeon.player.maxOrbs);
                }
            }
        }
    }
}
