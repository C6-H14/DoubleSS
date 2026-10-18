package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;

public class Lily extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Lily");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Lily.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Lily() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setDamage(4);
        setBlock(4);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(2);
            upgradeBlock(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        ModHelper.atbLambda(() -> {
            for (AbstractMonster mo : AllyManager.allies.monsters) {
                if (mo instanceof SoulAlly) {
                    SoulAlly s = (SoulAlly) mo;
                    addToBot(new GainBlockAction(s, this.block));
                }
            }
        });
        if (needManager()) {
            ModHelper.atbLambda(() -> {
                for (AbstractMonster mo : AllyManager.allies.monsters) {
                    if (mo instanceof SoulAlly) {
                        SoulAlly s = (SoulAlly) mo;
                        addToBot(new HealAction(s, p, this.block));
                    }
                }
            });
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Lily();
    }
}
