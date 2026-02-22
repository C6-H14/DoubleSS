package SS.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class AllyAbsorbCardEffect extends AbstractGameEffect {
    private AbstractCard card;
    private float startX, startY;
    private float targetX, targetY;

    // 动画总时长
    private static final float DUR = 0.8f;

    public AllyAbsorbCardEffect(AbstractCard c, float targetX, float targetY) {
        // 创建一个视觉副本，以免影响逻辑中的卡牌对象
        this.card = c.makeStatEquivalentCopy();
        this.duration = DUR;
        this.startingDuration = DUR;
        this.targetX = targetX;
        this.targetY = targetY;

        // 起始位置：屏幕左下角 (抽牌堆大概位置)
        this.startX = 200.0F * Settings.scale;
        this.startY = 200.0F * Settings.scale;

        // 初始状态
        this.card.current_x = startX;
        this.card.current_y = startY;
        this.card.drawScale = 0.1f; // 开始时很小，像从牌堆里钻出来
        this.card.targetDrawScale = 0.8f; // 飞行过程中变大一点以便看清
        this.card.angle = 0.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();

        // 进度 (0.0 -> 1.0)
        float t = 1.0f - (this.duration / this.startingDuration);

        if (t > 1.0f)
            t = 1.0f;

        // 1. 移动插值 (使用 CircleOut 或者 Pow2Out 让它看起来像被磁力吸过去)
        this.card.current_x = Interpolation.circleOut.apply(startX, targetX, t);
        this.card.current_y = Interpolation.circleOut.apply(startY, targetY, t);

        // 2. 缩放插值 (先变大，再变小融入体内)
        // 前 80% 时间变大，最后 20% 时间缩小
        if (t < 0.8f) {
            this.card.drawScale = Interpolation.linear.apply(0.1f, 0.8f, t / 0.8f);
        } else {
            this.card.drawScale = Interpolation.linear.apply(0.8f, 0.01f, (t - 0.8f) / 0.2f);
        }

        // 3. 旋转效果
        this.card.angle = MathUtils.lerp(0.0f, 360.0f * 2, t); // 旋转两圈

        // 4. 透明度/颜色 (最后阶段稍微变淡)
        if (t > 0.9f) {
            this.card.transparency = 1.0f - ((t - 0.9f) * 10.0f);
        }

        // 更新卡牌内部逻辑 (处理发光等)
        this.card.update();

        if (this.duration <= 0.0f) {
            this.isDone = true;
            // 播放一个音效表示吸入成功
            // CardCrawlGame.sound.play("HEAL_3");
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            this.card.render(sb);
        }
    }

    @Override
    public void dispose() {
    }
}