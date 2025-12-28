package SS.cards.Haohao;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.red.Dropkick;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.BerserkPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.AbstractRelic.RelicTier;

import SS.action.common.EchoACardAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.path.AbstractCardEnum;

public class BossSwapGod extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("BossSwapGod");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/BossSwapGod.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public BossSwapGod() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 3;
        this.cardsToPreview = new Dropkick();
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            this.cardsToPreview.upgrade();
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int amount = this.magicNumber;
        for (AbstractRelic r : p.relics) {
            if (r.tier == RelicTier.STARTER) {
                --amount;
                continue;
            }
            for (AbstractPackage c : modcore.mainPackageList) {
                if (c.StartRelic.relicId.equals(r.relicId)) {
                    --amount;
                    break;
                }
            }
        }
        amount = Math.max(0, amount);
        if (needManager()) {
            addToBot(new EchoACardAction(this.cardsToPreview));
        }
        if (amount > 0) {
            addToBot(new ApplyPowerAction(p, p, new BerserkPower(p, amount)));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new BossSwapGod();
    }
}
