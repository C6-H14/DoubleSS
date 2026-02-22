package SS.action.unique.c6h14;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import SS.cardmodifiers.PaintingModifier;
import basemod.helpers.CardModifierManager;

public class KaleidoscopeAction extends AbstractGameAction {

    public KaleidoscopeAction(int amount) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
    }

    @Override
    public void update() {
        ArrayList<AbstractCard> arr = new ArrayList<>();
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!CardModifierManager.hasModifier(c, (new PaintingModifier()).ID) && c.cost >= -1)
                arr.add(c);
        }
        Collections.shuffle(arr, new Random(AbstractDungeon.cardRandomRng.randomLong()));
        for (AbstractCard c : arr) {
            if (amount <= 0)
                break;
            CardModifierManager.addModifier(c, new PaintingModifier());
            --amount;
        }
        this.isDone = true;
    }
}