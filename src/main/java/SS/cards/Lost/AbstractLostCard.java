package SS.cards.Lost;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;

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
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, false, true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, false, true);
        this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target, card_string, exstrings, fiend, manager);
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractLostCard(String id, String name, RegionName img, int cost, String rawDescription,
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

    public void upgradePermanentDamage(int amount) {
        this.basePermanentDamage += amount;
        this.permanentDamage = this.basePermanentDamage;
        this.upgradePermanentDamage = true;
    }

    public void upgradePermanentMagicNumber(int amount) {
        this.basePermanentMagicNumber += amount;
        this.permanentMagicNumber = this.basePermanentMagicNumber;
        this.upgradePermanentMagicNumber = true;
    }

    public void upgradePermanentBlock(int amount) {
        this.basePermanentBlock += amount;
        this.permanentBlock = this.basePermanentBlock;
        this.upgradePermanentBlock = true;
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
