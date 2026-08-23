package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.InstantKillAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.monster.EvokeSoulAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower;

public class Martyrdom extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Martyrdom");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Martyrdom.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public Martyrdom() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int amount = 0;
        for (AbstractMonster mo : AllyManager.allies.monsters) {
            if (mo instanceof SoulAlly) {
                SoulAlly s = (SoulAlly) mo;
                amount += s.currentHealth;
                if (s.hasPower("Double:InscribeCardPower")) {
                    InscribeCardPower power = (InscribeCardPower) s.getPower("Double:InscribeCardPower");
                    if (power.card != null) {
                        power.card = null;
                        power.updateDescription();
                    }
                }
                addToBot(new InstantKillAction(mo));
            }
        }
        addToBot(new DamageAllEnemiesAction(p, amount, DamageType.NORMAL, AttackEffect.SMASH));
        if (needManager()) {
            addToBot(new EvokeSoulAction(1));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Martyrdom();
    }
}
