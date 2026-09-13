package SS.cards.Lost;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.EternalAttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.action.monster.EvokeSoulAction;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import basemod.helpers.CardModifierManager;

public class Respite extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("Respite");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Lost/Respite.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Respite() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.tags.add(AbstractCardEnum.Permanent);
        this.permanentDamage = this.basePermanentDamage = 9;
        this.permanentMagicNumber = this.basePermanentMagicNumber = 1;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradePermanentDamage(3);
            upgradePermanentMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ReducePowerAction(p, p, "Double:DyingPower", 1));
        addToBot(new ChannelDiceAction(new EternalAttackDice(permanentDamage, p)));
        addToBot(new EvokeSoulAction(this.permanentMagicNumber));

    }

    public void updateManager() {
        CardModifierManager.addModifier(this, new PaintingModifier());
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        CardModifierManager.removeModifiersById(this, "Double:PaintingModifier", false);
        UpdateDescription();
        initializeDescription();
    }

    public AbstractDoubleCard makeCopy() {
        Respite c = new Respite();
        c.copyPermanentFieldsFrom(this);
        return c;
    }
}