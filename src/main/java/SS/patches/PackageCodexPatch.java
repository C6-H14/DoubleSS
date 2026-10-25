package SS.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;

import SS.UI.PackageCodex;
import SS.characters.AbstractSSCharacter;

/** A modal on character select; the card popup remains owned by the game. */
public class PackageCodexPatch {
    private static boolean selected(CharacterSelectScreen screen) {
        for (CharacterOption option : screen.options) {
            if (option.selected && option.c instanceof AbstractSSCharacter) { return true; }
        }
        return false;
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "update")
    public static class EntryUpdate {
        public static SpireReturn<Void> Prefix(CharacterSelectScreen __instance) {
            if (selected(__instance) && PackageCodex.updateEntry()) { return SpireReturn.Return(null); }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "render")
    public static class EntryRender {
        public static void Postfix(CharacterSelectScreen __instance, SpriteBatch sb) {
            if (selected(__instance) && !PackageCodex.isOpen()) { PackageCodex.renderEntry(sb); }
        }
    }

    @SpirePatch(clz = MainMenuScreen.class, method = "update")
    public static class ModalUpdate {
        public static SpireReturn<Void> Prefix(MainMenuScreen __instance) {
            if (!PackageCodex.isOpen()) {
                if (__instance.screen == MainMenuScreen.CurScreen.CARD_LIBRARY
                        && PackageCodex.updateLibraryEntry()) { return SpireReturn.Return(null); }
                return SpireReturn.Continue();
            }
            if (__instance.screen != MainMenuScreen.CurScreen.CHAR_SELECT
                    && __instance.screen != MainMenuScreen.CurScreen.CARD_LIBRARY) {
                PackageCodex.close();
                return SpireReturn.Continue();
            }
            PackageCodex.update();
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz = MainMenuScreen.class, method = "render")
    public static class ModalRender {
        public static void Postfix(MainMenuScreen __instance, SpriteBatch sb) {
            if (__instance.screen == MainMenuScreen.CurScreen.CHAR_SELECT
                    || __instance.screen == MainMenuScreen.CurScreen.CARD_LIBRARY) {
                if (PackageCodex.isOpen()) { PackageCodex.render(sb); }
                else if (__instance.screen == MainMenuScreen.CurScreen.CARD_LIBRARY) {
                    PackageCodex.renderLibraryEntry(sb);
                }
            }
        }
    }
}
