package SS.cards;

import java.util.HashMap;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.action.unique.ss.LuxurianceAction;
import SS.animation.CharacterAnimationController;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.RerollPower;

public class Luxuriance extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Luxuriance");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Luxuriance.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Luxuriance() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        setDamage(9);
        setMagic(1);
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(2);
            upgradeMagicNumber(1);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        dmgAct(m, damage);
        selfPower(new RerollPower(p, magicNumber));
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.CAST_ATTACK_1;
    }

    public AbstractDoubleCard makeCopy() {
        return new Luxuriance();
    }
}