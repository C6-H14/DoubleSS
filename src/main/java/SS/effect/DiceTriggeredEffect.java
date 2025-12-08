package SS.effect;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DiceTriggeredEffect extends AbstractGameEffect
{
    private float effectDuration;
    private float x;
    private float y;
    private float OriginScale;
    private float ratio;
    private Texture img;
    public DiceTriggeredEffect(float x, float y, Texture img, float scale, Color color) {
        this.img = img;
        this.effectDuration = 0.4F;
        this.duration = this.effectDuration;
        this.x = x;
        this.y = y;
        this.color = color;
        this.OriginScale = scale;
    }
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.ratio = (this.effectDuration - this.duration) / this.effectDuration;
        if (this.duration < 0.0F) this.isDone = true; 
    }
    public void render(SpriteBatch sb)
    {
        float scale = 1.0F + this.ratio * 0.7F;
        Color tmpColor = this.color.cpy();
        this.color.a *= 1.0F - this.ratio;
        sb.setBlendFunction(770, 771);
        sb.setColor(tmpColor);
        if (this.img==null)
        {
            System.out.println("Found Null Pointer");
        }
        sb.draw(this.img, this.x, this.y, 48.0F, 48.0F, 96.0F, 96.0F, this.OriginScale * scale, this.OriginScale * scale, 0.0F, 0, 0, 96, 96, false, false);
    }
    public void dispose() {}
}