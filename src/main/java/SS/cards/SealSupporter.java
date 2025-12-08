package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.unique.ExhaustAllNonAttackAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

import SS.action.common.ExhaustSpecificTypeAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import basemod.abstracts.CustomSavable;

public class SealSupporter extends AbstractDoubleCard implements CustomSavable<Boolean> {
    public static final String ID = ModHelper.makePath("SealSupporter");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/SealSupporter.png";
    private static final String IMG_PATH1 = "img/cards/SealSupporter1.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;
    private boolean flag = true;

    public SealSupporter() {
        super(ID, NAME, testOutput % 2 == 0 ? IMG_PATH : IMG_PATH1, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        this.magicNumber = this.baseMagicNumber = 4;
        this.exhaust = true;
        if (testOutput % 2 == 0) {
            this.flag = true;
        } else {
            this.flag = false;
        }
        if (flag) {
            if (this.upgraded) {
                this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            } else {
                this.rawDescription = CARD_STRINGS.DESCRIPTION;
            }
        } else {
            if (this.upgraded) {
                this.rawDescription = CARD_STRINGS.EXTENDED_DESCRIPTION[1];
            } else {
                this.rawDescription = CARD_STRINGS.EXTENDED_DESCRIPTION[0];
            }
        }
        initializeDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            if (testOutput % 2 == 0) {
                this.flag = true;
            } else {
                this.flag = false;
            }
            if (flag) {
                if (this.upgraded) {
                    this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
                } else {
                    this.rawDescription = CARD_STRINGS.DESCRIPTION;
                }
            } else {
                if (this.upgraded) {
                    this.rawDescription = CARD_STRINGS.EXTENDED_DESCRIPTION[1];
                } else {
                    this.rawDescription = CARD_STRINGS.EXTENDED_DESCRIPTION[0];
                }
            }
            initializeDescription();
        }
    }

    private static int testOutput = 0;

    @Override
    public Boolean onSave() {
        System.out.println("退出游戏中，此时布尔值为" + this.flag);
        return this.flag;
    }

    @Override
    public void onLoad(Boolean arg0) {
        if (testOutput % 2 == 0) {
            this.flag = true;
        } else {
            this.flag = false;
        }
        System.out.println("本次启动游戏程序开始，累计第" + ++testOutput + "次SL");
        System.out.println("此时布尔值为" + this.flag);
        if (flag) {
            if (this.upgraded) {
                this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            } else {
                this.rawDescription = CARD_STRINGS.DESCRIPTION;
            }
        } else {
            if (this.upgraded) {
                this.rawDescription = CARD_STRINGS.EXTENDED_DESCRIPTION[1];
            } else {
                this.rawDescription = CARD_STRINGS.EXTENDED_DESCRIPTION[0];
            }
        }
        initializeDescription();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        if (testOutput % 2 == 0) {
            this.flag = true;
        } else {
            this.flag = false;
        }
        System.out.println("本牌的布尔值为：" + this.flag);
        if (flag) {
            int amount = this.magicNumber;
            for (AbstractCard c : p.hand.group) {
                if (c.type == CardType.ATTACK) {
                    --amount;
                    if (amount < 0) {
                        amount = 0;
                    }
                }
            }
            addToBot(new ExhaustSpecificTypeAction(CardType.ATTACK));
            addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, amount)));
        } else {
            int amount = this.magicNumber;
            for (AbstractCard c : p.hand.group) {
                if (c.type != CardType.ATTACK) {
                    --amount;
                    if (amount < 0) {
                        amount = 0;
                    }
                }
            }
            addToBot(new ExhaustAllNonAttackAction());
            addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, amount)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new SealSupporter();
    }
}