package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;

public class FallenPowerPatches {

    // 目标 ID
    private static final String FALLEN_POWER_ID = "Double:FallenPower";

    // Patch 1: 修改易伤 (Vulnerable) 的效果
    // 原版逻辑：受到的伤害 * 1.5 (或 1.75 带纸蛙)
    // 修改后逻辑：受到的伤害 * 0.75 (减少 25%)
    @SpirePatch(clz = VulnerablePower.class, method = "atDamageReceive")
    public static class InvertVulnerablePatch {
        public static SpireReturn<Float> Prefix(VulnerablePower __instance, float damage, DamageInfo.DamageType type) {
            // 只有当拥有者有 FallenPower 时才触发
            if (__instance.owner.hasPower(FALLEN_POWER_ID)) {
                // 易伤通常只对 NORMAL 类型的伤害生效 (防止影响荆棘、生命流失等)
                if (type == DamageInfo.DamageType.NORMAL) {
                    // 减少 25% 伤害 = 乘以 0.75
                    return SpireReturn.Return(damage * 0.75F);
                }
                // 如果不是 Normal 伤害，易伤本身就不生效，原样返回
                return SpireReturn.Return(damage);
            }
            // 否则执行原版逻辑
            return SpireReturn.Continue();
        }
    }

    // Patch 2: 修改虚弱 (Weak) 的效果
    // 原版逻辑：造成的伤害 * 0.75 (或 0.60 带纸鹤)
    // 修改后逻辑：造成的伤害 * 1.25 (增加 25%)
    @SpirePatch(clz = WeakPower.class, method = "atDamageGive")
    public static class InvertWeakPatch {
        public static SpireReturn<Float> Prefix(WeakPower __instance, float damage, DamageInfo.DamageType type) {
            // 只有当拥有者有 FallenPower 时才触发
            if (__instance.owner.hasPower(FALLEN_POWER_ID)) {
                // 虚弱通常只影响 NORMAL 类型的伤害
                if (type == DamageInfo.DamageType.NORMAL) {
                    // 增加 25% 伤害 = 乘以 1.25
                    return SpireReturn.Return(damage * 1.25F);
                }
                return SpireReturn.Return(damage);
            }
            // 否则执行原版逻辑
            return SpireReturn.Continue();
        }
    }
}