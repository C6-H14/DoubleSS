package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class Hymn extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Hymn");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Hymn.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public Hymn() {
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
            upgradeBaseCost(0);
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction(p,
                Math.max(0, getVirtue()), com.megacrit.cardcrawl.cards.DamageInfo.DamageType.THORNS,
                com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.FIRE));
        if (needManager()) addVirtue(magicNumber);
    }

    @Override
    public void updateManager() {
        super.updateManager();
    }

    @Override
    public void exitManager() {
        super.exitManager();
    }

    public AbstractDoubleCard makeCopy() {
        return new Hymn();
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.CAST_PRAYER;
    }
}
