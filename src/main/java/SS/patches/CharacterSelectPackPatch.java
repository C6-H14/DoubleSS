package SS.patches;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;

import SS.characters.AbstractSSCharacter;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.packages.AbstractPackage.PackageType;
import SS.path.PackageEnumList.PackageEnum;

/**
 * 人物选择界面的「自定义卡包游玩」：勾选框 + 三个选项条（对应局内三个卡包槽位）。
 *
 * 每个选项条第 0 行是「默认选项（进游戏后选择）」，之后按主卡包顺序列出
 * 8 个主卡包各自的 3 个子卡包，展示名与游戏内选择完全一致：
 * option_card 本地化名 + ":" + Double:OptionCardSuffix 后缀（数值/运转/上限，
 * 与 addClassChoice 中 左=VALUE/中=CONSISTENCY/右=CEILING 的配对相同）。
 *
 * 布局与输入完全参照 ThePackmaster 的 MainMenuUIPatch（renderRelics/updateHitbox 的 Postfix）。
 * 选择结果持久化在 SpireConfig("Double", "Common") 的 CustomPackEnabled / CustomPackSel0..2，
 * 由 modcore.TriggerAtGameStart 在新开局时读取并预置。
 */
public class CharacterSelectPackPatch {
    private static final int PACK_SLOTS = 3;
    private static final int DROPDOWN_ROWCOUNT = 10;
    private static final float SPACING = 50.0F * Settings.scale;
    private static final float CHECKBOX_Y = Settings.HEIGHT / 2.0F - 175.0F * Settings.yScale;
    private static final float CHECKBOX_X_OFF = 32.0F * Settings.xScale;
    private static final float DROPDOWNS_START_Y = CHECKBOX_Y + SPACING * (PACK_SLOTS + 0.5F);
    private static float CHECKBOX_X;
    private static float DROPDOWN_X;

    private static final Hitbox toggleHb = new Hitbox(40.0F * Settings.scale, 40.0F * Settings.scale);
    private static final DropdownMenu[] dropdowns = new DropdownMenu[PACK_SLOTS];
    private static final ArrayList<String> options = new ArrayList<>();
    private static final ArrayList<PowerTip> toggleTips = new ArrayList<>();
    private static String[] optionIDs = new String[0];
    private static PackageEnum[] optionColors = new PackageEnum[0]; // 每行对应的主卡包颜色（0 行为 null）
    private static final int[] selections = new int[PACK_SLOTS]; // 0=默认，1..24=具体子卡包

    private static boolean enabled = false;
    private static boolean built = false;
    private static UIStrings uiStrings;

    /** 首次使用时惰性构建（此时卡包与本地化都已就绪）。构建前每帧调用均为廉价 no-op。 */
    private static void ensureBuilt() {
        if (built) {
            return;
        }
        // 人物选择界面早于开局：新进程首次进来时卡包还没建，这里主动初始化
        modcore.ensurePackages();
        if (modcore.mainPackageList.isEmpty()) {
            return; // 卡包尚未初始化，下帧重试
        }
        loadPrefs();

        uiStrings = CardCrawlGame.languagePack.getUIString("Double:CustomPackToggle");
        String defaultText = CardCrawlGame.languagePack.getUIString("Double:CustomPackOptionDefault").TEXT[0];
        String[] suffixes;
        try {
            suffixes = CardCrawlGame.languagePack.getUIString("Double:OptionCardSuffix").TEXT;
        } catch (Exception e) {
            suffixes = null;
        }

        options.add(defaultText);
        optionIDs = new String[1 + modcore.mainPackageList.size() * 3];
        optionColors = new PackageEnum[optionIDs.length];
        optionIDs[0] = "";
        optionColors[0] = null;
        int idx = 1;
        for (AbstractPackage p : modcore.mainPackageList) {
            if (p.ID.equals("Double:NullPackage")) {
                continue;
            }
            // 子卡包顺序与游戏内选择一致：数值(_v) / 运转(_c) / 上限(_e)
            addOption(idx++, p, p.SubPackages.get(PackageType.VALUE), suffixes, 0);
            addOption(idx++, p, p.SubPackages.get(PackageType.CONSISTENCY), suffixes, 1);
            addOption(idx++, p, p.SubPackages.get(PackageType.CEILING), suffixes, 2);
        }
        optionIDs = Arrays.copyOf(optionIDs, idx);
        optionColors = Arrays.copyOf(optionColors, idx);

        for (int i = 0; i < PACK_SLOTS; i++) {
            if (selections[i] < 0 || selections[i] >= optionIDs.length) {
                selections[i] = 0;
            }
        }
        for (int i = 0; i < PACK_SLOTS; i++) {
            final int slot = i;
            dropdowns[i] = new DropdownMenu((d, optionIndex, s) -> onDropdownChanged(d, slot, optionIndex),
                    options, FontHelper.tipBodyFont, Settings.CREAM_COLOR, DROPDOWN_ROWCOUNT);
            dropdowns[i].setSelectedIndex(selections[i]);
        }

        // 布局参照 MainMenuUIPatch：靠右对齐，勾选框在上、选项条在下
        float dropdownX = Settings.WIDTH - 50.0F * Settings.scale - dropdowns[0].approximateOverallWidth();
        float checkboxX = Settings.WIDTH - 82.0F * Settings.scale
                - FontHelper.getWidth(FontHelper.tipHeaderFont, uiStrings.TEXT[0], 1.0F);
        if (checkboxX - dropdownX >= 60.0F) {
            DROPDOWN_X = dropdownX;
            CHECKBOX_X = checkboxX + CHECKBOX_X_OFF;
        } else {
            DROPDOWN_X = Math.min(dropdownX, checkboxX);
            CHECKBOX_X = DROPDOWN_X + CHECKBOX_X_OFF;
        }
        built = true;
    }

    private static void addOption(int idx, AbstractPackage main, AbstractPackage sub, String[] suffixes, int suffixIndex) {
        String name = (sub.OptionCard != null) ? sub.OptionCard.name : sub.ID;
        if (name != null && suffixes != null && suffixIndex < suffixes.length) {
            name = name + ":" + suffixes[suffixIndex];
        }
        options.add(name);
        optionIDs[idx] = sub.ID;
        optionColors[idx] = main.PackageColor;
    }

    /** 下拉选择变更：later-wins 防重复（同一主卡包的子卡包只占一个槽位，其余重置为默认）。 */
    private static void onDropdownChanged(DropdownMenu changed, int slot, int optionIndex) {
        selections[slot] = optionIndex;
        if (optionIndex >= 1) {
            PackageEnum chosen = optionColors[optionIndex];
            for (int i = 0; i < PACK_SLOTS; i++) {
                if (i != slot && selections[i] >= 1
                        && optionColors[selections[i]] == chosen) {
                    dropdowns[i].setSelectedIndex(0);
                    selections[i] = 0;
                }
            }
        }
        savePrefs();
    }

    private static void loadPrefs() {
        try {
            Properties defaults = new Properties();
            defaults.setProperty("CustomPackEnabled", "false");
            for (int i = 0; i < PACK_SLOTS; i++) {
                defaults.setProperty("CustomPackSel" + i, "0");
            }
            SpireConfig cfg = new SpireConfig("Double", "Common", defaults);
            enabled = "true".equals(cfg.getString("CustomPackEnabled"));
            for (int i = 0; i < PACK_SLOTS; i++) {
                try {
                    selections[i] = Integer.parseInt(cfg.getString("CustomPackSel" + i));
                } catch (Exception e) {
                    selections[i] = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void savePrefs() {
        try {
            SpireConfig cfg = new SpireConfig("Double", "Common");
            cfg.setString("CustomPackEnabled", String.valueOf(enabled));
            for (int i = 0; i < PACK_SLOTS; i++) {
                cfg.setString("CustomPackSel" + i, String.valueOf(selections[i]));
            }
            cfg.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 第 slot 个槽位预置的子卡包；默认/未启用时返回 null。返回主卡包 SubPackages 中的实例（与游戏内选择路径同源）。 */
    public static AbstractPackage resolveSelection(int slot) {
        ensureBuilt();
        if (!built || slot < 0 || slot >= PACK_SLOTS || !enabled) {
            return null;
        }
        int idx = selections[slot];
        if (idx <= 0 || idx >= optionIDs.length) {
            return null;
        }
        String id = optionIDs[idx];
        for (AbstractPackage p : modcore.mainPackageList) {
            for (AbstractPackage sub : p.SubPackages.values()) {
                if (sub.ID.equals(id)) {
                    return sub;
                }
            }
        }
        return null;
    }

    static void render(SpriteBatch sb) {
        ensureBuilt();
        if (!built) {
            return;
        }
        toggleHb.move(CHECKBOX_X, CHECKBOX_Y);
        toggleHb.render(sb);
        sb.setColor(Color.WHITE);
        float checkScale = Settings.scale * 0.8F;
        sb.draw(ImageMaster.CHECKBOX, toggleHb.cX - 32.0F, toggleHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F,
                checkScale, checkScale, 0.0F, 0, 0, 64, 64, false, false);
        if (enabled) {
            sb.draw(ImageMaster.TICK, toggleHb.cX - 32.0F, toggleHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F,
                    checkScale, checkScale, 0.0F, 0, 0, 64, 64, false, false);
        }
        FontHelper.renderSmartText(sb, FontHelper.tipHeaderFont, uiStrings.TEXT[0],
                toggleHb.cX + 25.0F * Settings.scale,
                toggleHb.cY + FontHelper.getHeight(FontHelper.tipHeaderFont) * 0.5F, Settings.BLUE_TEXT_COLOR);
        if (enabled) {
            for (int i = dropdowns.length - 1; i >= 0; i--) {
                dropdowns[i].render(sb, DROPDOWN_X, DROPDOWNS_START_Y - SPACING * i);
            }
        }
    }

    static void update() {
        ensureBuilt();
        if (!built) {
            return;
        }
        boolean stopInput = false;
        if (enabled) {
            for (DropdownMenu d : dropdowns) {
                if (d.isOpen) {
                    stopInput = true;
                }
                d.update();
                if (d.isOpen || stopInput) {
                    stopInput = true;
                    InputHelper.justClickedLeft = false;
                    InputHelper.justReleasedClickLeft = false;
                    CInputActionSet.select.unpress();
                    CInputActionSet.proceed.unpress();
                }
            }
        }
        if (!stopInput) {
            toggleHb.update();
            if (toggleHb.hovered) {
                if (toggleTips.isEmpty()) {
                    toggleTips.add(new PowerTip(uiStrings.TEXT[0], uiStrings.TEXT[1]));
                }
                if (InputHelper.mX < 1400.0F * Settings.scale) {
                    TipHelper.queuePowerTips(InputHelper.mX + 60.0F * Settings.scale,
                            InputHelper.mY - 50.0F * Settings.scale, toggleTips);
                } else {
                    TipHelper.queuePowerTips(InputHelper.mX - 350.0F * Settings.scale,
                            InputHelper.mY - 50.0F * Settings.scale, toggleTips);
                }
                if (InputHelper.justClickedLeft) {
                    CardCrawlGame.sound.playA("UI_CLICK_1", -0.4F);
                    toggleHb.clickStarted = true;
                }
            }
            if (toggleHb.clicked) {
                toggleHb.clicked = false;
                enabled = !enabled;
                savePrefs();
            }
        }
    }

    @SpirePatch(clz = CharacterOption.class, method = "renderRelics")
    public static class RenderOptions {
        public static void Postfix(CharacterOption obj, SpriteBatch sb) {
            if (obj != null && obj.c instanceof AbstractSSCharacter && obj.selected) {
                CharacterSelectPackPatch.render(sb);
            }
        }
    }

    @SpirePatch(clz = CharacterOption.class, method = "updateHitbox")
    public static class UpdateOptions {
        public static void Postfix(CharacterOption obj) {
            if (obj != null && obj.c instanceof AbstractSSCharacter && obj.selected) {
                CharacterSelectPackPatch.update();
            }
        }
    }
}
