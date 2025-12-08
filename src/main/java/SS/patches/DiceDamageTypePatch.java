package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.CurlUpPower;
import com.megacrit.cardcrawl.powers.DoubleDamagePower;
import com.megacrit.cardcrawl.powers.EnvenomPower;
import com.megacrit.cardcrawl.powers.FlightPower;
import com.megacrit.cardcrawl.powers.MalleablePower;
import com.megacrit.cardcrawl.powers.PenNibPower;
import com.megacrit.cardcrawl.powers.SlowPower;
import com.megacrit.cardcrawl.powers.ThornsPower;
import com.megacrit.cardcrawl.powers.watcher.VigorPower;

import SS.path.DamageInfoEnum;

public class DiceDamageTypePatch {
    @SpirePatch(clz = ThornsPower.class, method = "onAttacked")
    public static class updateThornsPowerPatch {
        @SpirePrefixPatch
        public static SpireReturn<Integer> Prefix(ThornsPower __init, DamageInfo info, int damageAmount) {
            if (info.type == DamageInfoEnum.DICE) {
                return SpireReturn.Return(damageAmount);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CurlUpPower.class, method = "onAttacked")
    public static class updateCurlUpPowerPatch {
        @SpirePrefixPatch
        public static void Prefix(CurlUpPower __init, DamageInfo info, int damageAmount) {
            if (info.type == DamageInfoEnum.DICE) {
                info.type = DamageType.NORMAL;
            }
        }
    }

    @SpirePatch(clz = DoubleDamagePower.class, method = "atDamageGive")
    public static class updateDoubleDamagePowerPatch {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(DoubleDamagePower __init, float damage, DamageInfo.DamageType type) {
            if (type == DamageInfoEnum.DICE) {
                return SpireReturn.Return(damage * 2.0F);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = EnvenomPower.class, method = "onAttack")
    public static class updateEnvenomPowerPatch {
        @SpirePrefixPatch
        public static void Prefix(EnvenomPower __init, DamageInfo info, int damageAmount, AbstractCreature target) {
            if (info.type == DamageInfoEnum.DICE) {
                info.type = DamageType.NORMAL;
            }
        }
    }

    @SpirePatch(clz = FlightPower.class, method = "calculateDamageTakenAmount")
    public static class updateFlightPowerPatch {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(FlightPower __init, float damage, DamageInfo.DamageType type) {
            if (type == DamageInfoEnum.DICE) {
                return SpireReturn.Return(damage);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = MalleablePower.class, method = "onAttacked")
    public static class updateMalleablePowerPatch {
        @SpirePrefixPatch
        public static void Prefix(MalleablePower __init, DamageInfo info, int damageAmount) {
            if (info.type == DamageInfoEnum.DICE) {
                info.type = DamageType.NORMAL;
            }
        }
    }

    @SpirePatch(clz = PenNibPower.class, method = "atDamageGive")
    public static class updatePenNibPowerPatch {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(PenNibPower __init, float damage, DamageInfo.DamageType type) {
            if (type == DamageInfoEnum.DICE) {
                return SpireReturn.Return(damage * 2.0F);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = SlowPower.class, method = "atDamageReceive")
    public static class updateSlowPowerPatch {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(SlowPower __init, float damage, DamageInfo.DamageType type) {
            if (type == DamageInfoEnum.DICE) {
                return SpireReturn.Return(damage * (1.0F + __init.amount * 0.1F));
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = VigorPower.class, method = "atDamageGive")
    public static class updateVigorPowerPatch {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(VigorPower __init, float damage, DamageInfo.DamageType type) {
            if (type == DamageInfoEnum.DICE) {
                return SpireReturn.Return(damage + __init.amount);
            }
            return SpireReturn.Continue();
        }
    }
}
