package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.SinsPower;

public class RecordingTeam extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("RecordingTeam");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/RecordingTeam.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.BASIC;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public RecordingTeam() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.magicNumber = this.baseMagicNumber = 1;
        this.damage = this.baseDamage = 2;
        this.cardsToPreview = new MultiFacial();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            upgradeDamage(1);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        if (p.getPower("Double:SinsPower") != null && p.getPower("Double:SinsPower").amount > this.magicNumber) {
            addToBot(new ApplyPowerAction(p, p, new SinsPower(p, -this.magicNumber), -this.magicNumber));
        } else {
            addToTop(new RemoveSpecificPowerAction(p, p, "Double:SinsPower"));
        }
        addToBot(new MakeTempCardInHandAction(this.cardsToPreview, this.magicNumber));
        for (int i = 0; i < this.magicNumber; ++i) {
            addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new RecordingTeam();
    }
}