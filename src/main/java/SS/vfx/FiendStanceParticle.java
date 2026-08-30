package SS.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * FiendStance 能力粒子：脚底向上翻飞的紫色光点，旋转上升、逐渐淡出。
 * 贴图使用内置 GLOW_SPARK 光斑（小而不挡视野）。
 */
public class FiendStanceParticle extends AbstractGameEffect {
    private float x;
    private float y;
    private float baseX;
    private float velY;
    private float accelY;
    private float driftAmp;
    private float driftFreq;
    private float driftPhase;
    private float rotationSpeed;
    private float startScale;

    private static final Color PURPLE = new Color(0.8f, 0.2f, 1.0f, 1f);

    public FiendStanceParticle(AbstractCreature owner) {
        this.duration = MathUtils.random(0.8f, 1.2f);
        this.startingDuration = this.duration;
        this.color = PURPLE.cpy();

        // 出生点：脚底，X 在血条宽度 70% 范围内随机
        this.baseX = owner.hb.cX + MathUtils.random(-0.35f, 0.35f) * owner.hb.width;
        this.x = this.baseX;
        this.y = owner.hb.cY - owner.hb.height / 2.0f + MathUtils.random(-8f, 8f) * Settings.scale;

        // 向上初速 + 轻微加速，越升越快，像被吸上去
        this.velY = MathUtils.random(180f, 280f) * Settings.scale;
        this.accelY = MathUtils.random(100f, 200f) * Settings.scale;

        // X 轴轻微正弦漂移
        this.driftAmp = MathUtils.random(12f, 30f) * Settings.scale;
        this.driftFreq = MathUtils.random(2f, 5f);
        this.driftPhase = MathUtils.random(0f, 6.283f);

        // 旋转角速度（避免 0 附近太小看不出旋转）
        this.rotationSpeed = MathUtils.random(-300f, 300f);
        if (Math.abs(this.rotationSpeed) < 60f) {
            this.rotationSpeed = 150f;
        }
        this.rotation = MathUtils.random(0f, 360f);

        this.startScale = MathUtils.random(0.4f, 0.8f) * Settings.scale;
        this.scale = this.startScale;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.duration -= dt;
        if (this.duration <= 0f) {
            this.isDone = true;
            return;
        }

        float elapsed = this.startingDuration - this.duration;
        float t = elapsed / this.startingDuration;

        this.velY += this.accelY * dt;
        this.y += this.velY * dt;
        this.x = this.baseX + MathUtils.sin(elapsed * this.driftFreq + this.driftPhase) * this.driftAmp;
        this.rotation += this.rotationSpeed * dt;
        this.scale = this.startScale * (1f - 0.6f * t);

        // 透明度：开头 10% 快速淡入，55% 之后平滑渐隐至 0
        float alpha = 0.7f;
        if (t < 0.1f) {
            alpha *= t / 0.1f;
        }
        if (t > 0.55f) {
            alpha *= (1f - (t - 0.55f) / 0.45f);
        }
        this.color.a = Math.max(0f, alpha);
    }

    @Override
    public void render(SpriteBatch sb) {
        float w = ImageMaster.GLOW_SPARK.packedWidth;
        float h = ImageMaster.GLOW_SPARK.packedHeight;
        sb.setColor(this.color);
        sb.draw(ImageMaster.GLOW_SPARK,
                this.x - w * this.scale / 2f, this.y - h * this.scale / 2f,
                w / 2f, h / 2f, w, h,
                this.scale, this.scale, this.rotation);
        sb.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        // 使用共享的 ImageMaster 贴图，无需释放
    }
}
