package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.ModifyDamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.interfaces.OnPaintingSubscriber;
import basemod.helpers.CardModifierManager;

public class Hymn extends AbstractC6H14Card implements OnPaintingSubscriber {
    public static final String ID = ModHelper.makePath("Hymn");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public Hymn() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setDamage(2);
        setMagic(1);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(1);
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
        addToBot(new ChannelDiceAction(new AttackDice(this.damage, m)));
    }

    @Override
    public void updateManager() {
        super.updateManager();
        CardModifierManager.addModifier(this, new PaintingModifier());
    }

    @Override
    public void exitManager() {
        super.exitManager();
        CardModifierManager.removeSpecificModifier(this, new PaintingModifier(), false);
    }

    public AbstractDoubleCard makeCopy() {
        return new Hymn();
    }

    @Override
    public void triggerOnPainting(AbstractCard c) {
        addToBot(new ModifyDamageAction(this.uuid, this.magicNumber));
    }
}
