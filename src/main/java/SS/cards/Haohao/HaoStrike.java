package SS.cards.Haohao;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackHaoDice;
import SS.action.common.EchoACardAction;
import SS.action.dice.ChannelDiceAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class HaoStrike extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("HaoStrike");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/HaoStrike.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public HaoStrike() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.damage = this.baseDamage = 3;
        this.magicNumber = this.baseMagicNumber = 1;
        this.tags.add(AbstractCard.CardTags.STRIKE);
        if (needManager()) {
            updateManager();
        }
        this.tags.add(CardTags.STRIKE);
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

    public static int countCards() {
        int count = 0;
        for (final AbstractCard c : AbstractDungeon.player.hand.group) {
            if (isHao(c)) {
                ++count;
            }
        }
        for (final AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (isHao(c)) {
                ++count;
            }
        }
        for (final AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (isHao(c)) {
                ++count;
            }
        }
        return count;
    }

    public static boolean isHao(final AbstractCard c) {
        return (c instanceof AbstractHaoCard);
    }

    @Override
    public void use(final AbstractPlayer p, final AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackHaoDice(this.damage, m)));
        if (needManager()) {
            addToBot(new EchoACardAction(this));
        }
    }

    @Override
    public void calculateCardDamage(final AbstractMonster mo) {
        final int realBaseDamage = this.baseDamage;
        this.baseDamage += this.magicNumber * countCards();
        super.calculateCardDamage(mo);
        this.baseDamage = realBaseDamage;
        this.isDamageModified = (this.damage != this.baseDamage);
    }

    @Override
    public void applyPowers() {
        final int realBaseDamage = this.baseDamage;
        this.baseDamage += this.magicNumber * countCards();
        super.applyPowers();
        this.baseDamage = realBaseDamage;
        this.isDamageModified = (this.damage != this.baseDamage);
    }

    public AbstractDoubleCard makeCopy() {
        return new HaoStrike();
    }
}
