package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.ImmolateDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.monster.ally.SoulAlly.SoulColor;

public class Trisagion extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Trisagion");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Trisagion.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public Trisagion() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setDamage(4);
        setMagic(10);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        if (getVirtue() < this.magicNumber)
            return;
        for (int i = 0; i < 3; i++) {
            addToBot(new ChannelDiceAction(new ImmolateDice(8, m)));
        }
        ModHelper.atbLambda(() -> {
            for (int i = 0; i < 3; i++) {
                for (AbstractMonster mo : AllyManager.allies.monsters) {
                    if (mo instanceof SoulAlly) {
                        SoulAlly s = (SoulAlly) mo;
                        s.changeColor(SoulColor.RED);
                        s.changeColor(SoulColor.WHITE);
                    }
                }
            }
        });
        if (needManager()) {
            addVirtue(10);
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Trisagion();
    }
}
