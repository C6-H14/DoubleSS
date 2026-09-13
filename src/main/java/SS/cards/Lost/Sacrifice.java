package SS.cards.Lost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.common.EchoACardAction;
import SS.cards.AbstractDoubleCard;
import SS.cards.Haohao.AbstractHaoCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class Sacrifice extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("Sacrifice");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Lost/Sacrifice.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Sacrifice() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ReducePowerAction(p, p, "Double:DyingPower", 1));

        // 在动作队列中按时序收集并回响 AbstractHaoCard
        ModHelper.atbLambda(() -> {
            ArrayList<AbstractCard> validCards = new ArrayList<>();

            // 1. 扫描抽牌堆
            for (AbstractCard c : p.drawPile.group) {
                if (c instanceof AbstractHaoCard) {
                    validCards.add(c);
                }
            }

            // 2. 扫描手牌
            for (AbstractCard c : p.hand.group) {
                if (c instanceof AbstractHaoCard) {
                    validCards.add(c);
                }
            }

            // 3. 扫描弃牌堆
            for (AbstractCard c : p.discardPile.group) {
                if (c instanceof AbstractHaoCard) {
                    validCards.add(c);
                }
            }

            // 4. 随机打乱并挑选 magicNumber 张赋予回响
            if (!validCards.isEmpty()) {
                Collections.shuffle(validCards, new Random(AbstractDungeon.cardRandomRng.randomLong()));
                int count = Math.min(this.magicNumber, validCards.size());

                for (int i = 0; i < count; i++) {
                    addToBot(new EchoACardAction(validCards.get(i)));
                }
            }
        });
    }

    public void updateManager() {
        upgradeMagicNumber(1);
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        upgradeMagicNumber(-1);
        UpdateDescription();
        initializeDescription();
    }

    public AbstractDoubleCard makeCopy() {
        Sacrifice c = new Sacrifice();
        c.copyPermanentFieldsFrom(this);
        return c;
    }
}