package SS.Dice;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import SS.helper.ModHelper;


public class EmptyDiceSlot extends AbstractDice
{
    public static final String ORB_ID =ModHelper.makePath("Empty");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    public static final String[] DESC = orbString.DESCRIPTION;
    public EmptyDiceSlot(float x, float y) {
        this.angle = MathUtils.random(360.0F);
        this.ID = "SS:Empty";
        this.name = orbString.NAME;
        this.evokeAmount = 0;
        this.cX = x;
        this.cY = y;
        updateDescription();
        this.channelAnimTimer = 0.5F;
    }
    public EmptyDiceSlot()
    {
        this.angle = MathUtils.random(360.0F);
        this.name = orbString.NAME;
        this.evokeAmount = 0;
        this.cX = AbstractDungeon.player.drawX + AbstractDungeon.player.hb_x;
        this.cY = AbstractDungeon.player.drawY + AbstractDungeon.player.hb_y + AbstractDungeon.player.hb_h / 2.0F;
        updateDescription();
    }
    public void updateDescription() {
        this.description = DESC[0];
    }
    public void myEvoke() {}
    public void updateAnimation() {
        super.updateAnimation();
        this.angle += Gdx.graphics.getDeltaTime() * 10.0F;
    }
  
    public void render(SpriteBatch sb) {
        sb.setColor(this.c);
        sb.draw(ImageMaster.ORB_SLOT_2, this.cX - 48.0F - this.bobEffect.y / 8.0F, this.cY - 48.0F + this.bobEffect.y / 8.0F, 48.0F, 48.0F, 96.0F, 96.0F, this.scale, this.scale, 0.0F, 0, 0, 96, 96, false, false);
        sb.draw(ImageMaster.ORB_SLOT_1, this.cX - 48.0F + this.bobEffect.y / 8.0F, this.cY - 48.0F - this.bobEffect.y / 8.0F, 48.0F, 48.0F, 96.0F, 96.0F, this.scale, this.scale, this.angle, 0, 0, 96, 96, false, false);
        renderText(sb);
        this.hb.render(sb);
    }
  
    public AbstractDice makeCopy()
    {
        return new EmptyDiceSlot();
    }
    public void playChannelSFX() {}
}