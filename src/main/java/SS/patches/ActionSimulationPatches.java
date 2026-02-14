package SS.patches;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class ActionSimulationPatches {

    // 全局模拟开关
    public static boolean isSimulating = false;

    // =================================================================
    // 模拟数据容器 (暂存拦截到的效果)
    // =================================================================
    public static List<AbstractPower> virtualPowers = new ArrayList<>(); // 拦截到的 Buff (力量/敏捷)
    public static int virtualBlock = 0; // 拦截到的直接格挡
    public static int virtualEnergy = 0; // 拦截到的能量 (支持《加速》等卡)
    public static boolean virtualDrawFound = false; // 是否拦截到抽牌

    // 清理状态 (每次模拟开始前调用)
    public static void clear() {
        isSimulating = false;
        virtualPowers.clear();
        virtualBlock = 0;
        virtualEnergy = 0;
        virtualDrawFound = false;
    }

    // =================================================================
    // 补丁：拦截 addToBottom
    // =================================================================
    @SpirePatch(clz = GameActionManager.class, method = "addToBottom")
    public static class InterceptBottomPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(GameActionManager __instance, AbstractGameAction action) {
            if (isSimulating) {
                analyzeAction(action); // 解析动作
                return SpireReturn.Return(); // 【核心】阻止动作进入真实队列
            }
            return SpireReturn.Continue();
        }
    }

    // =================================================================
    // 补丁：拦截 addToTop
    // =================================================================
    @SpirePatch(clz = GameActionManager.class, method = "addToTop")
    public static class InterceptTopPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(GameActionManager __instance, AbstractGameAction action) {
            if (isSimulating) {
                analyzeAction(action);
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }

    // =================================================================
    // 动作解析器 (核心逻辑)
    // =================================================================
    private static void analyzeAction(AbstractGameAction action) {
        // 1. 捕获能力给予 (ApplyPowerAction)
        if (action instanceof ApplyPowerAction) {
            try {
                // 反射获取私有字段 powerToApply
                Field f = ApplyPowerAction.class.getDeclaredField("powerToApply");
                f.setAccessible(true);
                AbstractPower power = (AbstractPower) f.get(action);

                if (power != null) {
                    // 我们主要关注影响数值的 Power
                    if (power instanceof StrengthPower || power instanceof DexterityPower) {
                        // 如果列表中已经有同类 Power，增加层数；否则添加
                        boolean merged = false;
                        for (AbstractPower existing : virtualPowers) {
                            if (existing.ID.equals(power.ID)) {
                                existing.amount += power.amount;
                                merged = true;
                                break;
                            }
                        }
                        if (!merged) {
                            virtualPowers.add(power);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. 捕获直接格挡 (GainBlockAction)
        else if (action instanceof GainBlockAction) {
            virtualBlock += action.amount;
        }

        // 3. 捕获抽牌 (DrawCardAction)
        else if (action instanceof DrawCardAction) {
            virtualDrawFound = true;
        }

        // 4. 捕获能量 (GainEnergyAction)
        else if (action instanceof GainEnergyAction) {
            virtualEnergy += action.amount;
        }
    }
}