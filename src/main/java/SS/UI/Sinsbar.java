package SS.UI;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import SS.power.SinsPower;

public class Sinsbar {// 图片原始尺寸数据 (根据你的测量)
    private static final float RAW_TOTAL_WIDTH = 1024.0F; // 227 + 619 + 178
    private static final float RAW_BAR_WIDTH = 619.0F; // 中间有效长度
    private static final float RAW_OFFSET_X = 227.0F; // 左侧空白长度
    private static final float RAW_HEIGHT = 1024.0F; // 实际图片高度
    // 修改这个值来整体缩放 UI (0.5F = 一半大小, 1.0F = 原大小)
    private static final float UI_SCALE = 0.6F;

    // 经过游戏缩放后的尺寸 (用于实际绘制计算)
    private static final float TOTAL_WIDTH = RAW_TOTAL_WIDTH * Settings.scale * UI_SCALE;
    private static final float MAX_BAR_WIDTH = RAW_BAR_WIDTH * Settings.scale * UI_SCALE;
    private static final float BAR_OFFSET_X = RAW_OFFSET_X * Settings.scale * UI_SCALE;
    private static final float BAR_HEIGHT = RAW_HEIGHT * Settings.scale * UI_SCALE;

    // 2. 实际进度条的最大宽度 (不包含翅膀)
    private static final float HB_Y_OFFSET = -10.0F * Settings.scale * UI_SCALE; // 距离头顶的高度
    private static final float MIN_SIN = -50.0F;
    private static final float MAX_SIN = 50.0F;
    private static final float TOTAL_RANGE = MAX_SIN - MIN_SIN;

    private Texture bgTexture;
    private Texture fillTexture;

    private float currentVal = 0.0F; // 用于动画显示的数值
    private float targetVal = 0.0F; // 实际的Power层数

    public Sinsbar() {
        // 加载图片
        this.bgTexture = ImageMaster.loadImage("img/UI/SinsBar/Sinsbar.png");
        this.fillTexture = ImageMaster.loadImage("img/UI/SinsBar/SinsbarFill.png");
        targetVal = 0;

        // 如果你想让条初始看起来是半满的，但玩家身上没有Power，
        // 你需要在这里设置 targetVal = MAX_SIN / 2;
        // 但通常建议是在战斗开始给予玩家 50 层 Power。
    }

    public void update() {
        if (AbstractDungeon.player == null)
            return;

        // 1. 获取目标值
        if (AbstractDungeon.player.hasPower(SinsPower.POWER_ID)) {
            // 直接读取 amount，可能是负数，例如 -20
            this.targetVal = AbstractDungeon.player.getPower(SinsPower.POWER_ID).amount;
        } else {
            // 如果没有该 Power，默认为 0（表现为半满）
            this.targetVal = 0.0F;
        }

        // 2. 限制数值范围，防止超出 UI 边界
        if (this.targetVal > MAX_SIN)
            this.targetVal = MAX_SIN;
        if (this.targetVal < MIN_SIN)
            this.targetVal = MIN_SIN;

        // 3. 线性插值动画
        this.currentVal = com.badlogic.gdx.math.MathUtils.lerp(this.currentVal, this.targetVal,
                com.badlogic.gdx.Gdx.graphics.getDeltaTime() * 5.0F);
    }

    public void render(SpriteBatch sb) {
        if (com.megacrit.cardcrawl.core.CardCrawlGame.dungeon == null) {
            return;
        }

        if (AbstractDungeon.player == null) {
            return;
        }

        if (AbstractDungeon.getCurrRoom() == null) {
            return;
        }

        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS ||
                AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP ||
                AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID ||
                AbstractDungeon.isScreenUp) {
            return;
        }

        // 如果你想在地图或篝火也显示，可以去掉这一行
        if (AbstractDungeon.getCurrRoom().phase != com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT) {
            return;
        }

        if (AbstractDungeon.getCurrRoom().phase != com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT)
            return;// -----------------------------------------------------------
        // 步骤 A: 计算背景图的位置
        // -----------------------------------------------------------
        // 我们希望整个长条图(1024宽) 在玩家头顶居中
        float bgX = AbstractDungeon.player.hb.cX - (TOTAL_WIDTH / 2.0F);
        float bgY = AbstractDungeon.player.hb.cY + HB_Y_OFFSET;

        sb.setColor(Color.WHITE);
        // 绘制完整的背景 (带翅膀)
        sb.draw(bgTexture, bgX, bgY, TOTAL_WIDTH, BAR_HEIGHT);

        // -----------------------------------------------------------
        // 步骤 B: 计算并绘制填充条
        // -----------------------------------------------------------
        // 计算百分比 (-50到50 映射到 0.0到1.0)
        float percentage = (this.currentVal - MIN_SIN) / TOTAL_RANGE;
        if (percentage > 1.0F)
            percentage = 1.0F;
        if (percentage < 0.0F)
            percentage = 0.0F;

        if (percentage > 0) {
            // 1. 填充条的起始 X 坐标 = 背景图左边缘 + 左侧空白(227缩放后)
            float fillX = bgX + BAR_OFFSET_X;

            // 2. 当前应该显示的宽度 = 最大宽度(619缩放后) * 百分比
            float currentRenderWidth = MAX_BAR_WIDTH * percentage;

            // 3. 纹理截取宽度 (你的填充图原始宽度应该是 619px)
            int srcWidth = fillTexture.getWidth();
            int srcRegionWidth = (int) (srcWidth * percentage);

            // 绘制逻辑

            sb.draw(fillTexture,
                    fillX, bgY, // 屏幕坐标：严格卡在左侧空白之后
                    0, 0, // 旋转原点
                    currentRenderWidth, BAR_HEIGHT, // 屏幕尺寸：动态变宽
                    1.0F, 1.0F, // 缩放
                    0, // 旋转
                    0, 0, // srcX, srcY: 从图片最左侧开始切
                    srcRegionWidth, fillTexture.getHeight(), // srcW, srcH: 切多少
                    false, false);

        }

        // -----------------------------------------------------------
        // 步骤 C: 绘制文字 (位置修正)
        // -----------------------------------------------------------
        // 注意：因为左右两边的翅膀不一样长 (227 vs 178)，
        // 所以"整个图片的中心"并不是"进度条的中心"。
        // 我们需要手动计算进度条的几何中心。

        // 进度条中心X = 填充条起点 + (总长度的一半)
        float textX = bgX + BAR_OFFSET_X + (MAX_BAR_WIDTH / 2.0F);
        float textY = bgY + (BAR_HEIGHT / 2.0F);

        int displayNum = (int) this.targetVal;
        String msg = String.valueOf(Math.abs(displayNum)) + "/"
                + String.valueOf((int) (displayNum > 0 ? MAX_SIN : -MIN_SIN));

        // 简单的颜色逻辑
        Color textColor = Color.WHITE;
        if (displayNum > 0)
            textColor = Color.RED;
        if (displayNum < 0)
            textColor = Color.CYAN;

        FontHelper.renderFontCentered(sb, FontHelper.powerAmountFont, msg, textX, textY, textColor, UI_SCALE * 2F);
    }
}