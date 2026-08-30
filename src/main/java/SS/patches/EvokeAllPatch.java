package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.defect.TriggerEndOfTurnOrbsAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

import SS.Dice.AbstractDice;
import SS.Dice.EmptyDiceSlot;
import SS.characters.AbstractSSCharacter;

/**
 * 回合结束自动激发的分流补丁（挂在原版 TriggerEndOfTurnOrbsAction 上）。
 *
 * 原版流程（任意角色，1.0 通用）：update() 遍历 player.orbs 调每个球的 onEndOfTurn()
 * （原版四球的 passive 就发生在这里：Dark 蓄能 / Frost 给格挡 / Lightning 被动攻击）。
 *
 * 设计规则：
 * - 骰子（AbstractDice）：任何角色持有都在回合结束时自动激发（给 Defect 也能用）。
 * - 原版充能球（非骰子）：仅当玩家是二硫键系时自动激发；其他角色保持原版行为。
 * - 二硫键系回合结束：抑制全部 passive（Prefix 直接跳过原版 onEndOfTurn 循环），
 *   只走"激发 + 清槽"。
 * - 清槽时空槽类型跟随主人：二硫键系 → EmptyDiceSlot；其他角色 → 原版 EmptyOrbSlot，
 *   避免把骰子空槽塞进 Defect 导致原版 instanceof EmptyOrbSlot 判断失灵。
 *
 * 【Spire 语义注意】Prefix 返回 SpireReturn.Return() 时，原方法与 Postfix 都会被跳过。
 * 因此二硫键系分支必须在 Prefix 内完成激发（随后 Return 跳过 passive 循环与 Postfix）；
 * 非二硫键系分支 Prefix 返回 Continue()，让原版 passive 跑完后由 Postfix 激发骰子。
 */
public class EvokeAllPatch {
    @SpirePatch(clz = TriggerEndOfTurnOrbsAction.class, method = "update")
    public static class updatePatch {

        // 二硫键系回合：跳过原版 passive 循环（同时跳过 Postfix），在 Prefix 内直接激发全部球
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(TriggerEndOfTurnOrbsAction __init) {
            if (AbstractDungeon.player instanceof AbstractSSCharacter) {
                doEvoke(AbstractDungeon.player, true);
                // 原方法体被跳过，其末尾的 this.isDone = true 不会执行；
                // 若不手动置位，GameActionManager 会永远停在这个 action 上（卡死）。
                __init.isDone = true;
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }

        // 仅当 Prefix 返回 Continue()（即非二硫键系）时才会执行到这里
        @SpirePostfixPatch
        public static void Postfix(TriggerEndOfTurnOrbsAction __init) {
            if (AbstractDungeon.player instanceof AbstractSSCharacter) {
                return; // 防御：二硫键系已在 Prefix 处理
            }
            doEvoke(AbstractDungeon.player, false);
        }

        /**
         * 激发并清槽。
         *
         * @param player 当前玩家
         * @param ss     true = 二硫键系（激发所有球，空槽用 EmptyDiceSlot）；
         *               false = 其他角色（只激发骰子，原版球保持 passive 不动，空槽用 EmptyOrbSlot）
         */
        private static void doEvoke(AbstractPlayer player, boolean ss) {
            if (player.orbs.isEmpty()) {
                return;
            }
            for (int i = 0; i < player.orbs.size(); i++) {
                AbstractOrb orb = player.orbs.get(i);
                if (orb instanceof EmptyOrbSlot) {
                    continue; // EmptyDiceSlot 是它的子类，一并跳过
                }
                if (!ss && !(orb instanceof AbstractDice)) {
                    continue; // 非二硫键系：只激发骰子
                }
                orb.triggerEvokeAnimation();
                orb.onEvoke();
                AbstractOrb empty = ss ? new EmptyDiceSlot(orb.cX, orb.cY)
                        : new EmptyOrbSlot(orb.cX, orb.cY);
                player.orbs.set(i, empty);
                empty.setSlot(i, player.maxOrbs);
            }
        }
    }
}
