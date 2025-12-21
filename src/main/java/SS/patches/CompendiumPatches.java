package SS.patches;

import basemod.ReflectionHacks;

import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.compendium.CardLibSortHeader;
import com.megacrit.cardcrawl.screens.mainMenu.SortHeaderButton;
import com.megacrit.cardcrawl.screens.mainMenu.SortHeaderButtonListener;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public class CompendiumPatches {
    public static UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(ModHelper.makePath("Compendium"));
    public static SortHeaderButton packButton = null;
    public static boolean isMyTab = false;
    public static ArrayList<AbstractCard.CardColor> allowedCardColors = new ArrayList<>(
            Arrays.asList(new AbstractCard.CardColor[] { AbstractCard.CardColor.RED, AbstractCard.CardColor.GREEN,
                    AbstractCard.CardColor.BLUE, AbstractCard.CardColor.PURPLE }));

    @SpirePatch2(clz = CardLibSortHeader.class, method = "<ctor>")
    public static class AddSortButton {
        @SpirePrefixPatch
        public static void changePositions() {
            ReflectionHacks.setPrivateStaticFinal(CardLibSortHeader.class, "START_X",
                    Float.valueOf(400.0F * Settings.xScale));
            ReflectionHacks.setPrivateStaticFinal(CardLibSortHeader.class, "SPACE_X",
                    Float.valueOf(176.0F * Settings.xScale));
        }

        @SpireInsertPatch(locator = Locator.class, localvars = { "xPosition" })
        public static void addButton(CardLibSortHeader __instance, float xPosition) {
            CompendiumPatches.packButton = new SortHeaderButton(CompendiumPatches.uiStrings.TEXT[0],
                    xPosition + CardLibSortHeader.SPACE_X, 0.0F, (SortHeaderButtonListener) __instance);
        }

        @SpirePostfixPatch
        public static void addToButtons(CardLibSortHeader __instance) {
            SortHeaderButton[] butts = new SortHeaderButton[__instance.buttons.length + 1];
            System.arraycopy(__instance.buttons, 0, butts, 0, __instance.buttons.length);
            butts[butts.length - 1] = CompendiumPatches.packButton;
            __instance.buttons = butts;
        }

        private static class Locator
                extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
                Matcher.FieldAccessMatcher fieldAccessMatcher = new Matcher.FieldAccessMatcher(CardLibSortHeader.class,
                        "buttons");
                return LineFinder.findAllInOrder(ctMethodToPatch, (Matcher) fieldAccessMatcher);
            }
        }
    }

    // 【新增】每当图鉴切换卡牌组时（点别的颜色标签时），检查一下是不是你的卡
    @SpirePatch2(clz = CardLibSortHeader.class, method = "setGroup")
    public static class DetectModTab {
        @SpirePostfixPatch
        public static void Postfix(CardLibSortHeader __instance, CardGroup group) {
            if (group != null && group.size() > 0) {
                // 获取第一张卡，检查颜色是否是你角色的颜色
                // 请将 MyCharacterEnum.MY_COLOR 替换为你实际的颜色枚举
                if (group.group.get(0).color == AbstractCardEnum.SS_Yellow) {
                    CompendiumPatches.isMyTab = true;
                    return;
                }
            }
            // 如果组是空的，或者颜色不对，就不是你的Tab
            CompendiumPatches.isMyTab = false;
        }
    }

    @SpirePatch2(clz = CardLibSortHeader.class, method = "didChangeOrder")
    public static class CustomOrdering {
        @SpirePostfixPatch
        public static void catchSort(CardLibSortHeader __instance, SortHeaderButton button, boolean isAscending) {
            if (button == CompendiumPatches.packButton
                    && CompendiumPatches.isMyTab) {
                CompendiumPatches.packSort(__instance, isAscending);
                __instance.group.sortByStatus(false);
                __instance.justSorted = true;
                button.setActive(true);
            }
        }

        public static int getPackName(Object o) {
            if (o instanceof AbstractDoubleCard) {
                String s = ((AbstractDoubleCard) o).packagetype.toString();
                if (s == "Default")
                    return 2147483647;
                if (s == "SS")
                    return 0;
                return -(((AbstractDoubleCard) o).packagetype.ordinal());
            }
            return 2147483647;
        }
    }

    @SpirePatch2(clz = CardLibSortHeader.class, method = "setGroup")
    public static class SortCorrectlyOnSwitch {
        @SpirePostfixPatch
        public static void patch(CardLibSortHeader __instance) {
            for (SortHeaderButton b : __instance.buttons) {
                if (b == CompendiumPatches.packButton
                        && ((Boolean) ReflectionHacks.getPrivate(b, SortHeaderButton.class, "isActive")).booleanValue()
                        && CompendiumPatches.isMyTab) {
                    CompendiumPatches.packSort(__instance, true);
                }
            }
        }
    }

    @SpirePatch2(clz = SortHeaderButton.class, method = "<ctor>", paramtypez = { String.class, float.class,
            float.class })
    public static class ReduceButtonHitboxSize {
        @SpireInstrumentPatch
        public static ExprEditor CatchHitboxInit() {
            return new ExprEditor() {
                public void edit(NewExpr m) throws CannotCompileException {
                    if (m.getClassName().equals(Hitbox.class.getName())) {
                        m.replace("{$_ = $proceed(" + CompendiumPatches.ReduceButtonHitboxSize.class

                                .getName() + ".changeXSize($1), $2);}");
                    }
                }
            };
        }

        public static float changeXSize(float curSize) {
            return curSize - 40.0F * Settings.xScale;
        }
    }

    public static void packSort(CardLibSortHeader input, boolean isAscending) {
        if (isAscending) {
            input.group.group.sort(
                    Comparator.comparing(CustomOrdering::getPackName)
                            .thenComparing(o -> ((AbstractCard) o).rarity)
                            .thenComparing(o -> ((AbstractCard) o).name));
        } else {

            input.group.group.sort(Collections.reverseOrder(
                    Comparator.comparing(CustomOrdering::getPackName)
                            .thenComparing(o -> ((AbstractCard) o).rarity)
                            .thenComparing(o -> ((AbstractCard) o).name)));
        }
    }

    @SpirePatch2(clz = CardLibSortHeader.class, method = "renderButtons")
    public static class HideSortButtonForOtherTabs {
        public static class HideSortButtonForOtherTabsExprEditor
                extends ExprEditor {
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (methodCall.getClassName().equals(SortHeaderButton.class.getName())
                        && methodCall.getMethodName().equals("render")) {
                    methodCall.replace(String.format(
                            "{ if(%1$s.isMyTab || $0 != %1$s.packButton) { $proceed($$); } }",
                            CompendiumPatches.class.getName()));
                }
            }
        }

        @SpireInstrumentPatch
        public static ExprEditor HideSortButtonForOtherTabsPatch() {
            return new HideSortButtonForOtherTabsExprEditor();
        }
    }

    @SpirePatch2(clz = SortHeaderButton.class, method = "update")
    public static class DisableClickForOtherTabs {
        @SpirePrefixPatch
        public static SpireReturn<Void> disableClickForOtherTabs(SortHeaderButton __instance) {
            if (__instance == CompendiumPatches.packButton
                    && !CompendiumPatches.isMyTab) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }
}