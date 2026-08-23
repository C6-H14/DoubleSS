package SS.relic.SS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.mod.stslib.actions.tempHp.AddTemporaryHPAction;
import com.evacipated.cardcrawl.mod.stslib.relics.ClickableRelic;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.MonsterRoom;

import SS.action.common.MessageCaller;
import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;

public class Egg extends CustomRelic implements ClickableRelic {
    public static final String ID = ModHelper.makePath("Egg");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/Egg.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.STARTER;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private int amount = 6;

    public Egg() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + "6" + this.DESCRIPTIONS[1];
    }

    public void atBattleStart() {
        addToBot(new RelicAboveCreatureAction((AbstractCreature) AbstractDungeon.player, this));
        addToBot(new AddTemporaryHPAction((AbstractCreature) AbstractDungeon.player,
                (AbstractCreature) AbstractDungeon.player, amount));
        System.out.println(AbstractDungeon.player.hb_x + "----" + AbstractDungeon.player.hb_y);
        System.out.println(AbstractDungeon.player.healthHb.cX + "----" + AbstractDungeon.player.healthHb.cY);
    }

    @Override
    public void onRightClick() {
        // 1. 安全检查：必须在战斗房间内右键才触发
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {

            this.addToBot(new MessageCaller("SS"));
        }
    }

    public AbstractRelic makeCopy() {
        return new Egg();
    }
}
