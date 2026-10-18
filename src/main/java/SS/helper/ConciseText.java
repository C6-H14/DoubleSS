package SS.helper;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 简洁模式文本表。
 *
 * <p>两份覆盖表，key 与正式文案完全一致，只在简洁模式下生效：
 * <ul>
 *   <li>{@code localization/ZHS/cards_concise.json} —— 覆盖卡牌的
 *       DESCRIPTION / UPGRADE_DESCRIPTION / EXTENDED_DESCRIPTION；</li>
 *   <li>{@code localization/ZHS/tutorial_concise.json} —— 覆盖教程的 TEXT 页数组。</li>
 * </ul>
 *
 * <p><b>回退规则：</b>某个 key 没写、或某个字段没写，就返回 null，由调用方原样用正式文案。
 * 所以文件可以只写一部分条目，甚至留空 —— 空文件 = 全部走详细文案，不会报错。
 * 现在是空壳，内容由作者后续填充。
 *
 * <p>加载时机：卡池构建（{@code modcore.LoadData}）会读到卡牌文本，所以必须在那之前就绪。
 * 因此这里用 {@code Gdx.files.internal} 惰性加载 + 静态缓存，第一次访问即读盘。
 */
public class ConciseText {
    private static final String CARD_PATH = "localization/ZHS/cards_concise.json";
    private static final String TUTORIAL_PATH = "localization/ZHS/tutorial_concise.json";

    private static Map<String, JsonObject> cards = null;
    private static Map<String, String[]> tutorials = null;

    /** 简洁版卡牌字段；没有对应条目时返回 null。field 取 "DESCRIPTION" 等。 */
    public static String cardText(String id, String field) {
        ensureCards();
        JsonObject o = cards.get(id);
        if (o == null || !o.has(field)) {
            return null;
        }
        JsonElement e = o.get(field);
        return e.isJsonNull() ? null : e.getAsString();
    }

    /** 简洁版卡牌 EXTENDED_DESCRIPTION 数组；没有对应条目时返回 null。 */
    public static String[] cardExtended(String id) {
        ensureCards();
        JsonObject o = cards.get(id);
        if (o == null || !o.has("EXTENDED_DESCRIPTION")) {
            return null;
        }
        JsonElement e = o.get("EXTENDED_DESCRIPTION");
        if (!e.isJsonArray()) {
            return null;
        }
        JsonArray arr = e.getAsJsonArray();
        String[] out = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            out[i] = arr.get(i).getAsString();
        }
        return out;
    }

    /**
     * 简洁版教程页数组；没有对应条目时返回 null。
     *
     * <p>页数与正式文案可以不同 —— 调用方（MessageCaller）会按这里返回的页数去加载
     * {@code img/UI/Tutorial/Tutorial_<key>_<n>.png}，所以精简教程要配自己的图片，
     * 或者沿用同名图的编号。图片缺失不影响文字显示。
     */
    public static String[] tutorialText(String key) {
        ensureTutorials();
        return tutorials.get(key);
    }

    // =====================================================================
    // 惰性加载
    // =====================================================================

    private static void ensureCards() {
        if (cards != null) {
            return;
        }
        cards = new HashMap<>();
        JsonObject root = readJson(CARD_PATH);
        if (root == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> en : root.entrySet()) {
            if (en.getValue().isJsonObject()) {
                cards.put(en.getKey(), en.getValue().getAsJsonObject());
            }
        }
    }

    private static void ensureTutorials() {
        if (tutorials != null) {
            return;
        }
        tutorials = new HashMap<>();
        JsonObject root = readJson(TUTORIAL_PATH);
        if (root == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> en : root.entrySet()) {
            JsonElement v = en.getValue();
            if (!v.isJsonObject() || !v.getAsJsonObject().has("TEXT")) {
                continue;
            }
            JsonElement t = v.getAsJsonObject().get("TEXT");
            if (!t.isJsonArray()) {
                continue;
            }
            JsonArray arr = t.getAsJsonArray();
            String[] pages = new String[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                pages[i] = arr.get(i).getAsString();
            }
            tutorials.put(en.getKey(), pages);
        }
    }

    /** 读不到文件（或文件是空壳）时静默返回 null，让调用方回退到正式文案。 */
    private static JsonObject readJson(String path) {
        try {
            FileHandle fh = Gdx.files.internal(path);
            if (fh == null || !fh.exists()) {
                return null;
            }
            String s = fh.readString("UTF-8");
            if (s == null || s.trim().isEmpty()) {
                return null;
            }
            JsonElement e = new JsonParser().parse(s);
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception ex) {
            System.out.println("[DoubleSS] concise text load failed: " + path + " -> " + ex);
            return null;
        }
    }
}
