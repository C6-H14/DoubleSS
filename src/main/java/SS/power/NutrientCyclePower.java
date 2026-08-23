package SS.power;

import SS.action.common.PlayCardAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class NutrientCyclePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("NutrientCyclePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public NutrientCyclePower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/NutrientCyclePower84.png";
        String path48 = "img/power/NutrientCyclePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    private boolean isValidCard(AbstractCard card) {
        if (card.color != AbstractCardEnum.SS_Yellow) {
            return true;
        }
        if (card instanceof AbstractDoubleCard) {
            return ((AbstractDoubleCard) card).packagetype != PackageEnum.Default;
        }
        return false;
    }

    @Override
    public void atStartOfTurn() {
        if (this.amount <= 0) {
            addToBot(new RemoveSpecificPowerAction(owner, owner, this));
            return;
        }

        this.flash();

        // 收集弃牌堆和消耗堆中符合条件的牌
        ArrayList<AbstractCard> validCards = new ArrayList<>();
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (isValidCard(c)) {
                validCards.add(c);
            }
        }
        for (AbstractCard c : AbstractDungeon.player.exhaustPile.group) {
            if (isValidCard(c)) {
                validCards.add(c);
            }
        }

        if (!validCards.isEmpty()) {
            Collections.shuffle(validCards, new Random(AbstractDungeon.cardRandomRng.randomLong()));
            int countToPlay = Math.min(this.amount, validCards.size());

            for (int i = 0; i < countToPlay; i++) {
                AbstractCard card = validCards.get(i);

                // 从原牌堆移除
                if (AbstractDungeon.player.discardPile.contains(card)) {
                    AbstractDungeon.player.discardPile.removeCard(card);
                } else if (AbstractDungeon.player.exhaustPile.contains(card)) {
                    AbstractDungeon.player.exhaustPile.removeCard(card);
                }

                // 获取随机存活敌人作为目标
                AbstractMonster targetMonster = AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true,
                        AbstractDungeon.cardRandomRng);

                // 通过修复后的 PlayCardAction 播放完整动画并打出
                addToBot(new PlayCardAction(targetMonster, card, false));
            }
        }

        // 移除自身
        addToBot(new RemoveSpecificPowerAction(owner, owner, this));
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}