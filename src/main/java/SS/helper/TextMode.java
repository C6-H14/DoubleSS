package SS.helper;

import java.io.IOException;
import java.util.Properties;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;

/**
 * 文本详略模式：简洁（默认） / 详细。
 *
 * <p>简洁模式下卡牌的 EXTENDED_DESCRIPTION 与教程文案会换成一套大幅精简的短文案
 * （内容在 {@code ZHS/cards_concise.json} 与 {@code ZHS/tutorial_concise.json}，
 * 与正式文案同 key，缺条目时自动回退到正式文案）；详细模式即目前这个版本的文字。
 *
 * <p>开关落在 {@code SpireConfig("Double", "Common")} 的 {@code TextDetailed}，
 * 与 CustomPackEnabled 等其它偏好放在同一个文件里。默认 false（简洁）。
 */
public class TextMode {
    private static final String CONFIG_NAME = "Double";
    private static final String CONFIG_FILE = "Common";
    private static final String KEY_DETAILED = "TextDetailed";

    /** true = 详细模式。默认简洁，所以字段初值是 false。 */
    private static boolean detailed = false;
    private static boolean loaded = false;

    /** 是否详细模式。第一次调用会惰性读盘（早于任何卡牌构造，卡池构建期即可安全使用）。 */
    public static boolean isDetailed() {
        ensureLoaded();
        return detailed;
    }

    /** 是否简洁模式。卡牌/教程取文案时用这个判断，比 {@code !isDetailed()} 读起来清楚。 */
    public static boolean isConcise() {
        return !isDetailed();
    }

    /** 由设置面板调用：写入内存并落盘。 */
    public static void setDetailed(boolean value) {
        detailed = value;
        loaded = true;
        try {
            SpireConfig cfg = new SpireConfig(CONFIG_NAME, CONFIG_FILE);
            cfg.setString(KEY_DETAILED, String.valueOf(value));
            cfg.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true; // 先置位，避免读盘异常时反复重试
        try {
            Properties defaults = new Properties();
            defaults.setProperty(KEY_DETAILED, "false");
            SpireConfig cfg = new SpireConfig(CONFIG_NAME, CONFIG_FILE, defaults);
            detailed = "true".equals(cfg.getString(KEY_DETAILED));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
