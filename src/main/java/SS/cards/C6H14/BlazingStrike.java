package SS.cards.C6H14;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.cards.Haohao.AbstractHaoCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower;
import basemod.helpers.CardModifierManager;

public class BlazingStrike extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("BlazingStrike");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/BlazingStrike.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public BlazingStrike() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.tags.add(CardTags.STRIKE);
        setDamage(6);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(3);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        ModHelper.atbLambda(() -> {
            ArrayList<AbstractHaoCard> candidates = new ArrayList<>();
            for (AbstractCard c : p.hand.group) {
                if (c instanceof AbstractHaoCard) {
                    candidates.add((AbstractHaoCard) c);
                }
            }

            if (!candidates.isEmpty()) {
                AbstractHaoCard target = candidates.get(AbstractDungeon.cardRandomRng.random(candidates.size() - 1));
                target.superFlash();

                AbstractCard copy = target.makeStatEquivalentCopy();

                InscribeCardPower power = null;
                for (AbstractMonster mo : AllyManager.allies.monsters) {
                    if (mo instanceof SoulAlly && !mo.isDeadOrEscaped() && mo.hasPower("Double:InscribeCardPower")) {
                        power = (InscribeCardPower) mo.getPower("Double:InscribeCardPower");
                        break;
                    }
                }
                if (power == null && p.hasPower("Double:InscribeCardPower")) {
                    power = (InscribeCardPower) p.getPower("Double:InscribeCardPower");
                }

                if (power != null) {
                    if (power.card != null) {
                        power.playCard();
                    }
                    copy.setCostForTurn(0);
                    power.card = copy;
                    power.updateDescription();
                }
            }
        });
    }

    public void updateManager() {
        CardModifierManager.addModifier(this, new PaintingModifier());
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        CardModifierManager.removeModifiersById(this, "Double:PaintingModifier", false);
        UpdateDescription();
        initializeDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new BlazingStrike();
    }
}
