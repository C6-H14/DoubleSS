package SS.cards.Lost;

import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;

public abstract class AbstractLostCard extends AbstractDoubleCard {
    public int permanentDamage = 0;
    public int permanentBlock = 0;
    public int permanentMagicNumber = 0;
    public int basePermanentDamage = 0;
    public int basePermanentBlock = 0;
    public int basePermanentMagicNumber = 0;
    public boolean upgradePermanentDamage = false;
    public boolean upgradePermanentBlock = false;
    public boolean upgradePermanentMagicNumber = false;

    public AbstractLostCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Lost, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Lost, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Lost, rarity, target, card_string, exstrings,
                false, true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Lost, rarity, target, card_string, exstrings,
                false, true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Lost, rarity, target, card_string, exstrings,
                fiend, manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, PackageEnum.Lost, rarity, target, card_string, exstrings,
                fiend, manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Manager();
    }

    public void upgradePermanentDamage(int amount) {
        // 0 也要拦：Godhead/Perseverance 在玩家没有奄息时会以 amount=0 调用，
        // 若仍置位 upgradePermanent* 布尔，数值没变却会被渲染色判定为"已强化"变绿
        if (amount == 0)
            return;
        this.basePermanentDamage += amount;
        this.permanentDamage = this.basePermanentDamage;
        this.upgradePermanentDamage = true;
    }

    public void upgradePermanentMagicNumber(int amount) {
        if (amount == 0)
            return;
        this.basePermanentMagicNumber += amount;
        this.permanentMagicNumber = this.basePermanentMagicNumber;
        this.upgradePermanentMagicNumber = true;
    }

    public void upgradePermanentBlock(int amount) {
        if (amount == 0)
            return;
        this.basePermanentBlock += amount;
        this.permanentBlock = this.basePermanentBlock;
        this.upgradePermanentBlock = true;
    }

    /**
     * 把 9 个 permanent 系列字段从 this 拷到目标卡。
     * Lost 卡的 makeCopy() 是全新实例，permanent 字段会回退到构造函数初值；
     * 战斗内 Perseverance 等强化过之后，new 完必须调这个方法补拷，
     * 否则副本丢强化值。upgradePermanent* 布尔也要拷，描述文本的升级色判定依赖它们。
     */
    protected void copyPermanentFieldsFrom(AbstractLostCard c) {
        c.permanentDamage = this.permanentDamage;
        c.permanentBlock = this.permanentBlock;
        c.permanentMagicNumber = this.permanentMagicNumber;
        c.basePermanentDamage = this.basePermanentDamage;
        c.basePermanentBlock = this.basePermanentBlock;
        c.basePermanentMagicNumber = this.basePermanentMagicNumber;
        c.upgradePermanentDamage = this.upgradePermanentDamage;
        c.upgradePermanentBlock = this.upgradePermanentBlock;
        c.upgradePermanentMagicNumber = this.upgradePermanentMagicNumber;
    }

    /**
     * 原版的 makeStatEquivalentCopy 只复制 baseDamage/baseBlock/baseMagicNumber 等原版字段，
     * 各 Lost 卡的 makeCopy() 是全新实例，permanent 系列字段会全部回退到构造函数初值。
     * 战斗内 Perseverance 强化过 Permanent 卡后，triggerOnExhaust 的 makeSameInstanceOf
     * （→ makeStatEquivalentCopy → makeCopy）产生的 limbo 副本就会丢强化值。
     * 这里在 super 之后补拷；各 Lost 卡的 makeCopy() 内部 new 完也会先调 copyPermanentFieldsFrom。
     */
    @Override
    public AbstractCard makeStatEquivalentCopy() {
        AbstractCard card = super.makeStatEquivalentCopy();
        if (card instanceof AbstractLostCard) {
            ((AbstractLostCard) card).copyPermanentFieldsFrom(this);
        }
        return card;
    }

    public void triggerOnExhaust() {
        if (this.tags.contains(AbstractCardEnum.Permanent)) {
            AbstractCard tmp = this.makeSameInstanceOf();
            AbstractDungeon.player.limbo.addToBottom(tmp);
            tmp.dontTriggerOnUseCard = true;
            tmp.current_x = this.current_x;
            tmp.current_y = this.current_y;
            tmp.target_x = (float) Settings.WIDTH / 2.0F - 300.0F * Settings.scale;
            tmp.target_y = (float) Settings.HEIGHT / 2.0F;

            tmp.purgeOnUse = true;
            AbstractDungeon.actionManager.addCardQueueItem(new CardQueueItem(tmp, true, this.energyOnUse, true, true),
                    true);
            if (!Settings.FAST_MODE) {
                addToTop(new WaitAction(Settings.ACTION_DUR_MED));
            } else {
                addToTop(new WaitAction(Settings.ACTION_DUR_FASTER));
            }
        }
    }
}
