package SS.cards.C6H14;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.InstantKillAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.GiantTextEffect;
import com.megacrit.cardcrawl.vfx.combat.WeightyImpactEffect;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class FinalJudgement extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("FinalJudgement");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public FinalJudgement() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(2);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (mo.currentHealth <= getVirtue() * this.magicNumber) {
                this.addToBot(new VFXAction(new WeightyImpactEffect(mo.hb.cX, mo.hb.cY, Color.GOLD.cpy())));
                this.addToBot(new WaitAction(0.8F));
                this.addToBot(new VFXAction(new GiantTextEffect(mo.hb.cX, mo.hb.cY)));
                addToBot(new InstantKillAction(mo));
            }
        }
    }

    @Override
    public void updateManager() {
        super.updateManager();
        upgradeMagicNumber(1);
    }

    @Override
    public void exitManager() {
        super.exitManager();
        upgradeMagicNumber(-1);
    }

    public AbstractDoubleCard makeCopy() {
        return new FinalJudgement();
    }
}
