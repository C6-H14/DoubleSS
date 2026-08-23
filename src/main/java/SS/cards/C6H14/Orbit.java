package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.DefendDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.modcore.modcore;

public class Orbit extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Orbit");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Orbit.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Orbit() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setBlock(2 + modcore.orbitMisc);
        setMagic(5);
        this.tags.add(CardTags.HEALING);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(3);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void applyPowers() {
        this.baseBlock = 2 + modcore.orbitMisc;
        super.applyPowers();
        this.initializeDescription();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        // 骰子用打出时显示的旧值（!B!），所以先建骰子，再加 orbitMisc
        addToBot(new ChannelDiceAction(new DefendDice(block, p)));
        addToBot(new ChannelDiceAction(new DefendDice(block, p)));
        addVirtue(magicNumber);
        if (needManager()) {
            addToBot(new GainBlockAction(p, block));
        }
        // 本局游戏中所有同名牌格挡值增加 2（升级后 4）
        modcore.orbitMisc += this.upgraded ? 4 : 2;
        this.applyPowers();
        for (AbstractCard c : p.hand.group) {
            if (c instanceof Orbit) {
                c.applyPowers();
            }
        }
        for (AbstractCard c : p.drawPile.group) {
            if (c instanceof Orbit) {
                c.applyPowers();
            }
        }
        for (AbstractCard c : p.discardPile.group) {
            if (c instanceof Orbit) {
                c.applyPowers();
            }
        }
        for (AbstractCard c : p.exhaustPile.group) {
            if (c instanceof Orbit) {
                c.applyPowers();
            }
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Orbit();
    }
}
