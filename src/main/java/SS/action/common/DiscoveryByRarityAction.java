package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import java.util.ArrayList;
import java.util.Iterator;

public class DiscoveryByRarityAction extends AbstractGameAction {
    private boolean retrieveCard = false;
    private boolean returnColorless = false;
    private AbstractCard.CardType cardType = null;
    private AbstractCard.CardRarity rarity = null;

    public DiscoveryByRarityAction() {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = 1;
    }

    public DiscoveryByRarityAction(AbstractCard.CardRarity rarity, int amount) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
        this.rarity = rarity;
    }

    public void update() {
        ArrayList generatedCards;
        generatedCards = this.generateCardChoices(this.rarity);

        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.customCombatOpen(generatedCards, CardRewardScreen.TEXT[1],
                    this.cardType != null);
            this.tickDuration();
        } else {
            if (!this.retrieveCard) {
                if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                    AbstractCard disCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                    AbstractCard disCard2 = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                    if (AbstractDungeon.player.hasPower("MasterRealityPower")) {
                        disCard.upgrade();
                        disCard2.upgrade();
                    }

                    disCard.setCostForTurn(0);
                    disCard2.setCostForTurn(0);
                    disCard.current_x = -1000.0F * Settings.xScale;
                    disCard2.current_x = -1000.0F * Settings.xScale + AbstractCard.IMG_HEIGHT_S;
                    if (this.amount == 1) {
                        if (AbstractDungeon.player.hand.size() < 10) {
                            AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard,
                                    (float) Settings.WIDTH / 2.0F, (float) Settings.HEIGHT / 2.0F));
                        } else {
                            AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard,
                                    (float) Settings.WIDTH / 2.0F, (float) Settings.HEIGHT / 2.0F));
                        }

                        disCard2 = null;
                    } else if (AbstractDungeon.player.hand.size() + this.amount <= 10) {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard,
                                (float) Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F,
                                (float) Settings.HEIGHT / 2.0F));
                        AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard2,
                                (float) Settings.WIDTH / 2.0F + AbstractCard.IMG_WIDTH / 2.0F,
                                (float) Settings.HEIGHT / 2.0F));
                    } else if (AbstractDungeon.player.hand.size() == 9) {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard,
                                (float) Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F,
                                (float) Settings.HEIGHT / 2.0F));
                        AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard2,
                                (float) Settings.WIDTH / 2.0F + AbstractCard.IMG_WIDTH / 2.0F,
                                (float) Settings.HEIGHT / 2.0F));
                    } else {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard,
                                (float) Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F,
                                (float) Settings.HEIGHT / 2.0F));
                        AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard2,
                                (float) Settings.WIDTH / 2.0F + AbstractCard.IMG_WIDTH / 2.0F,
                                (float) Settings.HEIGHT / 2.0F));
                    }

                    AbstractDungeon.cardRewardScreen.discoveryCard = null;
                }

                this.retrieveCard = true;
            }

            this.tickDuration();
        }
    }

    private ArrayList<AbstractCard> generateColorlessCardChoices() {
        ArrayList<AbstractCard> derp = new ArrayList();

        while (derp.size() != 3) {
            boolean dupe = false;
            AbstractCard tmp = AbstractDungeon.returnTrulyRandomColorlessCardInCombat();
            Iterator var4 = derp.iterator();

            while (var4.hasNext()) {
                AbstractCard c = (AbstractCard) var4.next();
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

    private ArrayList<AbstractCard> generateCardChoices(AbstractCard.CardRarity rarity) {
        ArrayList<AbstractCard> derp = new ArrayList();

        while (derp.size() != 3) {
            boolean dupe = false;
            AbstractCard tmp = null;
            if (rarity == null) {
                tmp = AbstractDungeon.returnTrulyRandomCardInCombat();
            } else {
                tmp = this.returnTrulyRandomCardInCombat(rarity);
            }

            Iterator var5 = derp.iterator();

            while (var5.hasNext()) {
                AbstractCard c = (AbstractCard) var5.next();
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

    public static AbstractCard returnTrulyRandomCardInCombat(AbstractCard.CardRarity rarity) {
        ArrayList<AbstractCard> list = new ArrayList();
        Iterator var2 = AbstractDungeon.srcCommonCardPool.group.iterator();

        AbstractCard c;
        while (var2.hasNext()) {
            c = (AbstractCard) var2.next();
            if (c.rarity == rarity && !c.hasTag(AbstractCard.CardTags.HEALING)) {
                list.add(c);
            }
        }

        var2 = AbstractDungeon.srcUncommonCardPool.group.iterator();

        while (var2.hasNext()) {
            c = (AbstractCard) var2.next();
            if (c.rarity == rarity && !c.hasTag(AbstractCard.CardTags.HEALING)) {
                list.add(c);
            }
        }

        var2 = AbstractDungeon.srcRareCardPool.group.iterator();

        while (var2.hasNext()) {
            c = (AbstractCard) var2.next();
            if (c.rarity == rarity && !c.hasTag(AbstractCard.CardTags.HEALING)) {
                list.add(c);
            }
        }

        return (AbstractCard) list.get(AbstractDungeon.cardRandomRng.random(list.size() - 1));
    }

}
