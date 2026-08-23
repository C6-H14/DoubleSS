package SS.power;

import SS.helper.ModHelper;
import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;

public class PhoneItInPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("PhoneItInPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    // 记录上回合符合条件的卡牌 ID
    private ArrayList<String> cardsPlayedLastTurn = new ArrayList<>();

    public PhoneItInPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/PhoneItInPower84.png";
        String path48 = "img/power/PhoneItInPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    /**
     * 判断卡牌是否满足条件：
     * 是 SS_Yellow 且 packagetype 不是 Default
     */
    private boolean isValidCard(AbstractCard card) {
        if (card.color == AbstractCardEnum.SS_Yellow && card instanceof AbstractDoubleCard) {
            return ((AbstractDoubleCard) card).packagetype != PackageEnum.Default;
        }
        return false;
    }

    /**
     * 玩家回合结束时记录本回合全局打出的所有有效卡牌
     */
    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            this.cardsPlayedLastTurn.clear();
            for (AbstractCard card : AbstractDungeon.actionManager.cardsPlayedThisTurn) {
                if (isValidCard(card)) {
                    this.cardsPlayedLastTurn.add(card.cardID);
                }
            }
        }
    }

    @Override
    public void atStartOfTurn() {
        if (!this.cardsPlayedLastTurn.isEmpty() && this.amount > 0) {
            this.flash();

            for (int i = 0; i < this.amount; i++) {
                // 1. 独立随机选择一张卡牌 ID（允许重复）
                int randomIndex = AbstractDungeon.cardRandomRng.random(this.cardsPlayedLastTurn.size() - 1);
                String cardIdToTrigger = this.cardsPlayedLastTurn.get(randomIndex);

                // 2. 创建一张未升级的卡牌实例
                AbstractCard tmp = CardLibrary.getCard(cardIdToTrigger).makeCopy();

                // 3. 放入 limbo 并设置相关属性
                AbstractDungeon.player.limbo.addToBottom(tmp);
                tmp.dontTriggerOnUseCard = true; // 不重复触发打出卡牌相关的遗物/能力（如时光吞噬者、苦无等）
                tmp.purgeOnUse = true; // 打出后彻底清除（purge），不会进入弃牌堆/消耗堆

                // 初始位置和目标屏幕中心位置
                tmp.current_x = (float) Settings.WIDTH / 2.0F - 300.0F * Settings.scale;
                tmp.current_y = (float) Settings.HEIGHT / 2.0F;
                tmp.target_x = (float) Settings.WIDTH / 2.0F - 300.0F * Settings.scale;
                tmp.target_y = (float) Settings.HEIGHT / 2.0F;

                // 4. 选取目标怪物
                AbstractMonster target = null;
                if (tmp.target == AbstractCard.CardTarget.ENEMY
                        || tmp.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                    target = AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true,
                            AbstractDungeon.cardRandomRng);
                }

                // 5. 加入出牌队列
                // CardQueueItem 构造函数参数：(card, monsterTarget, energyOnUse, ignoreEnergyTotal,
                // autoplaying)
                AbstractDungeon.actionManager.addCardQueueItem(
                        new CardQueueItem(tmp, target, tmp.energyOnUse, true, true),
                        false // false 会按循环顺序依次排队打出
                );
            }

            // 6. 添加短暂等待动画，使多张牌出牌观感更自然
            if (!Settings.FAST_MODE) {
                addToBot(new WaitAction(Settings.ACTION_DUR_MED));
            } else {
                addToBot(new WaitAction(Settings.ACTION_DUR_FASTER));
            }
        }
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}