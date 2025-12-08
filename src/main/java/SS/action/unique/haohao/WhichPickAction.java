package SS.action.unique.haohao;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardRarity;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;

import SS.action.common.EchoACardAction;

public class WhichPickAction extends AbstractGameAction {
    private int common;
    private int uncommon;
    private boolean upgrade;
    private boolean freeToPlay;

    public WhichPickAction(int amount, int c, int u, boolean upgrade, boolean freeToPlay) {
        this.duration = 0.25F;
        this.common = c;
        this.uncommon = u;
        this.amount = amount;
        this.upgrade = upgrade;
        this.freeToPlay = freeToPlay;
    }

    public void update() {
        for (int i = 0; i < this.amount; ++i) {
            addToTop(new EchoACardAction(generateCard(), this.freeToPlay));
        }
        this.isDone = true;
    }

    private AbstractCard generateCard() {
        int roll = AbstractDungeon.cardRandomRng.random(99);
        AbstractCard.CardRarity cardRarity;
        if (roll < this.common) {
            cardRarity = CardRarity.COMMON;
        } else if (roll < this.uncommon) {
            cardRarity = CardRarity.UNCOMMON;
        } else {
            cardRarity = CardRarity.RARE;
        }
        AbstractCard c = CardLibrary.getAnyColorCard(cardRarity);
        if (this.upgrade)
            c.upgrade();
        return c;
    }
}
