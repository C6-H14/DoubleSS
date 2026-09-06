package SS.relic.SS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.mod.stslib.relics.ClickableRelic;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;

import SS.action.common.MessageCaller;
import SS.helper.ModHelper;
import SS.power.DyingPower;
import basemod.abstracts.CustomRelic;
import basemod.abstracts.CustomSavable;

public class WoodenCross extends CustomRelic implements CustomSavable<int[]>, ClickableRelic {
    public static final String ID = ModHelper.makePath("WoodenCross");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/WoodenCross.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private int amount = 5;

    public WoodenCross() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
        this.amount = 5;
        this.counter = 0; // counter 用来在遗物右下角显示当前战斗计数 (0, 1, 2)
        updateDescription();
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    public void updateDescription() {
        this.description = getUpdatedDescription();
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onVictory() {
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom
                && !(AbstractDungeon.getCurrRoom() instanceof MonsterRoomElite
                        || AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss)) {
            this.counter++;
            if (this.counter >= 6) {
                this.counter = 0;
                this.amount++;
                this.flash();
                updateDescription();
            }
        }
    }

    @Override
    public void atPreBattle() {
        flash();
        addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player,
                new DyingPower(AbstractDungeon.player, this.amount), this.amount));
        this.grayscale = true;
    }

    @Override
    public void justEnteredRoom(AbstractRoom room) {
        this.grayscale = false;
    }

    // ================= S/L 存档/读档 支持 =================
    @Override
    public int[] onSave() {
        // 保存 [amount, counter] 两个数值
        return new int[] { this.amount, this.counter };
    }

    @Override
    public void onLoad(int[] savedData) {
        if (savedData != null && savedData.length >= 2) {
            this.amount = savedData[0];
            this.counter = savedData[1];
        }
        updateDescription();
    }

    @Override
    public void onRightClick() {
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {

            this.addToBot(new MessageCaller("Lost"));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new WoodenCross();
    }
}