package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.unique.c6h14.EraserAction;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower;
import basemod.helpers.CardModifierManager;

public class Eraser extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Eraser");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Eraser() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.exhaust = true;
        setMagic(5);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
        CardModifierManager.addModifier(this, new PaintingModifier());
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        ModHelper.atbLambda(() -> {
            for (AbstractMonster mo : AllyManager.allies.monsters) {
                if (mo instanceof SoulAlly) {
                    SoulAlly s = (SoulAlly) mo;
                    if (s.hasPower("Double:InscribeCardPower")) {
                        InscribeCardPower power = (InscribeCardPower) s.getPower("Double:InscribeCardPower");
                        if (power.card != null) {
                            power.card = null;
                            power.updateDescription();
                        }
                    }
                }
            }
        });
        addToBot(new EraserAction(this.upgraded));
        if (needManager()) {
            if (getVirtue() >= 5) {
                addToBot(new MakeTempCardInDiscardAction(this, 1));
                addVirtue(-5);
            }
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Eraser();
    }
}
