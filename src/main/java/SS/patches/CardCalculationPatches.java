package SS.patches;

import java.util.ArrayList;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.monster.AbstractCardMonster;
import SS.helper.MonsterCardContext;

public class CardCalculationPatches {

    // 拦截 1: applyPowers (计算对自己/无目标时的数值，如手牌显示的伤害)
    @SpirePatch(clz = AbstractCard.class, method = "applyPowers")
    public static class ApplyPowersPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractCard __instance) {
            // 如果上下文里没有怪物，说明是玩家在看牌，执行原版逻辑
            if (MonsterCardContext.activeMonster == null) {
                return SpireReturn.Continue();
            }

            // --- 进入怪物逻辑 ---
            AbstractCardMonster m = MonsterCardContext.activeMonster;

            // 1. 重置数值
            __instance.isDamageModified = false;
            __instance.damage = __instance.baseDamage;
            __instance.isBlockModified = false;
            __instance.block = __instance.baseBlock;
            __instance.magicNumber = __instance.baseMagicNumber;
            __instance.isMagicNumberModified = false;

            // 2. 计算伤害 (模拟玩家的力量逻辑)
            // 遍历怪物的 Power，寻找影响伤害的 Power (如 Strength)
            if (m.powers != null) {
                for (AbstractPower p : m.powers) {
                    // atDamageGive 通常处理 力量(Strength)、活力(Vigor) 等
                    __instance.damage = (int) p.atDamageGive(__instance.damage, __instance.damageTypeForTurn);
                }

                // 处理“最终”修正 (如 钢笔尖) - 虽然怪物通常没有 Relic，但有些 Power 也在这一步
                for (AbstractPower p : m.powers) {
                    __instance.damage = (int) p.atDamageFinalGive(__instance.damage, __instance.damageTypeForTurn);
                }
            }
            ArrayList<com.megacrit.cardcrawl.monsters.AbstractMonster> monsters = AbstractDungeon
                    .getCurrRoom().monsters.monsters;
            float[] tmp = new float[monsters.size()];
            __instance.multiDamage = new int[monsters.size()];

            for (int i = 0; i < tmp.length; i++) {
                AbstractMonster mo = monsters.get(i);
                tmp[i] = (float) __instance.baseDamage;

                // A. 施法者(友军)的加成
                if (m.powers != null) {
                    for (AbstractPower p : m.powers) {
                        tmp[i] = p.atDamageGive(tmp[i], __instance.damageTypeForTurn);
                    }
                }

                // B. 受击者(敌人)的加成 (如易伤)
                // 注意：这里要判断 mo 是否是 target。对于 AOE，所有活着的怪都是 target。
                if (m.powers != null) {
                    for (AbstractPower p : m.powers) {
                        tmp[i] = p.atDamageFinalGive(tmp[i], __instance.damageTypeForTurn);
                    }
                }

                if (mo != null) {
                    for (AbstractPower p : mo.powers) {
                        tmp[i] = p.atDamageReceive(tmp[i], __instance.damageTypeForTurn);
                    }
                    for (AbstractPower p : mo.powers) {
                        tmp[i] = p.atDamageFinalReceive(tmp[i], __instance.damageTypeForTurn);
                    }
                }

                // 取整存入
                __instance.multiDamage[i] = (int) Math.floor(tmp[i]);
                if (__instance.multiDamage[i] < 0)
                    __instance.multiDamage[i] = 0;
            }
            // 标记 AOE 伤害是否被修改
            if (__instance.baseDamage != __instance.multiDamage[0]) {
                __instance.isDamageModified = true;
            }

            // 3. 计算格挡 (模拟玩家的敏捷逻辑)
            // 遍历怪物的 Power，寻找影响格挡的 Power (如 Dexterity)
            float tmpBlock = (float) __instance.baseBlock;
            if (m.powers != null) {
                for (AbstractPower p : m.powers) {
                    tmpBlock = p.modifyBlock(tmpBlock);
                }
            }
            __instance.block = (int) Math.floor(tmpBlock);
            if (__instance.block < 0)
                __instance.block = 0;

            // 4. 设置修改标记
            if (__instance.damage != __instance.baseDamage)
                __instance.isDamageModified = true;
            if (__instance.block != __instance.baseBlock)
                __instance.isBlockModified = true;

            // 阻止原版逻辑运行
            return SpireReturn.Return();
        }
    }

    // 拦截 2: calculateCardDamage (计算对特定目标的数值，如指向敌人时)
    @SpirePatch(clz = AbstractCard.class, method = "calculateCardDamage")
    public static class CalculateCardDamagePatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractCard __instance, AbstractMonster mo) {
            if (MonsterCardContext.activeMonster == null) {
                return SpireReturn.Continue();
            }

            // --- 进入怪物逻辑 ---
            AbstractCardMonster source = MonsterCardContext.activeMonster;
            AbstractCreature target = mo; // 这里的 mo 其实是友军锁定的目标

            // 1. 先复用 applyPowers 的逻辑算出基础加成
            ApplyPowersPatch.Prefix(__instance);
            // 注意：上面这一步已经把 source(友军) 的力量算进去了
            // 下面只需要算 target(敌人) 的易伤等状态

            // 2. 针对目标的修正 (易伤 Vulnerable 等)
            if (target != null && target.powers != null) {
                float tmp = (float) __instance.damage;

                // atDamageReceive (易伤等)
                for (AbstractPower p : target.powers) {
                    if (p != null) { // 防空指针
                        tmp = p.atDamageReceive(tmp, __instance.damageTypeForTurn);
                    }
                }

                // atDamageFinalReceive
                for (AbstractPower p : target.powers) {
                    if (p != null) {
                        tmp = p.atDamageFinalReceive(tmp, __instance.damageTypeForTurn);
                    }
                }

                __instance.damage = (int) Math.floor(tmp);
                if (__instance.damage < 0)
                    __instance.damage = 0;
            }

            if (__instance.damage != __instance.baseDamage)
                __instance.isDamageModified = true;

            return SpireReturn.Return();
        }
    }
}