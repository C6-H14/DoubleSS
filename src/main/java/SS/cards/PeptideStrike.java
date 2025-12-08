package SS.cards;

import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.PeptideDice;
import SS.action.dice.ChannelDiceAction;
import SS.helper.ModHelper;

public class PeptideStrike extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("PeptideStrike");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/PeptideStrike.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCard.CardColor.COLORLESS;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.SPECIAL;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public PeptideStrike() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET);
        if (AbstractDungeon.player != null && AbstractDungeon.player.hasPower("PepsinPower")) {
            this.damage = this.baseDamage = 2 + (AbstractDungeon.player.getPower("Accuracy")).amount;
        } else {
            this.damage = this.baseDamage = 2;
        }
        this.magicNumber = this.baseMagicNumber = 1;
        this.tags.add(AbstractCard.CardTags.STRIKE);
        this.exhaust = true;
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(1);
            this.rawDescription = CARD_STRINGS.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int setDamage = this.damage;
        addToBot(new DamageAction(m, new DamageInfo(p, this.damage, DamageInfo.DamageType.NORMAL)));
        addToBot(new ChannelDiceAction(new PeptideDice(setDamage, m)));
    }

    public AbstractDoubleCard makeCopy() {
        return new PeptideStrike();
    }
}