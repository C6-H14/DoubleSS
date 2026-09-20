package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;

/**
 * 【Lagavulin 头部图层丢失修复】
 *
 * 机制（2026-09-16 探针数据 + skeleton.json 时间轴确认，探针已删）：
 * 睡眠中的 Lagavulin 受到首次有效伤害时，damage() 只把 ChangeStateAction(OPEN) 排到
 * 动作队列尾部，不切动画；OPEN 执行后播 Coming_out（1 秒）——它是唯一会挂上
 * Shell2（头身，t=0.5666）和 EYE（眼，t=0.7666）附件的动画，之后切 Idle_2 循环。
 * 而伤害的"Hit 硬切分支"（isOutTriggered 后再掉血 → setAnimation(0,"Hit")）会
 * freeAll 当前轨道条目：若它发生在 Coming_out 播放期间，Coming_out 被整个丢弃，
 * Shell2/EYE 的 attachment 帧永远没机会执行，Idle_2 对这两个 slot 又没有帧
 * → 附件永久丢失，只剩爪子。
 *
 * 本 mod 的骰子爆发（回合末大量 DiceDamageEnemyAction）+ 魂火跟班回合末出《圣约》
 * 恰好把二次伤害打进这个 1 秒窗口，所以骰子打醒时必现。
 *
 * 修法（只动动画轨道，不碰伤害数值/状态逻辑/骰子侧）：
 * Prefix 记录 track0 当前动画名与进度；Postfix 若发现「伤害前 = Coming_out、
 * 伤害后被硬切 = Hit」，把轨道恢复到伤害前的 Coming_out 进度并重排 Idle_2。
 * Spine 的 timeline apply 发生在下一帧怪物 update，Postfix 时 Hit 条目尚未被消费，
 * 因此恢复是无损的（零视觉闪烁）。
 *
 * 安全性：原版流程中 Coming_out 播放期间没有二次伤害（单段伤害打醒、OPEN 紧跟执行），
 * 本修复条件在原版里不触发；即使触发也只是跳过一次受击闪动动画，不改任何战斗逻辑。
 */
public class LagavulinWakeRepairPatch {
    private static final String COMING_OUT = "Coming_out";
    private static final String HIT = "Hit";
    private static final String IDLE_2 = "Idle_2";

    // 单次 damage 调用内的传递数据（伤害结算单线程、不重入，参照 CombatStatsPatch 的静态字段模式）
    private static String prevAnimName;
    private static float prevAnimTime;

    @SpirePatch(clz = Lagavulin.class, method = "damage")
    public static class OnDamage {

        @SpirePrefixPatch
        public static void Prefix(Lagavulin __instance, DamageInfo info) {
            prevAnimName = null;
            prevAnimTime = 0.0f;
            AnimationState st = __instance.state;
            AnimationState.TrackEntry cur = (st == null) ? null : st.getCurrent(0);
            if (cur != null && cur.getAnimation() != null) {
                prevAnimName = cur.getAnimation().getName();
                prevAnimTime = cur.getTime();
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Lagavulin __instance, DamageInfo info) {
            if (!COMING_OUT.equals(prevAnimName)) {
                return;
            }
            AnimationState st = __instance.state;
            AnimationState.TrackEntry cur = (st == null) ? null : st.getCurrent(0);
            if (cur == null || cur.getAnimation() == null || !HIT.equals(cur.getAnimation().getName())) {
                return;
            }
            // 伤害在 Coming_out 揭示头部的 1 秒窗口内把轨道硬切走了 → 恢复进度。
            // setAnimation 会 freeAll 旧条目（含 Hit 及其排的 Idle_2），需重排 Idle_2
            // （delay=0 → 按 stateData 默认 mix 0.2 收尾，与原版 changeState("OPEN") 的排法一致）。
            AnimationState.TrackEntry e = st.setAnimation(0, COMING_OUT, false);
            e.setTime(prevAnimTime);
            st.addAnimation(0, IDLE_2, true, 0.0f);
            System.out.println("[LagFix] 拦截了 Coming_out 窗口内的 Hit 硬切，动画轨道已恢复 t="
                    + String.format("%.2f", prevAnimTime));
        }
    }
}
