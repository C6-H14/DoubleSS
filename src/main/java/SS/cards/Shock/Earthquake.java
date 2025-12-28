package SS.cards.Shock;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class Earthquake extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("Earthquake");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 5;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Earthquake() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.damage = this.baseDamage = 6;
        this.magicNumber = this.baseMagicNumber = 4;
        if (needManager()) {
            updateManager();
        }
        if (CardCrawlGame.dungeon != null && AbstractDungeon.currMapNode != null
                && (AbstractDungeon.getCurrRoom()).phase == (AbstractRoom.RoomPhase.COMBAT)) {
            refreshPower();
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

    public void triggerWhenDrawn() {
        super.triggerWhenDrawn();
        setCostForTurn(COST - calculateCost());
    }

    @Override
    public void refreshPower() {
        System.out.println("Modifying cost" + (COST - calculateCost()));
        setCostForTurn(COST - calculateCost());
    }

    public int calculateCost() {
        int amount = -1;
        for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            if (amount == -1) {
                amount = getShock(mo);
            } else {
                amount = Math.min(amount, getShock(mo));
            }
        }
        return Math.max(0, amount);
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.magicNumber; ++i) {
            addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        }
        if (needManager()) {
            addToBot(new GainEnergyAction(1));
        }
    }

    @Override
    public AbstractCard makeStatEquivalentCopy() {
        AbstractCard c = super.makeStatEquivalentCopy();
        if (CardCrawlGame.dungeon != null && AbstractDungeon.currMapNode != null
                && (AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.COMBAT) {
            c.setCostForTurn(COST - calculateCost());
        }
        return c;
    }

    public AbstractDoubleCard makeCopy() {
        Earthquake tmp = new Earthquake();
        if (CardCrawlGame.dungeon != null && AbstractDungeon.currMapNode != null
                && (AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.COMBAT) {
            tmp.setCostForTurn(this.costForTurn);
            tmp.refreshPower();
        }
        return tmp;
    }
}
