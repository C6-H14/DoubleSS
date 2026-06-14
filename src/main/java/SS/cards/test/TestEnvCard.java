package SS.cards.test;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.interfaces.IEnvironmentCard;
import SS.path.AbstractCardEnum;
import SS.power.BleedingPower;
import basemod.AutoAdd;

@AutoAdd.Ignore

public class TestEnvCard extends AbstractDoubleCard implements IEnvironmentCard {
    public static final String ID = ModHelper.makePath("TestCard");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_power.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public TestEnvCard() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.selfRetain = true; // 环境牌通常具有留存效果
    }

    public void upgrade() {
        if (!this.upgraded) {
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        SS.helper.EnvironmentManager.playEnvironmentCard(this);
    }

    public AbstractDoubleCard makeCopy() {
        return new TestEnvCard();
    }

    @Override
    public String getEnvironmentID() {
        return "TEST";
    }

    @Override
    public void onEnterEnvironment() {
        System.out.println("环境进场！");
    }

    @Override
    public void onExitEnvironment() {
        System.out.println("环境退场！");
    }
}