package SS.action;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.PlayTopCardAction;
import com.megacrit.cardcrawl.actions.watcher.PressEndTurnButtonAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class TrumpCardAction extends AbstractGameAction {
    public int heal;
    private DamageInfo info;
    private AbstractPlayer p;

    public TrumpCardAction(AbstractPlayer p) {
        this.p = p;
    }

    public void update() {
        this.amount = this.p.drawPile.size();
        for (int i = 0; i < this.amount; i++) {
            addToTop(new PlayTopCardAction((AbstractDungeon.getCurrRoom()).monsters.getRandomMonster(null, true,
                    AbstractDungeon.cardRandomRng), false));
        }
        this.isDone = true;
    }
}