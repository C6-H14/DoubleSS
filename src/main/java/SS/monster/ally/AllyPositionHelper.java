package SS.monster.ally;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * 友军站位槽位工具（纯数学，无副作用；真正落地位置由 {@link AbstractAlly#relocateToSlot(int)} 完成）。
 *
 * <p>
 * 坐标约定（全部分辨率自适应，禁止任何裸绝对像素）：
 * <ul>
 * <li>X 基准：{@code AbstractDungeon.player.drawX}（玩家立绘脚底中心）</li>
 * <li>Y 基准：{@code AbstractDungeon.floorY}（地面基准线，340·yScale）</li>
 * <li>所有偏移严格 × {@code Settings.scale}（1920 基准的设计像素）</li>
 * </ul>
 *
 * <p>
 * 槽位表（1920 基准设计像素，原点左下、Y 向上）：
 * 
 * <pre>
 *   Slot 0 首发：玩家右前方贴地   (+170,  -15)
 *   Slot 1 后卫：玩家左后方       (-160,  +45)
 *   Slot 2      右侧后排（玩家与敌人之间） (+300, +90)
 *   Slot 3      左侧前排（玩家身后偏前）   (-260,  -40)
 *   Slot 4      右侧前排         (+430,  -45)
 * </pre>
 * 
 * 敌人站位在 0.75·W 及更右区域，最外槽位与其仍有充足间距；4:3 下最左槽位不出屏。
 */
public final class AllyPositionHelper {

    /** 槽位数（创世记阵法维持的魂火上限）。 */
    public static final int SLOT_COUNT = 5;

    // 设计像素（未乘 scale）：{ offsetX, offsetY }
    private static final float[][] SLOTS = {
            { 320.0F, 300.0F }, // 0 右侧后排
            { -250.0F, 400.0F }, // 1 玩家左后方
            { 430.0F, 130.0F }, // 2 玩家右前方
            { -270.0F, 0.0F }, // 3 左侧前排
            { 280.0F, 30.0F } // 4 右侧前排
    };

    private AllyPositionHelper() {
    }

    /** 越界钳制到 [0, SLOT_COUNT)。 */
    public static int clampSlot(int index) {
        if (index < 0)
            return 0;
        if (index >= SLOT_COUNT)
            return SLOT_COUNT - 1;
        return index;
    }

    /** 该槽位的立绘 drawX：player.drawX + 偏移·scale。 */
    public static float slotDrawX(int index) {
        return AbstractDungeon.player.drawX + SLOTS[clampSlot(index)][0] * Settings.scale;
    }

    /** 该槽位的立绘 drawY（脚底）：floorY + 偏移·scale。 */
    public static float slotDrawY(int index) {
        return AbstractDungeon.floorY + SLOTS[clampSlot(index)][1] * Settings.scale;
    }
}
