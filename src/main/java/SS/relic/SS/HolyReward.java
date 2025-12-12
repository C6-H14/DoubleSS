package SS.relic.SS;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

import SS.helper.ModHelper;
import SS.modcore.modcore;
import SS.path.AbstractCardEnum;
import basemod.abstracts.CustomRelic;

public class HolyReward extends CustomRelic {
    public static final String ID = ModHelper.makePath("HolyReward");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/HolyReward.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private boolean cardsSelected = true;

    public HolyReward() {
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
        AbstractDungeon.player.increaseMaxHp(5, true);
        this.cardsSelected = false;
        CardGroup tmp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (AbstractCard card : (AbstractDungeon.player.masterDeck.getPurgeableCards()).group) {
            if (modcore.blessMap.containsKey(card.cardID)) {
                tmp.addToTop(card);
            }
        }

        if (tmp.group.isEmpty()) {
            this.cardsSelected = true;
            return;
        }
        if (tmp.group.size() <= 1) {
            giveCards(tmp.group);
        } else if (!AbstractDungeon.isScreenUp) {
            AbstractDungeon.gridSelectScreen.open(tmp, 1, this.DESCRIPTIONS[1] + this.name + LocalizedStrings.PERIOD,
                    false, false, false, false);

        } else {

            AbstractDungeon.dynamicBanner.hide();
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
            AbstractDungeon.gridSelectScreen.open(tmp, 1, this.DESCRIPTIONS[1] + this.name + LocalizedStrings.PERIOD,
                    false, false, false, false);
        }
    }

    public void update() {
        super.update();
        if (!this.cardsSelected &&
                AbstractDungeon.gridSelectScreen.selectedCards.size() == 1) {
            giveCards(AbstractDungeon.gridSelectScreen.selectedCards);
        }
    }

    public void giveCards(ArrayList<AbstractCard> group) {
        this.cardsSelected = true;
        float displayCount = 0.0F;
        for (Iterator<AbstractCard> i = group.iterator(); i.hasNext();) {
            AbstractCard card = i.next();
            card.untip();
            card.unhover();
            AbstractDungeon.player.masterDeck.removeCard(card);
            AbstractCard bless = null;
            if (modcore.blessMap.containsKey(card.cardID)) {
                bless = (CardLibrary.getCard(modcore.blessMap.get(card.cardID))).makeCopy();
            }

            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.TRANSFORM && bless != null) {

                AbstractDungeon.topLevelEffectsQueue.add(new ShowCardAndObtainEffect(

                        bless, Settings.WIDTH / 3.0F + displayCount,
                        Settings.HEIGHT / 2.0F, false));

                displayCount += Settings.WIDTH / 6.0F;
            }
        }
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
        (AbstractDungeon.getCurrRoom()).rewardPopOutTimer = 0.25F;
    }

    public AbstractRelic makeCopy() {
        return new HolyReward();
    }
}
