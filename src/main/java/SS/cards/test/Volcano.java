package SS.cards.test;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.interfaces.IEnvironmentCard;
import SS.path.AbstractCardEnum;
import SS.power.BleedingPower;
import SS.power.VolcanoPower;

public class Volcano extends AbstractDoubleCard implements IEnvironmentCard {
    public static final String ID = ModHelper.makePath("TestCard1");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_power.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Volcano() {
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
        return new Volcano();
    }

    @Override
    public String getEnvironmentID() {
        return "TEST";
    }

    @Override
    public void onEnterEnvironment() {
        AbstractPlayer p = AbstractDungeon.player;

        // 1. 【处于效果】：给予玩家"火山环境能力"，每回合结束造成 5 点伤害
        // 如果卡牌升级了，可以传 8 点
        int dmg = this.upgraded ? 8 : 5;
        addToBot(new ApplyPowerAction(p, p, new VolcanoPower(p, dmg), dmg));
    }

    @Override
    public void onExitEnvironment() {
        AbstractPlayer p = AbstractDungeon.player;

        // 1. 【退场一次性效果】：火山喷发，对所有敌人造成 15 点伤害
        int exitDmg = this.upgraded ? 20 : 15;
        int[] damageArray = DamageInfo.createDamageMatrix(exitDmg, true);
        addToBot(new DamageAllEnemiesAction(
                p,
                damageArray,
                DamageInfo.DamageType.THORNS,
                AbstractGameAction.AttackEffect.FIRE));

        // 2. 【清理】：移除玩家身上的"火山环境能力"
        addToBot(new RemoveSpecificPowerAction(p, p, VolcanoPower.POWER_ID));
    }
}