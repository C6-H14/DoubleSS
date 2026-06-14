package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.BacktrackingPower;

public class Backtracking extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Backtracking");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_power.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Backtracking() {
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
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        selfPower(new BacktrackingPower(p, this.magicNumber));
        if (needManager()) {
            ModHelper.atbLambda(() -> {
                for (int i = AllyManager.allies.monsters.size() - 1; i >= 0; i--) {
                    AbstractMonster mo = AllyManager.allies.monsters.get(i);

                    // 判断是否是活着的魂火
                    if (mo instanceof SoulAlly && !mo.isDeadOrEscaped()) {
                        SoulAlly ally = (SoulAlly) mo;
                        addToTop(new ApplyPowerAction(ally, p, new StrengthPower(ally, 3), 3));
                    }
                }
            });
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Backtracking();
    }
}
