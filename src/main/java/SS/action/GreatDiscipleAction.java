package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.AbstractGameAction.ActionType;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import SS.Dice.AttackDice;
import SS.Dice.AttackHaoDice;
import SS.Dice.PeptideDice;
import SS.action.ChannelDiceAction;
import SS.cards.PeptideStrike;
import SS.power.ContemptPower;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GreatDiscipleAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    private AbstractPlayer p;

    public GreatDiscipleAction(int amount) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
    }

    public void update() {
        if (this.p.hand.isEmpty()) {
            this.isDone = true;
        } else if (this.p.hand.size() == 1) {
            if (this.p.hand.getBottomCard().costForTurn == -1) {
                for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                    for (int i = 0; i < EnergyPanel.getCurrentEnergy(); ++i) {
                        addToTop(new ChannelDiceAction(new AttackHaoDice(this.amount, mo)));
                    }
                }
            } else if (this.p.hand.getBottomCard().costForTurn > 0) {
                for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                    for (int i = 0; i < this.p.hand.getBottomCard().costForTurn; ++i) {
                        addToTop(new ChannelDiceAction(new AttackHaoDice(this.amount, mo)));
                    }
                }
            }

            this.p.hand.moveToExhaustPile(this.p.hand.getBottomCard());
            this.tickDuration();
        } else {
            ArrayList<AbstractCard> cardList = new ArrayList<>();
            int max = 999;
            for (AbstractCard q : AbstractDungeon.player.hand.group) {

                if (q.costForTurn < max && q.costForTurn >= -1 && !q.isEthereal && !q.selfRetain) {

                    cardList.clear();
                    cardList.add(q);
                    max = q.costForTurn;
                    continue;
                }
                if (q.costForTurn == max && q.costForTurn >= -1 && !q.isEthereal && !q.selfRetain) {
                    cardList.add(q);
                }
            }
            if (!cardList.isEmpty()) {
                AbstractCard c = getRandomItem(cardList);
                if (c.costForTurn == -1) {
                    for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                        for (int i = 0; i < EnergyPanel.getCurrentEnergy(); ++i) {
                            addToTop(new ChannelDiceAction(new AttackHaoDice(this.amount, mo)));
                        }
                    }
                } else if (c.costForTurn > 0) {
                    for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                        for (int i = 0; i < c.costForTurn; ++i) {
                            addToTop(new ChannelDiceAction(new AttackHaoDice(this.amount, mo)));
                        }
                    }
                }

                this.p.hand.moveToExhaustPile(c);
                this.isDone = true;
            }
        }
    }

    public static <T> T getRandomItem(List<T> list, Random rng) {
        return list.isEmpty() ? null : list.get(rng.random(list.size() - 1));
    }

    public static <T> T getRandomItem(List<T> list) {
        return getRandomItem(list, AbstractDungeon.cardRandomRng);
    }

    public static AbstractCard getRandomItem(CardGroup group, Random rng) {
        return getRandomItem(group.group, rng);
    }

    public static AbstractCard getRandomItem(CardGroup group) {
        return getRandomItem(group, AbstractDungeon.cardRandomRng);
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString("RecycleAction");
        TEXT = uiStrings.TEXT;
    }
}
