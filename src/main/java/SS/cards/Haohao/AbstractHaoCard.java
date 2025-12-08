package SS.cards.Haohao;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.localization.CardStrings;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;

public abstract class AbstractHaoCard extends AbstractDoubleCard {

    public AbstractHaoCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractHaoCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractHaoCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, false, true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractHaoCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, false, true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractHaoCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, fiend, manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractHaoCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, fiend, manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Manager();
    }
}
