package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.screens.mainMenu.SortHeaderButton;

import SS.interfaces.IEnvironmentCard;
import basemod.ReflectionHacks;

@SpirePatch(clz = UseCardAction.class, method = "update")
public class UseCardActionPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(UseCardAction __instance) {
        // 利用反射获取 UseCardAction 内部私有的 card 字段
        // 这极为安全，不改变任何公共 API
        try {
            java.lang.reflect.Field cardField = UseCardAction.class.getDeclaredField("card");
            cardField.setAccessible(true);
            AbstractCard card = (AbstractCard) cardField.get(__instance);

            // 如果打出的卡是环境卡，且动作已经执行到“准备清理卡牌”的阶段
            if (card instanceof IEnvironmentCard
                    && ((float) ReflectionHacks.getPrivate(__instance, UseCardAction.class, "duration")) == 0.15F) {
                // 1. 强制将 UseCardAction 设为已完成
                __instance.isDone = true;

                // 2. 阻止原版将其塞入弃牌堆/消耗堆的逻辑
                return SpireReturn.Return();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Continue();
    }
}