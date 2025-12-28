package SS.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import SS.monster.ally.AllyManager;

public class AllyHookPatch {
    @SpirePatch(clz = AbstractPlayer.class, method = "render")
    public static class RenderPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, SpriteBatch sb) {
            if (AbstractDungeon.getCurrRoom() != null
                    && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
                // 渲染友军
                AllyManager.render(sb);
            }
        }
    }// 2. 逻辑更新

    @SpirePatch(clz = AbstractRoom.class, method = "update")
    public static class UpdatePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractRoom __instance) {
            if (__instance.phase == AbstractRoom.RoomPhase.COMBAT) {
                AllyManager.update();
            }
        }
    }

    // 3. 战斗结束清理
    @SpirePatch(clz = AbstractDungeon.class, method = "resetPlayer")
    public static class ClearPatch {
        @SpirePostfixPatch
        public static void Postfix() {
            AllyManager.clear();
        }
    }

    // 4. 回合开始触发
    @SpirePatch(clz = AbstractPlayer.class, method = "applyStartOfTurnRelics")
    public static class StartTurnPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
                AllyManager.onPlayerStartTurn();
            }
        }
    }

    // 5. 回合结束触发
    @SpirePatch(clz = com.megacrit.cardcrawl.actions.GameActionManager.class, method = "callEndOfTurnActions")
    public static class EndTurnPatch {
        @SpirePostfixPatch
        public static void Postfix(Object __instance) {
            AllyManager.onPlayerEndTurn();
        }
    }

    // 监听 1: 施加能力 (例如: 获得力量、施加易伤)
    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class ApplyPowerUpdate {
        @SpirePostfixPatch
        public static void Postfix(ApplyPowerAction __instance) {
            // 当动作执行完毕时 (isDone == true)
            if (__instance.isDone) {
                AllyManager.refreshIntents();
            }
        }
    }

    // 监听 2: 移除能力 (例如: 易伤过期、被净化)
    @SpirePatch(clz = RemoveSpecificPowerAction.class, method = "update")
    public static class RemovePowerUpdate {
        @SpirePostfixPatch
        public static void Postfix(RemoveSpecificPowerAction __instance) {
            if (__instance.isDone) {
                AllyManager.refreshIntents();
            }
        }
    }
}
