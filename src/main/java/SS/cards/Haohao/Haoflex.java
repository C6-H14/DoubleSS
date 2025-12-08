package SS.cards.Haohao;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import SS.Dice.AbstractDice;
import SS.Dice.AttackHaoDice;
import SS.Dice.DefendHaoDice;
import SS.Dice.EmptyDiceSlot;
import SS.action.dice.RemoveDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Haoflex extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("Haoflex");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/Haoflex.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Haoflex() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 2;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public boolean canUse(AbstractPlayer p, AbstractMonster m) {
        boolean canUse = super.canUse(p, m);
        if (!canUse) {
            return false;
        }
        if (needManager()) {
            for (AbstractOrb o : AbstractDungeon.player.orbs) {
                if (o instanceof AbstractDice && !(o instanceof EmptyDiceSlot)) {
                    return true;
                }
            }
            return false;
        }
        for (AbstractOrb o : AbstractDungeon.player.orbs) {
            if (o instanceof AttackHaoDice || o instanceof DefendHaoDice) {
                return true;
            }
        }
        return false;
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
        if (needManager()) {
            addToBot(new RemoveDiceAction(1));
        } else {
            ArrayList<String> s = new ArrayList<>();
            s.add("Double:AttackHaoDice");
            s.add("Double:DefendHaoDice");
            addToBot(new RemoveDiceAction(s, 1));
        }
        addToBot(new DrawCardAction(this.magicNumber));
    }

    public AbstractDoubleCard makeCopy() {
        return new Haoflex();
    }
}
