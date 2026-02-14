package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import SS.monster.AbstractCardMonster;
import SS.helper.MonsterCardContext;

public class ActionRedirectPatches {

    @SpirePatch(clz = GameActionManager.class, method = "addToBottom")
    public static class RedirectBottomPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(GameActionManager __instance, AbstractGameAction action) {
            return processAction(action, __instance);
        }
    }

    @SpirePatch(clz = GameActionManager.class, method = "addToTop")
    public static class RedirectTopPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(GameActionManager __instance, AbstractGameAction action) {
            return processAction(action, __instance);
        }
    }

    private static SpireReturn<Void> processAction(AbstractGameAction action, GameActionManager manager) {
        // 只有在【怪物卡牌上下文】中才拦截
        if (MonsterCardContext.activeMonster == null) {
            return SpireReturn.Continue();
        }

        AbstractCardMonster owner = MonsterCardContext.activeMonster;

        // ---------------------------------------------------------
        // 1. 拦截抽牌 (DrawCardAction) -> 改为怪物抽牌
        // ---------------------------------------------------------
        if (action instanceof DrawCardAction) {
            // DrawCardAction 内部写死了 player.draw()，我们无法修改 target
            // 所以必须直接【取消】这个动作，改为手动调用怪物的 draw

            // 获取抽牌数量
            int amount = action.amount;

            // 立即执行怪物的抽牌逻辑
            // 注意：这里我们不能 addToBottom，因为这本身就是在 addToBottom 里的 Patch
            // 直接执行逻辑可能会有时序问题，建议封装一个新的 Action 或者直接在这里调
            // 为了保持队列顺序，我们把它替换为一个“怪物抽牌动作”
            // 但因为 drawCard 只是数据操作，直接调用也是可以的，只要视觉上过得去
            for (int i = 0; i < amount; i++) {
                owner.drawCard();
            }

            // 【关键】阻止原动作入队 (SpireReturn.Return)
            return SpireReturn.Return();
        }

        // ---------------------------------------------------------
        // 2. 拦截回费 (GainEnergyAction) -> 改为怪物回费
        // ---------------------------------------------------------
        else if (action instanceof GainEnergyAction) {
            owner.energy += action.amount;
            // 可以在这里加个特效，比如 owner 头顶冒出 "+[E]"
            return SpireReturn.Return();
        }

        // ---------------------------------------------------------
        // 3. 修正伤害来源 (DamageAction)
        // ---------------------------------------------------------
        else if (action instanceof DamageAction) {
            try {
                java.lang.reflect.Field infoField = DamageAction.class.getDeclaredField("info");
                infoField.setAccessible(true);
                DamageInfo info = (DamageInfo) infoField.get(action);

                // 如果源头是玩家，改为怪物
                if (info.owner == AbstractDungeon.player || info.owner == null) {
                    info.owner = owner;
                    action.source = owner;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ---------------------------------------------------------
        // 4. 修正格挡目标 (GainBlockAction)
        // ---------------------------------------------------------
        else if (action instanceof GainBlockAction) {
            if (action.target == AbstractDungeon.player) {
                action.target = owner;
                action.source = owner;
            }
        }

        // ---------------------------------------------------------
        // 5. 修正能力施加 (ApplyPowerAction)
        // ---------------------------------------------------------
        else if (action instanceof ApplyPowerAction) {
            // 给自己贴 Buff (如燃烧)
            if (action.target == AbstractDungeon.player) {
                action.target = owner;
            }
            // 给敌人上 Debuff (源头修正)
            if (action.source == AbstractDungeon.player) {
                action.source = owner;
            }
        }

        // 继续执行原逻辑 (把修改后的 action 加入队列)
        return SpireReturn.Continue();
    }
}