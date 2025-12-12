package SS.relic.SS;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;

import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;

public class DemonReward extends CustomRelic {
    public static final String ID = ModHelper.makePath("DemonReward");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/DemonReward.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private boolean cardsSelected = true;

    public DemonReward() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
        this.counter = 1;
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void instantObtain(AbstractPlayer p, int slot, boolean callOnEquip) {
        if (p.hasRelic(ID)) {
            // 如果玩家已经有这个遗物了
            stackRelic(p);
        } else {
            // 如果没有，执行默认的获取逻辑（添加到遗物栏）
            super.instantObtain(p, slot, callOnEquip);
        }
    }

    @Override
    public void instantObtain() {
        if (AbstractDungeon.player.hasRelic(ID)) {
            // 如果玩家已经有这个遗物了
            stackRelic(AbstractDungeon.player);
        } else {
            // 如果没有，执行默认的获取逻辑（添加到遗物栏）
            super.instantObtain();
        }
    }

    @Override
    public void obtain() {
        if (AbstractDungeon.player.hasRelic(ID)) {
            // 如果玩家已经有这个遗物了
            stackRelic(AbstractDungeon.player);
        } else {
            // 如果没有，执行默认获取逻辑
            super.obtain();
        }
    }

    private void stackRelic(AbstractPlayer p) {
        // 获取玩家身上已有的那个遗物实例
        AbstractRelic existingRelic = p.getRelic(ID);

        // 增加计数器
        existingRelic.counter++;

        // 播放闪烁效果，提示玩家生效了
        existingRelic.flash();

        // 在玩家头顶显示该遗物的图标（可选，增加视觉反馈）
        AbstractDungeon.actionManager.addToBottom(
                new RelicAboveCreatureAction(p, existingRelic));
        this.onEquip();
        this.isDone = true;
        this.isObtained = true;
        this.discarded = true;

        // 注意：这里不需要做任何"销毁当前新遗物"的操作，
        // 因为我们没有调用 super.obtain()，所以这个新生成的遗物实例会自动被垃圾回收。
    }

    public void onEquip() {
        AbstractDungeon.player.decreaseMaxHealth(5);
        this.cardsSelected = false;
        if (AbstractDungeon.isScreenUp) {
            AbstractDungeon.dynamicBanner.hide();
            AbstractDungeon.overlayMenu.cancelButton.hide();
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
        }
        (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.INCOMPLETE;

        CardGroup tmp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (AbstractCard card : (AbstractDungeon.player.masterDeck.getPurgeableCards()).group) {
            tmp.addToTop(card);
        }

        if (tmp.group.isEmpty()) {
            this.cardsSelected = true;
            return;
        }
        if (tmp.group.size() <= 1) {
            deleteCards(tmp.group);
        } else {
            AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.masterDeck
                    .getPurgeableCards(), 1, this.DESCRIPTIONS[1], false, false, false, true);
        }
    }

    public void update() {
        super.update();
        if (!this.cardsSelected &&
                AbstractDungeon.gridSelectScreen.selectedCards.size() == 1) {
            deleteCards(AbstractDungeon.gridSelectScreen.selectedCards);
        }
    }

    public void deleteCards(ArrayList<AbstractCard> group) {
        this.cardsSelected = true;
        float displayCount = 0.0F;
        for (Iterator<AbstractCard> i = group.iterator(); i.hasNext();) {
            AbstractCard card = i.next();
            card.untip();
            card.unhover();
            AbstractDungeon.topLevelEffects
                    .add(new PurgeCardEffect(card, Settings.WIDTH / 3.0F + displayCount, Settings.HEIGHT / 2.0F));

            displayCount += Settings.WIDTH / 6.0F;
            AbstractDungeon.player.masterDeck.removeCard(card);
        }

        (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.COMPLETE;
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
    }

    public AbstractRelic makeCopy() {
        return new DemonReward();
    }
}
