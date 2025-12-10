package SS.UI;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import SS.power.SinsPower;

public class Sinsbar {
    private static final float HB_Y_OFFSET = -80.0F * Settings.scale; // 距离头顶的高度
    private static final float BAR_WIDTH = 500.0F * Settings.scale;
    private static final float BAR_HEIGHT = 500.0F * Settings.scale;
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

        // 如果你想在地图或篝火也显示，可以去掉这一行
        if (AbstractDungeon.getCurrRoom().phase != com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT) {
            return;
        }

        if (AbstractDungeon.getCurrRoom().phase != com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT)
            return;

        // 计算位置
        float x = AbstractDungeon.player.hb.cX - (BAR_WIDTH / 2.0F);
        float y = AbstractDungeon.player.hb.cY + (AbstractDungeon.player.hb.height / 2.0F) + HB_Y_OFFSET;

        sb.setColor(Color.WHITE);
        sb.draw(bgTexture, x, y, BAR_WIDTH, BAR_HEIGHT);

        // --- 核心修改：计算百分比 ---

        // 假设 currentVal 是 0，则 (0 + 50) / 100 = 0.5 (半满)
        // 假设 currentVal 是 -50，则 (-50 + 50) / 100 = 0.0 (空)
        float percentage = (this.currentVal - MIN_SIN) / TOTAL_RANGE;

        // 安全钳制，防止越界
        if (percentage > 1.0F)
            percentage = 1.0F;
        if (percentage < 0.0F)
            percentage = 0.0F;

        if (percentage > 0) {
            sb.draw(fillTexture,
                    x, y,
                    BAR_WIDTH * percentage, // 宽度根据比例计算
                    BAR_HEIGHT);
        }

        // 1. 决定显示什么数字
        // 建议显示 targetVal (真实层数)，因为 currentVal 是浮点数动画，
        // 显示 decimals (如 10.45) 会很乱，显示跳变的整数动画也不太直观。
        int displayNum = (int) this.targetVal;
        String msg = String.valueOf(Math.abs(displayNum)) + "/"
                + (displayNum > 0 ? String.valueOf((int) MAX_SIN) : String.valueOf((int) -MIN_SIN));

        // 2. 决定文字颜色 (可选)
        // 比如：正数绿色，负数红色，0 白色
        Color textColor = Color.WHITE;
        if (displayNum > 0) {
            textColor = Color.RED; // 或者 Settings.GREEN_TEXT_COLOR
        } else if (displayNum < 0) {
            textColor = Color.BLUE; // 或者 Settings.RED_TEXT_COLOR
        }

        // 3. 绘制文字
        // 参数：画笔, 字体, 文字内容, 中心X, 中心Y, 颜色
        FontHelper.renderFontCentered(
                sb,
                FontHelper.powerAmountFont, // 这是一个比较适合小数值的字体
                msg,
                x + (BAR_WIDTH / 2.0F), // 条的中心 X
                y + (BAR_HEIGHT / 2.0F), // 条的中心 Y
                textColor);
    }
}