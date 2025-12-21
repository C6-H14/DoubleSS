package SS.cards.Shock;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class UnstableShockwave extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("UnstableShockwave");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public UnstableShockwave() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 3;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        if (needManager()) {
            for (int i = 0; i < this.magicNumber; ++i) {
                for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                    if (AbstractDungeon.cardRandomRng.randomBoolean()) {
                        addToBot(new ApplyPowerAction(mo, p, new WeakPower(mo, 1, false),
                                1, true));
                    } else {
                        addToBot(new ApplyPowerAction(mo, p, new VulnerablePower(mo, 1, false),
                                1, true));
                    }
                }
            }
        } else {
            int amount = 0;
            for (int i = 0; i < this.magicNumber; ++i) {
                if (AbstractDungeon.cardRandomRng.randomBoolean())
                    ++amount;
            }
            for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
                addToBot(new ApplyPowerAction(mo, p, new WeakPower(mo, amount, false)));
                addToBot(new ApplyPowerAction(mo, p, new VulnerablePower(mo, this.magicNumber - amount, false)));
            }
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new UnstableShockwave();
    }
}
