package SS.cards.Lost;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.DyingPower;

public class Perseverance extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("Perseverance");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_skill.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Perseverance() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.isEthereal = true;
        this.permanentMagicNumber = this.basePermanentMagicNumber = 3;
        this.tags.add(AbstractCardEnum.Permanent);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradePermanentMagicNumber(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(p, p, new DyingPower(p, this.permanentMagicNumber)));
        ModHelper.atbLambda(() -> {
            int amount = 0;
            if (AbstractDungeon.player.hasPower("Double:DyingPower")) {
                amount = AbstractDungeon.player.getPower("Double:DyingPower").amount;
            }
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                    ((AbstractLostCard) c).upgradePermanentDamage(amount);
                    ((AbstractLostCard) c).upgradePermanentBlock(amount);
                    ((AbstractLostCard) c).upgradePermanentMagicNumber(amount);
                }
            }
        });
        if (needManager()) {
            addToBot(new GainEnergyAction(1));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Perseverance();
    }
}
