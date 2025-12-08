package SS.cards;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.GainStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.evacipated.cardcrawl.mod.stslib.fields.cards.AbstractCard.ExhaustiveField;

import SS.action.common.ConjugateCardInHandAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.CrimePower;

public class HedonismShackles extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("HedonismShackles");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/HedonismShackles.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public HedonismShackles() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.magicNumber = this.baseMagicNumber = 2;
        ExhaustiveField.ExhaustiveFields.baseExhaustive.set(this, Integer.valueOf(2));
        ExhaustiveField.ExhaustiveFields.exhaustive.set(this, Integer.valueOf(2));
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Lust);
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(-1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            addToBot(new ApplyPowerAction(mo, p,
                    (AbstractPower) new StrengthPower(mo, -1), -1, true, AbstractGameAction.AttackEffect.NONE));
        }
        if (!this.upgraded) {
            for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                if (!mo.hasPower("Artifact")) {
                    addToBot(new ApplyPowerAction(mo, p,
                            (AbstractPower) new GainStrengthPower(mo, 1), 1, true,
                            AbstractGameAction.AttackEffect.NONE));
                }
            }
        }
        addToBot(new ConjugateCardInHandAction(p, m, 2));
        addToBot(new ApplyPowerAction(p, p, new CrimePower(p, this.magicNumber)));
        UpdateExhaustiveDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new HedonismShackles();
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public void updateFiend() {
        this.selfRetain = true;
        UpdateDescription();
        initializeDescription();
    }

    public void exitFiend() {
        this.selfRetain = false;
        UpdateDescription();
        initializeDescription();
    }
}