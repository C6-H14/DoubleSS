package SS.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.AbstractCreature;

import SS.monster.AbstractCardMonster;

/**
 * 【修复 power 图标画两个 + 调参错位】
 *
 * 根因：AbstractCardMonster.render() 会调用 renderHealth(sb)，
 * 而 renderHealth 内部会再调一次原版的 renderPowerIcons（画一套 power 图标），
 * 同时 AbstractCardMonster 又画了自己那套 renderCustomPowerIcons —— 两套叠在一起，
 * 且坐标公式基准不同，所以默认就有细微错位、一调参数更明显。
 *
 * 这里对 AbstractCardMonster 实例直接跳过原版 renderPowerIcons，只保留自定义那套
 * （可参数化：powerOffsetX/Y、powerIconScale、powerTextScale）。普通敌人/玩家不受影响。
 */
@SpirePatch(clz = AbstractCreature.class, method = "renderPowerIcons")
public class AllyPowerRenderPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractCreature __instance, SpriteBatch sb, float x, float y) {
        if (__instance instanceof AbstractCardMonster) {
            // 卡牌怪物/友军：只用 renderCustomPowerIcons，跳过原版绘制
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }
}
