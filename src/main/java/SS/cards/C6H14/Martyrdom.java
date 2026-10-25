package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.monster.EvokeSoulAction;
import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;

public class Martyrdom extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Martyrdom");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Martyrdom.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Martyrdom() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
        exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int amount = 0;
        java.util.ArrayList<AbstractMonster> souls = new java.util.ArrayList<>();
        for (AbstractMonster mo : AllyManager.allies.monsters) {
            if (mo instanceof SoulAlly && !mo.isDeadOrEscaped()) {
                SoulAlly s = (SoulAlly) mo;
                amount += s.currentHealth;
                souls.add(s);
            }
        }
        if (amount > 0) {
            addToBot(new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(p, p,
                    new com.megacrit.cardcrawl.powers.ThornsPower(p, amount), amount));
        }
        for (AbstractMonster soul : souls) {
            com.megacrit.cardcrawl.cards.DamageInfo info = new com.megacrit.cardcrawl.cards.DamageInfo(p, 99, DamageType.THORNS);
            SS.patches.AllyDamagePatch.allowFriendlyFire.set(info, true);
            addToBot(new com.megacrit.cardcrawl.actions.common.DamageAction(soul,
                    info, AttackEffect.FIRE));
        }
        if (needManager()) {
            addToBot(new EvokeSoulAction(1));
        }
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.COMMAND_ALLY;
    }

    public AbstractDoubleCard makeCopy() {
        return new Martyrdom();
    }
}
