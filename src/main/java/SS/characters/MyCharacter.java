package SS.characters;

import basemod.BaseMod;
import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.cutscenes.CutscenePanel;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.screens.CharSelectInfo;

import SS.cards.RecordingTeam;
import SS.cards.Strike;
import SS.path.AbstractCardEnum;
import SS.path.ThmodClassEnum;

import java.util.ArrayList;

/**
 * 二硫键（SS）。
 *
 * 只保留身份差异：颜色/牌库枚举、起始牌组与圣物、人物选择信息、本地化文本、
 * cutscene、渲染颜色、SpireEnum 注册。
 * 机制（骰子充能、卡包卡池、选角色特效等）全部在 {@link AbstractSSCharacter}，
 * 未来变种直接 extends AbstractSSCharacter 即继承整套机制。
 */
public class MyCharacter extends AbstractSSCharacter {
    private static final String MY_CHARACTER_SHOULDER_1 = "img/char/shoulder.png";
    private static final String MY_CHARACTER_SHOULDER_2 = "img/char/shoulder2.png";
    private static final String CORPSE_IMAGE = "img/char/Corpse.png";
    private static final String[] ORB_TEXTURES = new String[] { "img/UI/orb/layer5.png", "img/UI/orb/layer4.png",
            "img/UI/orb/layer3.png", "img/UI/orb/layer2.png", "img/UI/orb/layer1.png", "img/UI/orb/layer6.png",
            "img/UI/orb/layer5d.png", "img/UI/orb/layer4d.png", "img/UI/orb/layer3d.png", "img/UI/orb/layer2d.png",
            "img/UI/orb/layer1d.png" };
    private static final float[] LAYER_SPEED = new float[] { -40.0F, -32.0F, 20.0F, -20.0F, 0.0F, -10.0F, -8.0F, 5.0F,
            -5.0F, 0.0F };
    private static final CharacterStrings characterStrings = CardCrawlGame.languagePack
            .getCharacterString("Double:Double");
    public static final Color SS_Yellow = new Color(0.984375F, 0.91796875F, 0.16796875F, 1.0F);

    public static class PlayerColorEnum {
        @SpireEnum
        public static AbstractPlayer.PlayerClass SS;
        @SpireEnum
        public static AbstractCard.CardColor SS_Yellow;
        @SpireEnum
        public static AbstractCard.CardColor Hao_Green;
    }

    public static class PlayerLibraryEnum {
        @SpireEnum
        public static CardLibrary.LibraryType SS_Yellow;
        @SpireEnum
        public static CardLibrary.LibraryType Hao_Green;
        @SpireEnum
        public static CardLibrary.LibraryType Lost_Black;
        @SpireEnum
        public static CardLibrary.LibraryType Shock_Blue;
        @SpireEnum
        public static CardLibrary.LibraryType C6H14_Cyan;
    }

    public MyCharacter(String name) {
        super(name, ThmodClassEnum.SS_CLASS, ORB_TEXTURES, "img/UI/orb/vfx.png", LAYER_SPEED, null, null);
        this.dialogX = this.drawX + 0.0F * Settings.scale;
        this.dialogY = this.drawY + 150.0F * Settings.scale;
        initializeClass("img/char/Character.png", "img/char/shoulder2.png", "img/char/shoulder.png",
                "img/char/Corpse.png",
                getLoadout(), 0.0F, 0.0F, 300.0F, 350.0F, new EnergyManager(3));
    }

    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<>();
        int x;
        for (x = 0; x < 3; x++) {
            retVal.add("Double:Strike");
        }
        for (x = 0; x < 3; x++) {
            retVal.add("Double:Defend");
        }
        retVal.add("Double:RecordingTeam");
        retVal.add("Double:Seething");
        return retVal;
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        return new RecordingTeam();
    }

    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<>();
        retVal.add("Double:Egg");
        return retVal;
    }

    public CharSelectInfo getLoadout() {
        return new CharSelectInfo("二硫键", "二硫键看到这里记得发一下录播", 70, 70, 5, 99, 5, (AbstractPlayer) this,
                getStartingRelics(),
                getStartingDeck(), false);
    }

    public String getTitle(AbstractPlayer.PlayerClass playerClass) {
        return "二硫键";
    }

    @Override
    public AbstractCard.CardColor getCardColor() {
        return AbstractCardEnum.SS_Yellow;
    }

    public Color getCardTrailColor() {
        return SS_Yellow;
    }

    public ArrayList<CutscenePanel> getCutscenePanels() {
        ArrayList<CutscenePanel> panels = new ArrayList<>();
        panels.add(new CutscenePanel("img/char/scene1.png", "ATTACK_MAGIC_FAST_1"));
        panels.add(new CutscenePanel("img/char/scene2.png"));
        panels.add(new CutscenePanel("img/char/scene3.png"));
        return panels;
    }

    public String getCustomModeCharacterButtonSoundKey() {
        return "ATTACK_HEAVY";
    }

    public String getLocalizedCharacterName() {
        return "二硫键";
    }

    @Override
    public AbstractPlayer newInstance() {
        return (AbstractPlayer) new MyCharacter(this.name);
    }

    public String getSpireHeartText() {
        return characterStrings.TEXT[1];
    }

    public Color getSlashAttackColor() {
        return SS_Yellow;
    }

    public String getVampireText() {
        // 本地化：characters.json 的 Double:Double TEXT[2]（含 NL 换行与 ~...~ 颜色标记）
        return characterStrings.TEXT[2];
    }

    public Color getCardRenderColor() {
        return SS_Yellow;
    }
}
