package SS.action.common;

import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.ui.FtueTip;

// 【核心】：直接使用 BaseMod 自带的类！
import basemod.abstracts.CustomMultiPageFtue;

public class MessageCaller extends AbstractGameAction {
    private String relicKey;
    private Texture[] images;
    private String[] texts;

    public MessageCaller(String relicKey) {
        this.relicKey = relicKey;

        // 1. 读取语言包文本 (例如 JSON 里的 "Double:MarkOfAncient")
        TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Double:" + relicKey);

        if (tutorialStrings != null && tutorialStrings.TEXT != null) {
            this.texts = tutorialStrings.TEXT;
            int pageCount = this.texts.length;
            this.images = new Texture[pageCount];

            // 2. 根据 JSON 文本行数，全自动加载对应数量的图片
            // 完美遵循你设计的 "Tutorial_String_no" 命名规范
            for (int i = 0; i < pageCount; i++) {
                int pageNo = i + 1; // 编号从 1 开始
                String imagePath = "img/UI/Tutorial/Tutorial_" + relicKey + "_" + pageNo + ".png";
                this.images[i] = ImageMaster.loadImage(imagePath);
            }
        }
    }

    @Override
    public void update() {
        if (this.images != null && this.texts != null) {
            // 直接赋值唤起 BaseMod 的滑动多页教程！
            AbstractDungeon.ftue = (FtueTip) new CustomMultiPageFtue(this.images, this.texts);
        }
        this.isDone = true;
    }
}