package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;

import SS.stats.DiceAttribution;

/**
 * 给原版 GainBlockAction 注入一个"骰子归属快照"字段（战斗统计用）。
 *
 * 格挡骰子（DefendDice/DefendHaoDice/IronwaveDice 的格挡部分）在激发时
 * addToBottom(new GainBlockAction(...))，真正加格挡发生在几帧后的 update()。
 * 激发时卡牌栈里的出牌帧早已弹掉，无法靠栈顶归属，因此把归属快照挂在
 * GainBlockAction 上；GainBlockAction.update 的前后钩（Prefix 存静态上下文
 * CardStats.blockDice / Postfix 清）夹住 addBlock 调用，让
 * CombatStatsPatch.OnPlayerBlock 能把格挡归属到骰子来源。
 *
 * EternalDefendDice 的 NextTurnBlockPower 回合开始加的格挡也走同一条路
 * （power 构造时携带快照，atStartOfTurn 挂到 GainBlockAction 上）。
 *
 * 未赋值时 get() 返回 null（= 非骰子格挡，走原有的栈顶/无归属逻辑）。
 */
@SpirePatch(clz = GainBlockAction.class, method = "<class>")
public class GainBlockDiceSource {
    public static SpireField<DiceAttribution> diceRef = new SpireField<>(() -> null);
}
