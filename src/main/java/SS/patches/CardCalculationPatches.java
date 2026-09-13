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
import SS.monster.ally.AbstractAlly; // 【引入友军基类】
import SS.interfaces.IAllyDamageModifier; // 【引入指挥能力接口】
import SS.helper.MonsterCardContext;

public class CardCalculationPatches {

    // =========================================================================
    // 拦截 1: applyPowers (计算常规/无目标时的数值，以及 AOE multiDamage 数组)
    // =========================================================================
    @SpirePatch(clz = AbstractCard.class, method = "applyPowers")
    public static class ApplyPowersPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractCard __instance) {
            if (MonsterCardContext.activeMonster == null) {
                return SpireReturn.Continue();
            }

            AbstractCardMonster m = MonsterCardContext.activeMonster;

            // 1. 重置数值
            __instance.isDamageModified = false;
            __instance.damage = __instance.baseDamage;
            __instance.isBlockModified = false;
            __instance.block = __instance.baseBlock;
            __instance.magicNumber = __instance.baseMagicNumber;
            __instance.isMagicNumberModified = false;

            // 2. 单体基础伤害计算 (友军自身的力量/活力等)
            if (m.powers != null) {
                for (AbstractPower p : m.powers) {
                    __instance.damage = (int) p.atDamageGive(__instance.damage, __instance.damageTypeForTurn);
                }
                for (AbstractPower p : m.powers) {
                    __instance.damage = (int) p.atDamageFinalGive(__instance.damage, __instance.damageTypeForTurn);
                }
            }

            // 【新增】：玩家身上的全局加算指挥能力 (例如：你的友军伤害+3)
            if (m instanceof AbstractAlly && AbstractDungeon.player != null) {
                for (AbstractPower p : AbstractDungeon.player.powers) {
                    if (p instanceof IAllyDamageModifier) {
                        __instance.damage = (int) ((IAllyDamageModifier) p).onAllyModifyDamageGive(
                                (AbstractAlly) m, null, (float) __instance.damage, __instance.damageTypeForTurn);
                    }
                }
            }

            // 3. 【AOE 伤害计算】依据场上怪物逐个计算
            ArrayList<AbstractMonster> monsters = AbstractDungeon.getCurrRoom().monsters.monsters;
            float[] tmp = new float[monsters.size()];
            __instance.multiDamage = new int[monsters.size()];

            for (int i = 0; i < tmp.length; i++) {
                AbstractMonster mo = monsters.get(i);
                tmp[i] = (float) __instance.baseDamage;

                // A. 施法者(友军自身)的加成
                if (m.powers != null) {
                    for (AbstractPower p : m.powers) {
                        tmp[i] = p.atDamageGive(tmp[i], __instance.damageTypeForTurn);
                    }
                }

                // 【新增】：AOE - 玩家身上的指挥能力 (加算阶段)
                if (m instanceof AbstractAlly && AbstractDungeon.player != null) {
                    for (AbstractPower p : AbstractDungeon.player.powers) {
                        if (p instanceof IAllyDamageModifier) {
                            tmp[i] = ((IAllyDamageModifier) p).onAllyModifyDamageGive(
                                    (AbstractAlly) m, mo, tmp[i], __instance.damageTypeForTurn);
                        }
                    }
                }

                if (m.powers != null) {
                    for (AbstractPower p : m.powers) {
                        tmp[i] = p.atDamageFinalGive(tmp[i], __instance.damageTypeForTurn);
                    }
                }

                // B. 受击者(敌人)身上的加成 (易伤/无实体等)
                if (mo != null) {
                    for (AbstractPower p : mo.powers) {
                        tmp[i] = p.atDamageReceive(tmp[i], __instance.damageTypeForTurn);
                    }
                    for (AbstractPower p : mo.powers) {
                        tmp[i] = p.atDamageFinalReceive(tmp[i], __instance.damageTypeForTurn);
                    }
                }

                // 【新增】：AOE - 玩家身上的指挥能力 (乘算阶段，双重弱化翻倍在此生效)
                if (m instanceof AbstractAlly && AbstractDungeon.player != null) {
                    for (AbstractPower p : AbstractDungeon.player.powers) {
                        if (p instanceof IAllyDamageModifier) {
                            tmp[i] = ((IAllyDamageModifier) p).onAllyModifyDamageFinal(
                                    (AbstractAlly) m, mo, tmp[i], __instance.damageTypeForTurn);
                        }
                    }
                }

                // 取整存入
                __instance.multiDamage[i] = (int) Math.floor(tmp[i]);
                if (__instance.multiDamage[i] < 0)
                    __instance.multiDamage[i] = 0;
            }

            // 标记 AOE 伤害是否被修改
            if (__instance.multiDamage.length > 0 && __instance.baseDamage != __instance.multiDamage[0]) {
                __instance.isDamageModified = true;
            }

            // 4. 计算格挡
            float tmpBlock = (float) __instance.baseBlock;
            if (m.powers != null) {
                for (AbstractPower p : m.powers) {
                    tmpBlock = p.modifyBlock(tmpBlock);
                }
            }
            __instance.block = (int) Math.floor(tmpBlock);
            if (__instance.block < 0)
                __instance.block = 0;

            // 5. 设置修改标记
            if (__instance.damage != __instance.baseDamage)
                __instance.isDamageModified = true;
            if (__instance.block != __instance.baseBlock)
                __instance.isBlockModified = true;

            return SpireReturn.Return();
        }
    }

    // =========================================================================
    // 拦截 2: calculateCardDamage (计算指向特定敌人时的最终伤害)
    // =========================================================================
    @SpirePatch(clz = AbstractCard.class, method = "calculateCardDamage")
    public static class CalculateCardDamagePatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractCard __instance, AbstractMonster mo) {
            if (MonsterCardContext.activeMonster == null) {
                return SpireReturn.Continue();
            }

            AbstractCardMonster source = MonsterCardContext.activeMonster;
            AbstractCreature target = mo;

            // 1. 先复用 applyPowers 的逻辑算出基础加成
            ApplyPowersPatch.Prefix(__instance);
            float tmp = (float) __instance.damage;

            // 【新增】：单体 - 玩家身上的指挥能力 (针对特定目标的加算)
            if (source instanceof AbstractAlly && AbstractDungeon.player != null) {
                for (AbstractPower p : AbstractDungeon.player.powers) {
                    if (p instanceof IAllyDamageModifier) {
                        tmp = ((IAllyDamageModifier) p).onAllyModifyDamageGive(
                                (AbstractAlly) source, target, tmp, __instance.damageTypeForTurn);
                    }
                }
            }

            // 2. 针对目标敌人身上的修正 (如易伤 Vulnerable)
            if (target != null && target.powers != null) {
                for (AbstractPower p : target.powers) {
                    if (p != null) {
                        tmp = p.atDamageReceive(tmp, __instance.damageTypeForTurn);
                    }
                }

                for (AbstractPower p : target.powers) {
                    if (p != null) {
                        tmp = p.atDamageFinalReceive(tmp, __instance.damageTypeForTurn);
                    }
                }
            }

            // =============================================================
            // 【核心新增】：单体 - 玩家身上的指挥能力 (乘算阶段，双重弱化翻倍在此生效)
            // =============================================================
            if (source instanceof AbstractAlly && AbstractDungeon.player != null) {
                for (AbstractPower p : AbstractDungeon.player.powers) {
                    if (p instanceof IAllyDamageModifier) {
                        tmp = ((IAllyDamageModifier) p).onAllyModifyDamageFinal(
                                (AbstractAlly) source, target, tmp, __instance.damageTypeForTurn);
                    }
                }
            }

            // 结算最终伤害
            __instance.damage = (int) Math.floor(tmp);
            if (__instance.damage < 0)
                __instance.damage = 0;

            if (__instance.damage != __instance.baseDamage)
                __instance.isDamageModified = true;

            return SpireReturn.Return();
        }
    }
}