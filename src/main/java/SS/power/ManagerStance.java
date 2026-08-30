package SS.power;

import SS.helper.ModHelper;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import SS.vfx.ManagerStanceParticle;

public class ManagerStance extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("ManagerStance");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    private float particleTimer = 0.0f;

    public ManagerStance(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = -1;

        String path128 = "img/power/ManagerPower84.png";
        String path48 = "img/power/ManagerPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void onVictory() {
        addToBot(new RemoveSpecificPowerAction(owner, owner, this));
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    @Override
    public void updateParticles() {
        this.particleTimer -= Gdx.graphics.getDeltaTime();
        if (this.particleTimer <= 0.0f) {
            this.particleTimer = MathUtils.random(0.08f, 0.14f);
            AbstractDungeon.effectsQueue.add(new ManagerStanceParticle(owner));
            if (MathUtils.random(3) < 2) {
                AbstractDungeon.effectsQueue.add(new ManagerStanceParticle(owner));
            }
        }
    }
}