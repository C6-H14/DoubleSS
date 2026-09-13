package SS.cards.Lost;

import java.util.ArrayList;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.cards.AbstractDoubleCard;
import SS.cards.Shock.AbstractShockCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import basemod.cardmods.EtherealMod;
import basemod.helpers.CardModifierManager;

public class Unravel extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("Unravel");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Lost/Unravel.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Unravel() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(0);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        ModHelper.atbLambda(() -> {
            ArrayList<AbstractCard> validCards = new ArrayList<>();
            for (AbstractCard c : p.drawPile.group)
                if (c instanceof AbstractShockCard)
                    validCards.add(c);
            for (AbstractCard c : p.discardPile.group)
                if (c instanceof AbstractShockCard)
                    validCards.add(c);
            for (AbstractCard c : p.hand.group)
                if (c instanceof AbstractShockCard && c != this)
                    validCards.add(c);
            for (AbstractCard c : p.exhaustPile.group)
                if (c instanceof AbstractShockCard)
                    validCards.add(c);

            if (!validCards.isEmpty()) {
                AbstractCard chosen = validCards.get(AbstractDungeon.cardRandomRng.random(validCards.size() - 1));
                processShockCard(chosen, p);
            }
        });
    }

    private void processShockCard(AbstractCard card, AbstractPlayer p) {
        // 1. 放入手牌（若原本就在手牌中则无需移动）
        if (p.drawPile.contains(card)) {
            p.drawPile.moveToHand(card, p.drawPile);
        } else if (p.discardPile.contains(card)) {
            p.discardPile.moveToHand(card, p.discardPile);
        } else if (p.exhaustPile.contains(card)) {
            p.exhaustPile.moveToHand(card, p.exhaustPile);
        }

        // 2. 利用 BaseMod 自带的 EtherealMod 加上虚无（若已有则不重复添加）
        if (!card.isEthereal) {
            CardModifierManager.addModifier(card, new EtherealMod());
        }

        // 3. 费用调整：needManager() 永久本场战斗 0 费，否则为本回合 0 费
        if (needManager()) {
            card.modifyCostForCombat(-card.cost); // 永久变为 0 费
        } else {
            card.setCostForTurn(0); // 本回合（下次打出）0 费
        }

        card.applyPowers();
        p.hand.refreshHandLayout();
    }

    public AbstractDoubleCard makeCopy() {
        Unravel c = new Unravel();
        c.copyPermanentFieldsFrom(this);
        return c;
    }
}