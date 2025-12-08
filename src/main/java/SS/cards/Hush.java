package SS.cards;

import java.util.Iterator;

import com.evacipated.cardcrawl.mod.stslib.actions.common.StunMonsterAction;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.ImmolateDice;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Hush extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("Hush");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Hush.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public Hush() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.exhaust = true;
        this.damage = this.baseDamage = 9;
        initializeDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(2);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < 2; ++i) {
            addToBot(new ChannelDiceAction(new ImmolateDice(this.damage, m)));
        }
        Iterator<AbstractMonster> var3 = (AbstractDungeon.getCurrRoom()).monsters.monsters.iterator();
        while (var3.hasNext()) {
            AbstractMonster mo = var3.next();
            addToBot((AbstractGameAction) new StunMonsterAction(mo, (AbstractCreature) p, 1));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Hush();
    }
}