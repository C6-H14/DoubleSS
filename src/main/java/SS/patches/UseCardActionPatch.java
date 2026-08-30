package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import SS.interfaces.IEnvironmentCard;
import basemod.ReflectionHacks;

@SpirePatch(clz = UseCardAction.class, method = "update")
public class UseCardActionPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(UseCardAction __instance) {
        try {
            // 1. 读取私有字段 targetCard
            AbstractCard targetCard = ReflectionHacks.getPrivate(__instance, UseCardAction.class, "targetCard");

            // 2. 通过 ReflectionHacks 安全读取父类的 protected 字段 duration
            float duration = ReflectionHacks.getPrivate(__instance, AbstractGameAction.class, "duration");

            if (targetCard instanceof IEnvironmentCard && duration == 0.15F) {
                // 触发使用后的能力监听
                for (com.megacrit.cardcrawl.powers.AbstractPower p : AbstractDungeon.player.powers) {
                    if (!targetCard.dontTriggerOnUseCard) {
                        p.onAfterUseCard(targetCard, __instance);
                    }
                }
                for (com.megacrit.cardcrawl.monsters.AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    for (com.megacrit.cardcrawl.powers.AbstractPower p : m.powers) {
                        if (!targetCard.dontTriggerOnUseCard) {
                            p.onAfterUseCard(targetCard, __instance);
                        }
                    }
                }

                // 重置卡牌状态
                targetCard.freeToPlayOnce = false;
                targetCard.isInAutoplay = false;
                AbstractDungeon.player.cardInUse = null;

                // 标记完成，拦截后续进入弃牌堆/消耗堆的逻辑
                __instance.isDone = true;
                return SpireReturn.Return();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Continue();
    }
}