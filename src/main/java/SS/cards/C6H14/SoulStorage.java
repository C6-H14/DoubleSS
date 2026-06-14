package SS.cards.C6H14;

import com.evacipated.cardcrawl.mod.stslib.fields.cards.AbstractCard.ExhaustiveField;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;

public class SoulStorage extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("SoulStorage");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public SoulStorage() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setDamage(6);
        ExhaustiveField.ExhaustiveFields.baseExhaustive.set(this, Integer.valueOf(2));
        ExhaustiveField.ExhaustiveFields.exhaustive.set(this, Integer.valueOf(2));
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            ExhaustiveField.ExhaustiveFields.baseExhaustive.set(this,
                    ExhaustiveField.ExhaustiveFields.baseExhaustive.get(this) + 1);
            ExhaustiveField.ExhaustiveFields.exhaustive.set(this,
                    ExhaustiveField.ExhaustiveFields.exhaustive.get(this) + 1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (AbstractMonster mo : AllyManager.allies.monsters) {
            if (mo instanceof SoulAlly) {
                DamageInfo info = new DamageInfo(p, this.damage, DamageType.NORMAL);
                SS.patches.AllyDamagePatch.allowFriendlyFire.set(info, true);
                addToBot(new DamageAction(mo, info));
            }
        }
        for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
            dmgAct(mo, this.damage, DamageType.NORMAL);
        }
        ModHelper.atbLambda(() -> {
            int cnt = 0;
            for (AbstractMonster mo : AllyManager.allies.monsters) {
                if (mo instanceof SoulAlly) {
                    if (mo.hasPower("Double:SoulFirePower")) {
                        cnt = Math.max(cnt, mo.getPower("Double:SoulFirePower").amount);
                    }
                }
            }
            addToBot(new GainEnergyAction(cnt));
        });
        if (needManager()) {
            for (AbstractMonster mo : AllyManager.allies.monsters) {
                if (mo instanceof SoulAlly) {
                    addToBot(new GainBlockAction(mo, 6));
                }
            }
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new SoulStorage();
    }
}
