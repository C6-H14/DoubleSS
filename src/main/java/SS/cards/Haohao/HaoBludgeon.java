package SS.cards.Haohao;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackHaoDice;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class HaoBludgeon extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("HaoBludgeon");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/HaoBludgeon.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;
    private boolean flag = false;

    public HaoBludgeon() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.damage = this.baseDamage = 2;
        this.magicNumber = this.baseMagicNumber = 3;
        flag = false;
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

    public void updateManager() {
        upgradeDamage(1);
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        upgradeDamage(-1);
        UpdateDescription();
        initializeDescription();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.magicNumber; ++i) {
            addToBot(new ChannelAction(new AttackHaoDice(this.damage, m)));
        }
        if (!flag) {
            addToBot(new GainEnergyAction(3));
        }
        flag = true;
    }

    public AbstractDoubleCard makeCopy() {
        return new HaoBludgeon();
    }
}
