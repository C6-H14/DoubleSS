package SS.helper;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import basemod.BaseMod;
import basemod.ModLabel;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;

/**
 * 二硫键的 BaseMod 模组设置面板（主菜单 → 模组 → 二硫键 的小徽章图标）。
 *
 * <p>目前只有一个开关：文本详略模式。默认简洁，勾上后变详细（即旧版全文案）。
 * 文案改动需要重开一局才能完整生效：卡牌描述在构造时就会算一次，
 * 已经在手里的实例不会因为改设置而立刻重刷（设置面板只负责存盘 + 置内存标志）。
 */
public class DoubleConfigPanel {
    private static final float X = 350.0F;
    private static final float Y = 700.0F;
    private static final float SPACING = 50.0F;

    public static void register() {
        ModPanel panel = new ModPanel();
        float y = Y;

        ModLabel title = new ModLabel("二硫键 DoubleSS", X, y, panel, (label) -> {
        });
        panel.addUIElement(title);
        y -= SPACING;

        ModLabel hint = new ModLabel("文本详略模式（改动后重开一局才完整生效）", X, y,
                new Color(0.7F, 0.7F, 0.7F, 1.0F), FontHelper.tipBodyFont, panel, (label) -> {
                });
        panel.addUIElement(hint);
        y -= SPACING;

        ModLabeledToggleButton detailed = new ModLabeledToggleButton(
                "详细模式",
                "关闭时为简洁模式：卡牌描述与教程换成精简短文案。",
                X, y, Color.WHITE, FontHelper.buttonLabelFont,
                TextMode.isDetailed(), panel,
                (label) -> {
                },
                (button) -> {
                    TextMode.setDetailed(button.enabled);
                });
        panel.addUIElement(detailed);

        Texture badge = ImageMaster.loadImage("img/UI/badge.png");
        BaseMod.registerModBadge(badge, "DoubleSS", "C6H14", "二硫键 - 自定义角色与卡包", panel);
    }
}
