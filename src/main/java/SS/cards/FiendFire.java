package SS.cards;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.unique.ss.DoubleFiendFireAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class FiendFire extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("FiendFire");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/fiend_fire_attack.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public FiendFire() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, true);
        this.baseDamage = 7;
        this.exhaust = true;
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Manager);
        if (needFiend()) {
            updateFiend();
        }
        if (needManager()) {
            updateManager();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot((AbstractGameAction) new DoubleFiendFireAction((AbstractCreature) m, this.damage));
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(3);
            UpdateDescription();
            initializeDescription();
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new FiendFire();
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

    public void updateManager() {
        this.baseDamage += 2;
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        this.baseDamage -= 2;
        UpdateDescription();
        initializeDescription();
    }
}