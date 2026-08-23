package SS.power;

import SS.cards.Miserly;
import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MiserlyPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("MiserlyPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private int count = 0;
    private AbstractCard card;

    public MiserlyPower(AbstractCreature owner, int amount, int index) {
        this.name = NAME;
        this.ID = POWER_ID + index;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;
        this.count = index;
        card = new Miserly();
        for (int i = 0; i < index; ++i) {
            card.upgrade();
        }
        String path128 = "img/power/MiserlyPower84.png";
        String path48 = "img/power/MiserlyPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    @Override
    public void atStartOfTurnPostDraw() {
        if (this.amount <= 0) {
            return;
        }
        flash();

        int remaining = this.amount;
        Random rng = new Random(AbstractDungeon.cardRandomRng.randomLong());

        // 1. 第一优先级：优先随机替换手牌
        ArrayList<AbstractCard> handList = new ArrayList<>(AbstractDungeon.player.hand.group);
        if (!handList.isEmpty()) {
            Collections.shuffle(handList, rng);
            int countToChange = Math.min(remaining, handList.size());
            for (int i = 0; i < countToChange; i++) {
                replaceCardInGroup(AbstractDungeon.player.hand, handList.get(i), this.card);
            }
            remaining -= countToChange;

            // 刷新手牌的高亮和数值计算
            AbstractDungeon.player.hand.applyPowers();
            AbstractDungeon.player.hand.glowCheck();
        }

        // 2. 第二优先级：手牌不够时，在抽牌堆 + 弃牌堆中随机替换
        if (remaining > 0) {
            ArrayList<AbstractCard> drawAndDiscard = new ArrayList<>();
            drawAndDiscard.addAll(AbstractDungeon.player.drawPile.group);
            drawAndDiscard.addAll(AbstractDungeon.player.discardPile.group);

            if (!drawAndDiscard.isEmpty()) {
                Collections.shuffle(drawAndDiscard, rng);
                int countToChange = Math.min(remaining, drawAndDiscard.size());
                for (int i = 0; i < countToChange; i++) {
                    AbstractCard target = drawAndDiscard.get(i);
                    if (AbstractDungeon.player.drawPile.contains(target)) {
                        replaceCardInGroup(AbstractDungeon.player.drawPile, target, this.card);
                    } else if (AbstractDungeon.player.discardPile.contains(target)) {
                        replaceCardInGroup(AbstractDungeon.player.discardPile, target, this.card);
                    }
                }
                remaining -= countToChange;
            }
        }

        // 3. 第三优先级：如果依然不够，最后在消耗堆中随机替换
        if (remaining > 0) {
            ArrayList<AbstractCard> exhaustList = new ArrayList<>(AbstractDungeon.player.exhaustPile.group);
            if (!exhaustList.isEmpty()) {
                Collections.shuffle(exhaustList, rng);
                int countToChange = Math.min(remaining, exhaustList.size());
                for (int i = 0; i < countToChange; i++) {
                    replaceCardInGroup(AbstractDungeon.player.exhaustPile, exhaustList.get(i), this.card);
                }
                remaining -= countToChange;
            }
        }
    }

    /**
     * 在指定的卡牌组中，将 oldCard 替换为 templateCard 的副本
     */
    private void replaceCardInGroup(CardGroup group, AbstractCard oldCard, AbstractCard templateCard) {
        int idx = group.group.indexOf(oldCard);
        if (idx != -1) {
            AbstractCard newCard = templateCard.makeStatEquivalentCopy();
            // 如果是手牌，继承原有卡牌的渲染位置/缩放/角度，防止UI跳变
            if (group == AbstractDungeon.player.hand) {
                newCard.current_x = oldCard.current_x;
                newCard.current_y = oldCard.current_y;
                newCard.target_x = oldCard.target_x;
                newCard.target_y = oldCard.target_y;
                newCard.drawScale = oldCard.drawScale;
                newCard.targetDrawScale = oldCard.targetDrawScale;
                newCard.angle = oldCard.angle;
                newCard.targetAngle = oldCard.targetAngle;
            }
            group.group.set(idx, newCard);
        }
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] + (3 - this.count) + DESCRIPTIONS[2];
    }
}