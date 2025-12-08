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
import com.megacrit.cardcrawl.powers.ThornsPower;

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
}
