package SS.cards.BlessCard;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.CardStrings;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;

public abstract class AbstractBlessCard extends AbstractDoubleCard {

    public AbstractBlessCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.tags.add(AbstractCardEnum.Blessed);
    }

    public AbstractBlessCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.tags.add(AbstractCardEnum.Blessed);
    }

    public AbstractBlessCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, false, true);
        this.tags.add(AbstractCardEnum.Blessed);
    }

    public AbstractBlessCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, false, true);
        this.tags.add(AbstractCardEnum.Blessed);
    }

    public AbstractBlessCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, fiend, manager);
        this.tags.add(AbstractCardEnum.Blessed);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractBlessCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, fiend, manager);
        this.tags.add(AbstractCardEnum.Blessed);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public void triggerOnGlowCheck() {
        if (this.hasTag(AbstractCardEnum.Fiend)) {
            triggerOnGlowCheck_Fiend();
        } else if (this.hasTag(AbstractCardEnum.Manager)) {
            triggerOnGlowCheck_Manager();
        }
    }

    public static String blessCardFrom, blessCardUpgrade;

    public AbstractCard getBlessing() {
        return CardLibrary.getCard(blessCardUpgrade).makeCopy();
    }

}
