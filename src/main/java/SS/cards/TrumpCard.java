package SS.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.ShuffleAction;
import com.megacrit.cardcrawl.actions.defect.ShuffleAllAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.purple.Vault;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.EndTurnDeathPower;

import SS.action.unique.ss.TrumpCardAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class TrumpCard extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("TrumpCard");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/TrumpCard.png";
    private static final int COST = 3;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public TrumpCard() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.exhaust = true;
        this.tags.add(AbstractCardEnum.Fiend);
        this.tags.add(AbstractCardEnum.Sins);
        this.tags.add(AbstractCardEnum.Pride);
        if (needFiend()) {
            updateFiend();
        }
        this.cardsToPreview = new Vault();
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            this.isInnate = true;
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        for (AbstractCard c : AbstractDungeon.actionManager.cardsPlayedThisCombat) {
            if (c.cardID == this.cardID && c != this) {
                return;
            }
        }
        if (needFiend()) {
            addToBot(new ShuffleAllAction());
            addToBot(new ShuffleAction(p.drawPile, false));
        }
        addToBot(new MakeTempCardInDrawPileAction(new Vault(), 1, true, true, true));
        addToBot(new ApplyPowerAction(p, p, new EndTurnDeathPower(p)));
        addToBot(new TrumpCardAction(p));
    }

    public AbstractDoubleCard makeCopy() {
        return new TrumpCard();
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }
}