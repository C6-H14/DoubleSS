package SS.action.unique.ss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.EnergizedPower;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import SS.action.common.PlayCardAction;
import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;

public class NutrientCycleAction extends AbstractGameAction {
    private AbstractPlayer p;
    private boolean freeToPlayOnce = false;
    private int energyOnUse = -1;
    private boolean upgraded = false;

    public NutrientCycleAction(AbstractPlayer p, boolean freeToPlayOnce, int energyOnUse, boolean upgraded) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.freeToPlayOnce = freeToPlayOnce;
        this.upgraded = upgraded;
        this.duration = Settings.ACTION_DUR_XFAST;
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        this.energyOnUse = energyOnUse;
    }

    public void update() {
        int effect = EnergyPanel.totalCount;
        if (this.energyOnUse != -1) {
            effect = this.energyOnUse;
        }

        if (this.p.hasRelic("Chemical X")) {
            effect += 2;
            this.p.getRelic("Chemical X").flash();
        }
        if (!this.freeToPlayOnce) {
            this.p.energy.use(EnergyPanel.totalCount);
        }
        if (this.upgraded) {
            effect++;
        }
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
        addToBot(new ApplyPowerAction(p, p, new EnergizedPower(p, effect), effect));
        this.isDone = true;
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
}
