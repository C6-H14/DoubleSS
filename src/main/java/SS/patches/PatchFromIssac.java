package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.interfaces.PreSetMoveSubscriber;
import SS.monster.ally.AbstractAlly;

public class PatchFromIssac {
    @SpirePatch(clz = AbstractMonster.class, method = "damage")
    public static class PatchDamage {
        @SpireInsertPatch(rloc = 69, localvars = { "damageAmount" })
        public static void Insert(final AbstractMonster m, final DamageInfo info, @ByRef final int[] _damageAmount) {
            if (info.owner != null && info.owner != m) {
                for (final AbstractPower power : info.owner.powers) {
                    power.onInflictDamage(info, _damageAmount[0], (AbstractCreature) m);
                }
            }
        }
    }

    @SpirePatch(clz = AbstractMonster.class, method = "applyBackAttack")
    public static class PatchApplyBackAttack {
        public static SpireReturn<Boolean> Prefix(final AbstractMonster m) {
            if (m instanceof AbstractAlly) {
                return SpireReturn.Return(false);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = AbstractMonster.class, method = "setMove", paramtypez = { String.class, byte.class,
            AbstractMonster.Intent.class, int.class, int.class, boolean.class })
    public static class PatchSetMove {
        public static boolean preventNextInvoke;

        public static SpireReturn<Void> Prefix(final AbstractMonster monster, final String moveName,
                final byte nextMove, final AbstractMonster.Intent intent, final int baseDamage, final int multiplier,
                final boolean isMultiDamage) {
            if (PatchSetMove.preventNextInvoke) {
                PatchSetMove.preventNextInvoke = false;
                return SpireReturn.Return();
            }
            for (final AbstractPower power : monster.powers) {
                if (power instanceof PreSetMoveSubscriber) {
                    PatchSetMove.preventNextInvoke |= ((PreSetMoveSubscriber) power).receivePreSetMove();
                }
            }
            return SpireReturn.Continue();
        }

        static {
            PatchSetMove.preventNextInvoke = false;
        }
    }

}
