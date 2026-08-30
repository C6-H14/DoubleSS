package SS.action.unique.haohao;

import java.util.Map;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardRarity;
import com.megacrit.cardcrawl.cards.AbstractCard.CardTags;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.CardGroup.CardGroupType;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

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
        AbstractCard c = getCard(cardRarity);
        if (this.upgrade)
            c.upgrade();
        return c;
    }

    public static AbstractCard getCard(AbstractCard.CardRarity rarity) {
        CardGroup anyCard = new CardGroup(CardGroupType.UNSPECIFIED);

        for (Map.Entry<String, AbstractCard> c : CardLibrary.cards.entrySet()) {
            if (((AbstractCard) c.getValue()).rarity == rarity
                    && !((AbstractCard) c.getValue()).hasTag(CardTags.HEALING)
                    && ((AbstractCard) c.getValue()).type != CardType.CURSE
                    && ((AbstractCard) c.getValue()).type != CardType.STATUS
                    && (!UnlockTracker.isCardLocked((String) c.getKey()) || Settings.treatEverythingAsUnlocked())) {
                anyCard.addToBottom((AbstractCard) c.getValue());
            }
        }

        anyCard.shuffle(AbstractDungeon.cardRandomRng);
        return anyCard.getRandomCard(true, rarity);
    }
}
