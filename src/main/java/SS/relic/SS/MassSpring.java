package SS.relic.SS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.ApplyPowerToRandomEnemyAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;

public class MassSpring extends CustomRelic {
    public static final String ID = ModHelper.makePath("MassSpring");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/MassSpring.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private int amount = 2;

    public MassSpring() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
        this.amount = 2;
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public void atPreBattle() {
        this.amount = 2;
        this.grayscale = false;
    }

    public void atTurnStart() {
        if (this.amount <= 0) {
            this.grayscale = true;
            return;
        }
        --this.amount;
        this.flash();
        addToBot(new ApplyPowerToRandomEnemyAction(AbstractDungeon.player, new VulnerablePower(null, 1, false), 1));
        addToBot(new ApplyPowerToRandomEnemyAction(AbstractDungeon.player, new WeakPower(null, 1, false), 1));
    }

    public AbstractRelic makeCopy() {
        return new MassSpring();
    }
}
