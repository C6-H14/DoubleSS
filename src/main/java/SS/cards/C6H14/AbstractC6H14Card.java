package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.localization.CardStrings;

import SS.action.unique.GainVirtueAction;
import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;
import SS.power.PaintingPower;

public abstract class AbstractC6H14Card extends AbstractDoubleCard {

    public AbstractC6H14Card(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
    }

    public AbstractC6H14Card(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.C6H14, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractC6H14Card(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.C6H14, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractC6H14Card(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.C6H14, rarity, target, card_string, exstrings,
                false,
                true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractC6H14Card(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.C6H14, rarity, target, card_string, exstrings,
                false,
                true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractC6H14Card(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.C6H14, rarity, target, card_string, exstrings,
                fiend,
                manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractC6H14Card(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.C6H14, rarity, target, card_string, exstrings,
                fiend, manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    void paint() {
        selfPower(new PaintingPower(getP(), 1));
    }

    protected int getVirtue() {
        return -countPower("Double:SinsPower");
    }

    protected void addVirtue(int amount) {
        addToBot(new GainVirtueAction(getP(), amount));
    }
}
