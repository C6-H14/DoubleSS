package SS.cards;

import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.mod.stslib.fields.cards.AbstractCard.ExhaustiveField;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.helper.CardUtil;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;
import basemod.abstracts.CustomCard;

public abstract class AbstractDoubleCard extends CustomCard {
    public AbstractDoubleCard fatherCard = this;
    protected CardStrings CARD_STRINGS;
    protected String[] EXTENDED_DESCRIPTION = { "", "", "", "", "", "" };
    protected boolean isFiend = false, isManager = false;
    public PackageEnum packagetype;

    public AbstractDoubleCard findFather() {
        if (this.fatherCard == null)
            this.fatherCard = this;
        if (this.fatherCard == this)
            return this;
        this.fatherCard = this.fatherCard.findFather();
        return this.fatherCard;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    private void repaint() {
        if (this.packagetype == PackageEnum.Default || this.packagetype == PackageEnum.SS) {
            return;
        }
        if (this.packagetype == PackageEnum.RED || this.packagetype == PackageEnum.GREEN
                || this.packagetype == PackageEnum.BLUE || this.packagetype == PackageEnum.PURPLE) {
            return;
        }
        String s = this.packagetype.toString();
        this.setOrbTexture("img/512/" + s + "_orb.png", "img/1024/" + s + "_orb.png");
        switch (this.type) {
            case ATTACK:
                this.setBackgroundTexture("img/512/" + s + "_attack.png", "img/1024/" + s + "_attack.png");
                break;
            case SKILL:
                this.setBackgroundTexture("img/512/" + s + "_skill.png", "img/1024/" + s + "_skill.png");
                break;
            case POWER:
                this.setBackgroundTexture("img/512/" + s + "_power.png", "img/1024/" + s + "_power.png");
                break;
            default:
                this.setBackgroundTexture("img/512/" + s + "_skill.png", "img/1024/" + s + "_skill.png");
                break;
        }
    }

    protected void setDamage(int amount) {
        this.damage = this.baseDamage = amount;
    }

    protected void setBlock(int amount) {
        this.block = this.baseBlock = amount;
    }

    protected void setMagic(int amount) {
        this.magicNumber = this.baseMagicNumber = amount;
    }

    protected void selfPower(AbstractPower p) {
        addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, p));
    }

    protected void enemyPower(AbstractPower p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(m, m, p));
    }

    protected void allEnemyPower(AbstractPower p) {
        for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            enemyPower(p, mo);
        }
    }

    protected void dmgAct(AbstractMonster m, int amount) {
        addToBot(
                new DamageAction(m, new DamageInfo(AbstractDungeon.player, amount, DamageInfo.DamageType.NORMAL)));
    }

    protected void dmgAct(AbstractMonster m, int amount, DamageType type) {
        addToBot(
                new DamageAction(m, new DamageInfo(AbstractDungeon.player, amount, type)));
    }

    protected void blkAct(int amount) {
        addToBot(new GainBlockAction(AbstractDungeon.player, amount));
    }

    protected void heal(int amount) {
        addToBot(new HealAction(AbstractDungeon.player, AbstractDungeon.player, amount));
    }

    protected void lose(int amount) {
        addToBot(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, amount));
    }

    protected AbstractCreature getUser() {
        return CardUtil.getCardSource();
    }

    protected AbstractPlayer getP() {
        return AbstractDungeon.player;
    }

    protected int countPower(String s) {
        int amount = 0;
        if (getP().hasPower(s)) {
            amount = getP().getPower(s).amount;
        }
        return amount;
    }

    public void downgrade() {
    }

    public boolean canDowngrade() {
        return false;
    }

    public boolean isCardInHand() {
        return AbstractDungeon.player.hand.group.contains(this);
    }

    public boolean isCardInDrawPile() {
        return AbstractDungeon.player.drawPile.group.contains(this);
    }

    public boolean isCardInDiscardPile() {
        return AbstractDungeon.player.discardPile.group.contains(this);
    }

    public boolean isCardInExhaustPile() {
        return AbstractDungeon.player.exhaustPile.group.contains(this);
    }

    public void updateFiend() {
        UpdateDescription();
        initializeDescription();
    }

    public void exitFiend() {
        UpdateDescription();
        initializeDescription();
    }

    public void updateManager() {
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        UpdateDescription();
        initializeDescription();
    }

    public void updateNull() {
        UpdateDescription();
        initializeDescription();
    }

    @Override
    public abstract AbstractDoubleCard makeCopy();

    public void refreshPower() {
    }

    public void triggerOnGlowCheck() {
        if (hasSyn()) {
            triggerOnSyn();
        } else {
            if (this.isFiend) {
                triggerOnGlowCheck_Fiend();
            }
            if (this.isManager) {
                triggerOnGlowCheck_Manager();
            }
        }
    }

    public void triggerOnGlowCheck_Fiend() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        if (AbstractDungeon.player.getPower("Double:FiendStance") != null) {
            this.glowColor = new Color(1.0F, 0.4F, 1.0F, 1.0F);
        }
    }

    public void triggerOnGlowCheck_Manager() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        if (AbstractDungeon.player.getPower("Double:ManagerStance") != null) {
            this.glowColor = AbstractCard.GOLD_BORDER_GLOW_COLOR.cpy();
        }
    }

    public void triggerOnSyn() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        if (hasSyn()) {
            this.glowColor = new Color(1.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    public boolean hasSyn() {
        return false;
    }

    public void UpdateExhaustiveDescription() {
        if (this.purgeOnUse || this.exhaust)
            return;
        this.rawDescription = this.rawDescription.substring(0, this.rawDescription.length() - 1)
                + ExhaustiveField.ExhaustiveFields.exhaustive.get(this);
        initializeDescription();
    }

    public boolean needFiend() {
        return AbstractDungeon.player != null && AbstractDungeon.player.getPower("Double:FiendStance") != null
                && !(AbstractDungeon.getCurrRoom()).isBattleEnding();
    }

    public boolean needManager() {
        return AbstractDungeon.player != null && AbstractDungeon.player.getPower("Double:ManagerStance") != null
                && !(AbstractDungeon.getCurrRoom()).isBattleEnding();
    }

    public void UpdateDescription() {
        UpdateDescription(0, 1, 2, 3, 4, 5);
    }

    public void UpdateDescription(int a, int b, int c, int d, int e, int f) {
        if (!isFiend && !isManager) {
            if (this.upgraded)
                this.rawDescription = this.CARD_STRINGS.UPGRADE_DESCRIPTION;
            else
                this.rawDescription = this.CARD_STRINGS.DESCRIPTION;
            return;
        }
        if (isFiend && !isManager) {
            if (this.upgraded) {
                if (needFiend()) {
                    this.rawDescription = this.EXTENDED_DESCRIPTION[b];
                } else {
                    this.rawDescription = this.CARD_STRINGS.UPGRADE_DESCRIPTION;
                }
            } else {
                if (needFiend()) {
                    this.rawDescription = this.EXTENDED_DESCRIPTION[a];
                } else {
                    this.rawDescription = this.CARD_STRINGS.DESCRIPTION;
                }
            }
            return;
        }
        if (!isFiend && isManager) {
            if (this.upgraded) {
                if (needManager()) {
                    this.rawDescription = this.EXTENDED_DESCRIPTION[b];
                } else {
                    this.rawDescription = this.CARD_STRINGS.UPGRADE_DESCRIPTION;
                }
            } else {
                if (needManager()) {
                    this.rawDescription = this.EXTENDED_DESCRIPTION[a];
                } else {
                    this.rawDescription = this.CARD_STRINGS.DESCRIPTION;
                }
            }
            return;
        }
        if (isFiend && isManager) {
            if (this.upgraded) {
                if (needFiend()) {
                    if (needManager()) {
                        this.rawDescription = this.EXTENDED_DESCRIPTION[f];
                    } else {
                        this.rawDescription = this.EXTENDED_DESCRIPTION[b];
                    }
                } else {
                    if (needManager()) {
                        this.rawDescription = this.EXTENDED_DESCRIPTION[d];
                    } else {
                        this.rawDescription = this.CARD_STRINGS.UPGRADE_DESCRIPTION;
                    }
                }
            } else {
                if (needFiend()) {
                    if (needManager()) {
                        this.rawDescription = this.EXTENDED_DESCRIPTION[e];
                    } else {
                        this.rawDescription = this.EXTENDED_DESCRIPTION[a];
                    }
                } else {
                    if (needManager()) {
                        this.rawDescription = this.EXTENDED_DESCRIPTION[c];
                    } else {
                        this.rawDescription = this.CARD_STRINGS.DESCRIPTION;
                    }
                }
            }
        }
    }
}
