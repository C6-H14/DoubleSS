package SS.cards;

import com.evacipated.cardcrawl.mod.stslib.patches.core.AbstractCreature.TempHPField;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.unique.KindnessAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Kindness extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Kindness");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Kindness.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCard.CardColor.COLORLESS;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Kindness() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.magicNumber = this.baseMagicNumber = 6;
        this.exhaust = true;
        this.tags.add(AbstractCardEnum.Virtues);
        this.tags.add(AbstractCardEnum.Kindness);
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(-1);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int amount = p.currentHealth + ((Integer) TempHPField.tempHp.get(AbstractDungeon.player)).intValue();
        addToBot(new LoseHPAction(p, p, amount - 1));
        addToBot(new KindnessAction(p, amount, this.magicNumber));
    }

    public AbstractDoubleCard makeCopy() {
        return new Kindness();
    }
}