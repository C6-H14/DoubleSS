package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.DamageInfo;

import SS.stats.DiceAttribution;

/**
 * 给原版 DamageInfo 注入一个"骰子归属快照"字段（战斗统计用）。
 *
 * 单目标骰子伤害走 DiceDamageEnemyAction → DamageInfo → DamageAction → monster.damage，
 * 伤害在几帧后才结算，届时卡牌栈里的出牌帧早已弹掉，无法靠栈顶归属。
 * 因此在 DiceDamageEnemyAction.update 创建 DamageInfo 时把归属快照挂到
 * DamageInfo 上，结算钩子（CombatStatsPatch.OnMonsterDamage）读取本字段：
 * 既按快照累计骰子统计行，又按 sources 均分给来源牌。
 *
 * 未赋值时 get() 返回 null（= 非骰子伤害，走原有的栈顶/无归属逻辑）。
 * 与本 mod 已有的 AllyDamagePatch（同为 DamageInfo 的 <class> 字段注入）并存。
 */
@SpirePatch(clz = DamageInfo.class, method = "<class>")
public class DamageInfoDiceSource {
    public static SpireField<DiceAttribution> diceRef = new SpireField<>(() -> null);
}
