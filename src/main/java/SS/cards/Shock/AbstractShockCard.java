package SS.cards.Shock;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.localization.CardStrings;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;

public abstract class AbstractShockCard extends AbstractDoubleCard {

    public AbstractShockCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
    }

    public AbstractShockCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Shock, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractShockCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Shock, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractShockCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Shock, rarity, target, card_string, exstrings,
                false,
                true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractShockCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Shock, rarity, target, card_string, exstrings,
                false,
                true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractShockCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Shock, rarity, target, card_string, exstrings,
                fiend,
                manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractShockCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardRarity rarity, AbstractCard.CardTarget target,
            CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Shock, rarity, target, card_string, exstrings,
                fiend, manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    /**
     * 进入/退出群主身份时同步状态位。
     *
     * <p>基类 {@code AbstractDoubleCard.updateManager()} 只刷新描述、不改 {@code isManager}，
     * 所以这里必须自己置位，否则 {@code UpdateDescription()} 里
     * {@code !isFiend && isManager} 那条分支永远进不去，牌面会一直停在 DESCRIPTION 上。
     */
    @Override
    public void updateManager() {
        this.isManager = true;
        super.updateManager();
    }

    @Override
    public void exitManager() {
        this.isManager = false;
        super.exitManager();
    }

    protected int getShock(AbstractCreature m) {
        int amount = 0;
        if (m.hasPower("Vulnerable") && m.hasPower("Weakened")) {
            amount = Math.min(m.getPower("Vulnerable").amount, m.getPower("Weakened").amount);
        }
        return amount;
    }
}
