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

import SS.action.common.MessageCaller;
import SS.helper.ModHelper;
import SS.power.DyingPower;
import basemod.abstracts.CustomRelic;

public class HolyMantle extends CustomRelic implements ClickableRelic {
    public static final String ID = ModHelper.makePath("HolyMantle");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/HolyMantle.png";
    // Boss 遗物级别
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.BOSS;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.MAGICAL;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;

    // 标记当前是否处于“免死保护期”
    public boolean isProtected = false;

    public HolyMantle() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
        updateDescription();
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    public void updateDescription() {
        this.description = getUpdatedDescription();
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    // ================= Boss 遗物替换逻辑 =================
    @Override
    public boolean canSpawn() {
        // 只有持有 WoodenCross 时才会在 Boss 宝箱中生成
        return AbstractDungeon.player.hasRelic(WoodenCross.ID);
    }

    @Override
    public void obtain() {
        // 原位替换 WoodenCross
        if (AbstractDungeon.player.hasRelic(WoodenCross.ID)) {
            for (int i = 0; i < AbstractDungeon.player.relics.size(); ++i) {
                if (AbstractDungeon.player.relics.get(i).relicId.equals(WoodenCross.ID)) {
                    instantObtain(AbstractDungeon.player, i, true);
                    break;
                }
            }
        } else {
            super.obtain();
        }
    }

    // ================= 战斗效果 =================
    @Override
    public void atPreBattle() {
        this.isProtected = false;
        this.grayscale = false;
        flash();
        addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        // 战斗开始时获得 10 层 DyingPower
        addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player,
                new DyingPower(AbstractDungeon.player, 10), 10));
    }

    @Override
    public void atTurnStart() {
        // 新回合开始，重置保护状态并恢复高亮
        if (this.isProtected) {
            this.isProtected = false;
            this.grayscale = false;
        }
    }

    /**
     * 触发免死保护：进入保护期并变灰
     */
    public void triggerProtection() {
        this.isProtected = true;
        this.grayscale = true;
        this.flash();
        addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
    }

    @Override
    public void justEnteredRoom(AbstractRoom room) {
        this.grayscale = false;
        this.isProtected = false;
    }

    @Override
    public void onRightClick() {
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {
            this.addToBot(new MessageCaller("Lost"));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new HolyMantle();
    }
}