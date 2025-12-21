package SS.cards.Haohao;

import java.util.HashMap;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackHaoDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;

public class Prism extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("Prism");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/Prism.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;
    private HashMap<AbstractCard.CardColor, Boolean> hsmap = new HashMap<AbstractCard.CardColor, Boolean>();
    private HashMap<PackageEnum, Boolean> shmap = new HashMap<>();

    public Prism() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.damage = this.baseDamage = 2;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int d = this.damage, amount = 0;
        if (needManager()) {
            if (p.hasRelic("PrismaticShard")) {
                d = d * 2;
            }
        }
        hsmap.clear();
        shmap.clear();
        for (AbstractCard c : p.masterDeck.group) {
            if (c instanceof AbstractDoubleCard) {
                PackageEnum pe = ((AbstractDoubleCard) c).packagetype;
                if (!shmap.containsKey(pe)) {
                    ++amount;
                    shmap.put(pe, true);
                }
                continue;
            }
            if (!hsmap.containsKey(c.color)) {
                ++amount;
                hsmap.put(c.color, true);
            }
        }
        for (int i = 0; i < amount; ++i) {
            addToBot(new ChannelDiceAction(new AttackHaoDice(d, m)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Prism();
    }
}
