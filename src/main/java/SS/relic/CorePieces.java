package SS.relic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;

public class CorePieces extends CustomRelic {
    public static final String ID = ModHelper.makePath("CorePieces");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/CorePieces.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private int amount = 4;

    public CorePieces() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public void atBattleStart() {
        addToBot(new RelicAboveCreatureAction((AbstractCreature) AbstractDungeon.player, this));
        addToBot(new ChannelDiceAction(new AttackDice(amount, AbstractDungeon.getRandomMonster())));
    }

    public AbstractRelic makeCopy() {
        return new CorePieces();
    }
}
