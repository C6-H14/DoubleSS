package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import SS.characters.AbstractSSCharacter;
import SS.stats.CardStats;

/**
 * 战斗统计系统的游戏侧钩子（全部 Postfix/Prefix，不修改游戏原逻辑）。
 *
 * 钩点签名均已对照 D:\StSmod\desktop-1.0 反编译源码核实：
 * - AbstractPlayer.useCard(AbstractCard, AbstractMonster, int):1404 —— 所有真实出牌的唯一汇聚点
 *   （手牌点击走这里；cardQueue 路径在 GameActionManager:296 也回到这里；本 mod 的
 *   PlayCardAction→NewQueueCardAction→cardQueue 同样汇入）
 * - AbstractMonster.damage(DamageInfo):631 —— 玩家对怪物伤害的最终结算（info.output 为最终值）
 * - AbstractPlayer.damage(DamageInfo):1433 —— 玩家受伤（用 HP 前后差，不受 IntangiblePlayer
 *   回写 info.output=1 影响）
 * - AbstractCreature.addBlock(int):447 —— 格挡获取（玩家/怪物共用，按 target 区分）
 * - AbstractRoom.endBattle():436 —— 战斗胜利收尾
 *
 * 战斗开始不走 patch：modcore.receiveOnBattleStart（BaseMod 订阅者）已覆盖三类房间
 * （三个房间类都覆写了 onPlayerEntry 且不调 super，patch 它不可靠）。
 * 每帧驱动与终局导出在 modcore.receivePostUpdate 调用 CardStats.update()。
 *
 * <h3>第二批新增钩子</h3>
 * - channelOrb（AbstractPlayer + AbstractSSCharacter 各一个；SS 系覆写了该方法且不调
 *   super，MTS 只 patch 声明方法，故必须两个都挂）：充能瞬间卡牌栈顶 = 充能牌 →
 *   标注骰子 source（遗物/环境产骰栈空 → 无归属桶）
 * - ApplyPowerAction 主构造器（6 参，所有重载都汇入它）Postfix：power→授予牌 映射
 *   （power 产骰回溯 source 用）、力量/敏捷直接授予累计、buff/debuff 台账。
 *   挂构造器而非 update()：update() 每帧执行、且首帧判定依赖 private 字段，
 *   构造器恰好一次、字段全部就绪。
 * - DamageAllEnemiesAction.update / GainBlockAction.update 前后钩：把挂在 action 上的
 *   骰子归属快照存入 CardStats 静态上下文，让内部 monster.damage / addBlock 的结算钩
 *   能读到（AOE 的 DamageInfo 是 action 内部 new 的，无法逐 info 挂 ref）。
 */
public class CombatStatsPatch {

    @SpirePatch(clz = AbstractPlayer.class, method = "useCard")
    public static class OnPlay {
        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance, AbstractCard c, AbstractMonster m, int energyOnUse) {
            CardStats.onPlay(c);
        }
    }

    @SpirePatch(clz = AbstractMonster.class, method = "damage")
    public static class OnMonsterDamage {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance, DamageInfo info) {
            if (info != null && info.owner == AbstractDungeon.player) {
                CardStats.onMonsterDamage(info, __instance);
            }
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "damage")
    public static class OnPlayerDamage {
        private static int hpBefore;

        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance, DamageInfo info) {
            hpBefore = __instance.currentHealth;
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, DamageInfo info) {
            int loss = hpBefore - __instance.currentHealth;
            if (loss > 0) {
                CardStats.onPlayerHpLoss(loss, info);
            }
        }
    }

    @SpirePatch(clz = AbstractCreature.class, method = "addBlock")
    public static class OnPlayerBlock {
        @SpirePostfixPatch
        public static void Postfix(AbstractCreature __instance, int blockAmount) {
            if (__instance == AbstractDungeon.player) {
                CardStats.onPlayerBlock(blockAmount);
            }
        }
    }

    @SpirePatch(clz = AbstractRoom.class, method = "endBattle")
    public static class OnCombatWin {
        @SpirePostfixPatch
        public static void Postfix(AbstractRoom __instance) {
            CardStats.onCombatWin();
        }
    }

    // ==================== 第二批：骰子 source / power 台账 ====================

    /** 充能完成（原版角色路径）：卡牌打出充能时栈顶 = 充能牌 → 标注骰子 source。 */
    @SpirePatch(clz = AbstractPlayer.class, method = "channelOrb")
    public static class OnChannelOrb {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractOrb orbToSet) {
            CardStats.onOrbChanneled(orbToSet);
        }
    }

    /** 充能完成（二硫键系路径）：AbstractSSCharacter 覆写了 channelOrb 且不调 super。 */
    @SpirePatch(clz = AbstractSSCharacter.class, method = "channelOrb")
    public static class OnChannelOrbSS {
        @SpirePostfixPatch
        public static void Postfix(AbstractSSCharacter __instance, AbstractOrb orbToSet) {
            CardStats.onOrbChanneled(orbToSet);
        }
    }

    /**
     * power 施加记录。挂在 update() 上（本 mod 已验证的 patch 模式，见 EvokeAllPatch；
     * 不用 <init> 构造器 patch——其参数约定无法在编译期验证，错位会运行时崩掉加 buff 的牌）。
     * update() 每帧执行，由 CardStats.onPowerAppliedFrame 内部按 power 实例去重，只记首帧。
     * powerToApply 是 private，CardStats 用缓存反射读取（失败只跳过统计，不影响游戏）。
     */
    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class OnPowerApplied {
        @SpirePostfixPatch
        public static void Postfix(ApplyPowerAction __instance) {
            CardStats.onPowerAppliedFrame(__instance);
        }
    }

    /**
     * 骰子 AOE 伤害上下文：DamageAllEnemiesAction.update() 在 isDone 帧内部对每只怪
     * 逐个 monster.damage()（DamageInfo 是 action 内部 new 的），用前后钩把挂在
     * action 上的归属快照存入 CardStats 静态上下文，结算钩读到后清除。
     */
    @SpirePatch(clz = DamageAllEnemiesAction.class, method = "update")
    public static class OnAoeDamage {
        @SpirePrefixPatch
        public static void Prefix(DamageAllEnemiesAction __init) {
            CardStats.setAoeDice(DamageAllEnemiesDiceSource.diceRef.get(__init));
        }

        @SpirePostfixPatch
        public static void Postfix(DamageAllEnemiesAction __init) {
            CardStats.setAoeDice(null);
        }
    }

    /**
     * 骰子格挡上下文：GainBlockAction.update() 首帧内部 target.addBlock()，
     * 同理用前后钩把归属快照存入静态上下文。
     */
    @SpirePatch(clz = GainBlockAction.class, method = "update")
    public static class OnBlockGain {
        @SpirePrefixPatch
        public static void Prefix(GainBlockAction __init) {
            CardStats.setBlockDice(GainBlockDiceSource.diceRef.get(__init));
        }

        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __init) {
            CardStats.setBlockDice(null);
        }
    }
}
