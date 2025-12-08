package SS.cardmodifiers;

import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

import SS.action.common.PlaySpecificCardInDrawPile;
import SS.action.common.PlaySpecificCardInHand;
import SS.action.common.RemoveConjugateModifierAction;
import SS.helper.ModHelper;
import basemod.abstracts.AbstractCardModifier;

public class ConjugateModifier extends AbstractCardModifier {
    public static String ID = ModHelper.makePath("ConjugateModifier");
    private static final UIStrings STRINGS = CardCrawlGame.languagePack.getUIString(ID);
    public AbstractCard c;

    @Override
    public AbstractCardModifier makeCopy() {
        return new ConjugateModifier();
    }

    public ConjugateModifier() {
    }

    public ConjugateModifier(AbstractCard card) {
        this.c = card;
    }

    public void onUse(final AbstractCard card, final AbstractCreature target, final UseCardAction action) {
        AbstractPlayer p = AbstractDungeon.player;
        AbstractCard root = FindFather.findFather(card);
        boolean flag = false;
        addToTop(new RemoveConjugateModifierAction(card));
        for (AbstractCard c : p.drawPile.group) {
            if (FindFather.findFather(c) == root && c != card) {
                addToTop(new RemoveConjugateModifierAction(c));
                addToTop(new PlaySpecificCardInDrawPile((AbstractCreature) (AbstractDungeon.getCurrRoom()).monsters
                        .getRandomMonster(null, true, AbstractDungeon.cardRandomRng), c, false));
                FindFather.setFather(c, c);
                if (c == root) {
                    flag = true;
                }
            }
        }
        for (AbstractCard c : p.hand.group) {
            if (FindFather.findFather(c) == root && c != card) {
                addToTop(new RemoveConjugateModifierAction(c));
                addToTop(new PlaySpecificCardInHand((AbstractCreature) (AbstractDungeon.getCurrRoom()).monsters
                        .getRandomMonster(null, true, AbstractDungeon.cardRandomRng), c, false));
                FindFather.setFather(c, c);
                if (c == root) {
                    flag = true;
                }
            }
        }
        if (flag) {
            AbstractCard nextfather = root;
            for (AbstractCard c : p.drawPile.group) {
                if (FindFather.findFather(c) == root) {
                    if (nextfather == root) {
                        nextfather = c;
                    }
                    FindFather.setFather(c, nextfather);
                }
            }
            for (AbstractCard c : p.hand.group) {
                if (FindFather.findFather(c) == root) {
                    if (nextfather == root) {
                        nextfather = c;
                    }
                    FindFather.setFather(c, nextfather);
                }
            }
            for (AbstractCard c : p.discardPile.group) {
                if (FindFather.findFather(c) == root) {
                    if (nextfather == root) {
                        nextfather = c;
                    }
                    FindFather.setFather(c, nextfather);
                }
            }
            for (AbstractCard c : p.exhaustPile.group) {
                if (FindFather.findFather(c) == root) {
                    if (nextfather == root) {
                        nextfather = c;
                    }
                    FindFather.setFather(c, nextfather);
                }
            }
        }
    }

    @Override
    public String identifier(AbstractCard card) {
        return ID;
    }
}
