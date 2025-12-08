package SS.action.unique.haohao;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardRarity;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import java.util.ArrayList;

public class DishaoveryAction
        extends AbstractGameAction {
    private boolean retrieveCard = false;
    private boolean upgraded;

    public DishaoveryAction(boolean upgraded) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.upgraded = upgraded;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.customCombatOpen(generateCardChoices(), CardRewardScreen.TEXT[1], true);
            tickDuration();

            return;
        }
        if (!this.retrieveCard) {
            if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                AbstractCard disCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                if (this.upgraded) {
                    disCard.setCostForTurn(0);
                }
                disCard.current_x = -1000.0F * Settings.xScale;
                if (AbstractDungeon.player.hand.size() < 10) {
                    AbstractDungeon.effectList.add(
                            new ShowCardAndAddToHandEffect(disCard, Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
                } else {

                    AbstractDungeon.effectList.add(
                            new ShowCardAndAddToDiscardEffect(disCard, Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
                }

                AbstractDungeon.cardRewardScreen.discoveryCard = null;
            }
            this.retrieveCard = true;
        }

        tickDuration();
    }

    private ArrayList<AbstractCard> generateCardChoices() {
        ArrayList<AbstractCard> derp = new ArrayList<>();

        while (derp.size() != 3) {
            boolean dupe = false;
            int roll = AbstractDungeon.cardRandomRng.random(99);
            AbstractCard.CardRarity cardRarity;
            if (roll < 55) {
                cardRarity = CardRarity.COMMON;
            } else if (roll < 85) {
                cardRarity = CardRarity.UNCOMMON;
            } else {
                cardRarity = CardRarity.RARE;
            }
            AbstractCard tmp = CardLibrary.getAnyColorCard(cardRarity);
            while (tmp.color != AbstractCard.CardColor.RED) {
                tmp = CardLibrary.getAnyColorCard(cardRarity);
            }
            for (AbstractCard c : derp) {
                if (c.cardID.equals(tmp.cardID)) {
                    dupe = true;
                    break;
                }
            }
            if (!dupe) {
                derp.add(tmp.makeCopy());
            }
        }

        return derp;
    }
}
