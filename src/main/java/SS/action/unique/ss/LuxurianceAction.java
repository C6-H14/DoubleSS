package SS.action.unique.ss;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;
import basemod.BaseMod;

public class LuxurianceAction extends AbstractGameAction {
    private AbstractPlayer p;

    public LuxurianceAction(int damage, AbstractCreature target) {
        this.p = AbstractDungeon.player;
        setValues((AbstractCreature) this.p, (AbstractCreature) this.p);
        this.duration = Settings.ACTION_DUR_MED;
        this.amount = damage;
        this.target = target;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            if ((this.p.drawPile.isEmpty() && this.p.discardPile.isEmpty())
                    || this.p.hand.size() >= BaseMod.MAX_HAND_SIZE) {
                addToTop(new ChannelDiceAction(new AttackDice(this.amount, (AbstractMonster) target)));
                this.isDone = true;
                return;
            }
            if (AbstractDungeon.player.hasPower("No Draw")) {
                addToTop(new ChannelDiceAction(new AttackDice(this.amount, (AbstractMonster) target)));

                AbstractDungeon.player.getPower("No Draw").flash();
                this.isDone = true;

                return;
            }
            if (!this.p.drawPile.isEmpty()) {
                AbstractCard c = AbstractDungeon.player.drawPile.group.get(AbstractDungeon.player.drawPile.size() - 1);
                boolean flag = true;
                for (AbstractCard card : AbstractDungeon.player.hand.group) {
                    if (card.color == c.color && card.color != AbstractCardEnum.SS_Yellow
                            || (card.color == AbstractCardEnum.SS_Yellow
                                    && ((AbstractDoubleCard) card).packagetype == ((AbstractDoubleCard) c).packagetype)) {
                        flag = false;
                        break;
                    }
                }
                if (flag)
                    addToTop(new LuxurianceAction(amount, target));
                addToTop(new DrawCardAction(1));
                addToTop(new ChannelDiceAction(new AttackDice(this.amount, (AbstractMonster) target)));
            } else {
                addToTop(new LuxurianceAction(amount, target));
                addToTop(new EmptyDeckShuffleAction());
                addToTop(new ChannelDiceAction(new AttackDice(this.amount, (AbstractMonster) target)));
            }

            this.isDone = true;

            return;
        }
        tickDuration();
    }
}
