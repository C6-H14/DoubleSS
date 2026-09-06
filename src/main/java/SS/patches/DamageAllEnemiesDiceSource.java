package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;

import SS.stats.DiceAttribution;

/**
 * 给原版 DamageAllEnemiesAction 注入一个"骰子归属快照"字段（战斗统计用）。
 *
 * ImmolateDice（HitAll 分支）激发时 addToTop(new DamageAllEnemiesAction(..., THORNS, ...))，
 * 伤害在几帧后 update() 里对每只怪逐个 monster.damage()，届时卡牌栈里的出牌帧早已弹掉。
 * 把归属快照挂在 AOE action 上；DamageAllEnemiesAction.update 的
 * "Prefix 存静态上下文 / isDone 时 Postfix 清"夹住整段 monster.damage 调用，让
 * CombatStatsPatch.OnMonsterDamage 能把 AOE 伤害归属到骰子来源。
 *
 * EternalAttackDice 的 NextTurnDamagePower 回合开始的延迟 AOE 也走同一条路
 * （power 构造时携带快照，atStartOfTurn 挂到它创建的 DamageAllEnemiesAction 上）。
 *
 * 未赋值时 get() 返回 null（= 非骰子 AOE）。
 */
@SpirePatch(clz = DamageAllEnemiesAction.class, method = "<class>")
public class DamageAllEnemiesDiceSource {
    public static SpireField<DiceAttribution> diceRef = new SpireField<>(() -> null);
}
