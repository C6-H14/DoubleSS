package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;

import SS.modcore.modcore;
import javassist.CtBehavior;

public class RewardPatch {
    @SpirePatch(clz = CombatRewardScreen.class, method = "setupItemReward")
    public static class LocatorInsertPatch {
        @SpireInsertPatch(locator = DoubleLocator.class)
        public static void InsertPatch(CombatRewardScreen __init) {
            if (!modcore.combatReward.rewards.isEmpty()) {
                __init.rewards.addAll(modcore.combatReward.rewards);
            }
        }

        private static class DoubleLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher.NewExprMatcher matcher = new Matcher.NewExprMatcher(RewardItem.class);
                int line = LineFinder.findAllInOrder(ctBehavior, matcher)[1];
                return new int[] { line };
            }
        }
    }
}
