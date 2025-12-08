package SS.cards.Haohao;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.green.Prepared;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.ToughBandages;
import com.megacrit.cardcrawl.vfx.RelicAboveCreatureEffect;

import SS.action.common.EchoACardAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.HaoPackPower;

public class HaoPack extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("HaoPack");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/HaoPack.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.POWER;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public HaoPack() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.cardsToPreview = new Prepared();
        this.cardsToPreview.upgrade();
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(p, p, new HaoPackPower(p, 1)));
        if (this.upgraded) {
            if (!p.hasRelic("Tough Bandages")) {
                final ToughBandages r = new ToughBandages();
                AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float) (Settings.WIDTH / 2),
                        (float) (Settings.HEIGHT / 2), r);
                AbstractDungeon.effectList.add(new RelicAboveCreatureEffect(AbstractDungeon.player.hb.cX,
                        AbstractDungeon.player.hb.cY, r));
            }
        }
        if (needManager()) {
            AbstractCard c = new Prepared();
            c.upgrade();
            addToBot(new EchoACardAction(c));
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new HaoPack();
    }
}
