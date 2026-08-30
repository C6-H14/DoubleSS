package SS.relic.SS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.mod.stslib.relics.ClickableRelic;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.MonsterRoom;

import SS.action.common.MessageCaller;
import SS.action.monster.EvokeSoulAction;
import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;

public class BookOfVirtue extends CustomRelic implements ClickableRelic {
    public static final String ID = ModHelper.makePath("BookOfVirtue");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/BookOfVirtue.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.MAGICAL;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;

    public BookOfVirtue() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public void atTurnStart() {
        addToBot(new EvokeSoulAction(1));
    }

    @Override
    public void onRightClick() {
        // 1. 安全检查：必须在战斗房间内右键才触发
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {

            this.addToBot(new MessageCaller("C6H14"));
        }
    }

    public AbstractRelic makeCopy() {
        return new BookOfVirtue();
    }
}
