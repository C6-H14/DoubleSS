package SS.cards.Options;

import basemod.abstracts.CustomCard;
import basemod.helpers.BaseModCardTags;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.DeafForm;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.DeafFormPower;
import SS.power.DelayDamagePower;

public class GREEN_option extends CustomCard {
    public static final String ID = ModHelper.makePath("GREEN_option");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/GREEN_option.png";
    private static final int COST = -2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.STATUS;
    private static final AbstractCard.CardColor COLOR = AbstractCard.CardColor.GREEN;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.SPECIAL;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.NONE;

    public GREEN_option() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
    }

    public void upgrade() {
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    public AbstractCard makeCopy() {
        return new GREEN_option();
    }
}
