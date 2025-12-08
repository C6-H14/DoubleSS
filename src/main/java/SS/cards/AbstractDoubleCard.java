package SS.cards;

import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.mod.stslib.fields.cards.AbstractCard.ExhaustiveField;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;

import SS.action.PlaySpecificCardInDrawPile;
import SS.action.PlaySpecificCardInHand;
import SS.action.RemoveConjugateModifierAction;
import SS.path.AbstractCardEnum;
import basemod.abstracts.CustomCard;

public abstract class AbstractDoubleCard extends CustomCard {
    public AbstractDoubleCard fatherCard = this;
    protected CardStrings CARD_STRINGS;
    protected String[] EXTENDED_DESCRIPTION = { "", "", "", "", "", "" };
    protected boolean isFiend = false, isManager = false;

    public AbstractDoubleCard findFather() {
        if (this.fatherCard == null)
            this.fatherCard = this;
        if (this.fatherCard == this)
            return this;
        this.fatherCard = this.fatherCard.findFather();
        return this.fatherCard;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
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
