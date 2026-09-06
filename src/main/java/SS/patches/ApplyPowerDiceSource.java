package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;

import SS.Dice.AbstractDice;

/**
 * 给原版 ApplyPowerAction 注入一个"骰子"字段（战斗统计用）。
 *
 * 骰子在回合末激发时施加 power（WitherDice→流血、AttackHaoDice→中毒、
 * EternalAttack/DefendDice→持命/延迟伤害格挡），此时卡牌栈已空，无法靠栈顶归属。
 * 各骰子 myEvoke 构造 ApplyPowerAction 后把 this 挂到本字段，
 * 统计钩子（CardStats.onPowerAppliedFrame）读取：该 power 的"来源牌"取
 * dice.sources（充能瞬间标注的打出牌），台账的"来源牌"列由此精确到具体骰子。
 *
 * 未赋值时 get() 返回 null（= 普通卡牌/环境施加，走栈顶归属）。
 * 与本 mod 已有的 AllyDamagePatch（同为 <class> 字段注入）并存。
 */
@SpirePatch(clz = ApplyPowerAction.class, method = "<class>")
public class ApplyPowerDiceSource {
    public static SpireField<AbstractDice> diceRef = new SpireField<>(() -> null);
}
