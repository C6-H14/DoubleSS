package SS.relic.SS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;

public class BathWater extends CustomRelic {
    public static final String ID = ModHelper.makePath("BathWater");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/BathWater.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private boolean firstTurn = true;

    public BathWater() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public void atPreBattle() {
        this.firstTurn = true;
    }

    public void atTurnStart() {
        if (this.firstTurn) {
            flash();
            addToTop(new GainEnergyAction(1));
            addToTop(new RelicAboveCreatureAction((AbstractCreature) AbstractDungeon.player, this));
            this.firstTurn = false;
        }
    }

    public AbstractRelic makeCopy() {
        return new BathWater();
    }
}
