package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.monster.EnterEvokeSoulEnvAction;
import SS.action.monster.ExitEvokeSoulEnvAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.interfaces.IEnvironmentCard;

public class Genesis extends AbstractC6H14Card implements IEnvironmentCard {
    public static final String ID = ModHelper.makePath("Genesis");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_power.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Genesis() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.selfRetain = true; // 环境牌通常具有留存效果
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        SS.helper.EnvironmentManager.playEnvironmentCard(this);
    }

    public AbstractDoubleCard makeCopy() {
        return new Genesis();
    }

    @Override
    public String getEnvironmentID() {
        return "Double:GENESIS";
    }

    @Override
    public void onEnterEnvironment() {
        // 进场：召唤魂火直到有 5 个并列阵
        addToBot(new EnterEvokeSoulEnvAction());
    }

    @Override
    public void onExitEnvironment() {
        // 退场：吞噬合并，返还卡牌
        addToBot(new ExitEvokeSoulEnvAction());
    }
}
