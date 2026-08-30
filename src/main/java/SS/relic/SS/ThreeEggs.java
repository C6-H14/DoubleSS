package SS.relic.SS;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.mod.stslib.actions.tempHp.AddTemporaryHPAction;
import com.evacipated.cardcrawl.mod.stslib.relics.ClickableRelic;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import SS.action.common.MessageCaller;
import SS.helper.ModHelper;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import basemod.abstracts.CustomRelic;

public class ThreeEggs extends CustomRelic implements ClickableRelic {
    public static final String ID = ModHelper.makePath("ThreeEggs");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/ThreeEggs.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.BOSS;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.MAGICAL;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private int amount = 9;

    public ThreeEggs() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
        this.counter = 0;
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + "9" + this.DESCRIPTIONS[1];
    }

    @Override
    public boolean canSpawn() {
        return AbstractDungeon.player.hasRelic("Double:Egg");
    }

    // 2. 获得时替换原初始遗物
    @Override
    public void obtain() {
        if (AbstractDungeon.player.hasRelic("Double:Egg")) {
            // 找到初始遗物所在的具体位置
            for (int i = 0; i < AbstractDungeon.player.relics.size(); ++i) {
                if (AbstractDungeon.player.relics.get(i).relicId.equals("Double:Egg")) {
                    // 原位替换：放入第 i 个槽位，第三个参数 true 表示触发获取逻辑
                    instantObtain(AbstractDungeon.player, i, true);
                    break;
                }
            }
        } else {
            super.obtain();
        }
    }

    public void atBattleStart() {
        addToBot(new RelicAboveCreatureAction((AbstractCreature) AbstractDungeon.player, this));
        addToBot(new AddTemporaryHPAction((AbstractCreature) AbstractDungeon.player,
                (AbstractCreature) AbstractDungeon.player, amount));
    }

    @Override
    public void onRightClick() {
        // 1. 安全检查：必须在战斗房间内右键才触发
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {

            this.addToBot(new MessageCaller("SS"));
        }
    }

    @Override
    public void onVictory() {
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {
            this.counter++;
            if (this.counter >= 5) {
                this.counter = 0;
                AbstractPlayer p = AbstractDungeon.player;
                ArrayList<AbstractRelic> validRelics = new ArrayList<>();
                ArrayList<AbstractRelic> targetRelics = new ArrayList<>();
                for (AbstractRelic r : p.relics) {
                    boolean isTarget = false;
                    if (r.tier == RelicTier.STARTER && !r.relicId.equals(this.relicId)) {
                        isTarget = true;
                    } else {
                        for (AbstractPackage c : modcore.mainPackageList) {
                            if (c.StartRelic != null && c.StartRelic.relicId.equals(r.relicId)) {
                                isTarget = true;
                                targetRelics.add(c.BossRelic.makeCopy());
                                break;
                            }
                        }
                    }
                    if (isTarget) {
                        validRelics.add(r);
                    }
                }
                if (!validRelics.isEmpty()) {
                    AbstractRelic toRemove = validRelics.get(0), toAdd = targetRelics.get(0).makeCopy();
                    for (int i = 0; i < AbstractDungeon.player.relics.size(); ++i) {
                        if (AbstractDungeon.player.relics.get(i).relicId.equals(toRemove.relicId)) {
                            // 原位替换：放入第 i 个槽位，第三个参数 true 表示触发获取逻辑
                            toAdd.instantObtain(AbstractDungeon.player, i, true);
                            break;
                        }
                    }
                }
                this.flash();
            }
        }
    }

    public AbstractRelic makeCopy() {
        return new ThreeEggs();
    }
}
