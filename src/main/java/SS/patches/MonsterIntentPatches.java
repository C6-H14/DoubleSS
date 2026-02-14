package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import basemod.ReflectionHacks;
import SS.monster.AbstractCardMonster;

@SpirePatch(clz = AbstractMonster.class, method = "calculateDamage")
public class MonsterIntentPatches {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractMonster __instance, int dmg) {
        // 检查是否是我们的卡牌怪物
        if (__instance instanceof AbstractCardMonster) {
            AbstractCardMonster monster = (AbstractCardMonster) __instance;

            // 如果开启了"模拟锁"，则拦截原版逻辑
            if (monster.isReadingSimulation) {
                // 直接强制设置 intentDmg 为传入的数值 (即模拟器算好的最终值)
                ReflectionHacks.setPrivate(__instance, AbstractMonster.class, "intentDmg", dmg);

                // 【核心】直接返回，跳过原版那些 applyPowers 的计算
                return SpireReturn.Return();
            }
        }

        // 否则继续执行原版逻辑
        return SpireReturn.Continue();
    }
}