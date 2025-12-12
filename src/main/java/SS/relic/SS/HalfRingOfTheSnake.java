package SS.relic.SS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;

public class HalfRingOfTheSnake extends CustomRelic {
    public static final String ID = ModHelper.makePath("HalfRingOfTheSnake");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/HalfRingOfTheSnake.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private int amount = 1;

    public HalfRingOfTheSnake() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + "1" + this.DESCRIPTIONS[1];
    }

    public void atBattleStart() {
        addToBot(new RelicAboveCreatureAction((AbstractCreature) AbstractDungeon.player, this));
        addToBot(new DrawCardAction((AbstractCreature) AbstractDungeon.player, this.amount));
    }

    public AbstractRelic makeCopy() {
        return new HalfRingOfTheSnake();
    }
}
