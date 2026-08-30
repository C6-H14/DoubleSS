package SS.cardmodifiers;

import java.util.ArrayList;
import java.util.List;

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

    public ConjugateModifier() {
    }

    @Override
    public AbstractCardModifier makeCopy() {
        return new ConjugateModifier();
    }

    @Override
    public String modifyDescription(String rawDescription, AbstractCard card) {
        String templateWithoutPlaceholder = STRINGS.TEXT[0].replace("%s", "").trim();
        if (!templateWithoutPlaceholder.isEmpty() && rawDescription.contains(templateWithoutPlaceholder)) {
            return rawDescription;
        }
        return String.format(STRINGS.TEXT[0], rawDescription);
    }

    @Override
    public void onUse(final AbstractCard card, final AbstractCreature target, final UseCardAction action) {
        AbstractPlayer p = AbstractDungeon.player;
        AbstractCard root = FindFather.findFather(card);

        // 1. 先安全地收集所有与当前卡牌同组的牌（此时不修改任何父节点指针）
        List<AbstractCard> drawPileToPlay = new ArrayList<>();
        List<AbstractCard> handToPlay = new ArrayList<>();
        List<AbstractCard> allGroupCards = new ArrayList<>();

        // 收集手牌
        for (AbstractCard c : p.hand.group) {
            if (FindFather.findFather(c) == root) {
                allGroupCards.add(c);
                if (c != card) {
                    handToPlay.add(c);
                }
            }
        }
        // 收集抽牌堆
        for (AbstractCard c : p.drawPile.group) {
            if (FindFather.findFather(c) == root) {
                allGroupCards.add(c);
                if (c != card) {
                    drawPileToPlay.add(c);
                }
            }
        }
        // 收集弃牌堆和消耗堆（用于彻底清理该连通块的绑定关系）
        for (AbstractCard c : p.discardPile.group) {
            if (FindFather.findFather(c) == root) {
                allGroupCards.add(c);
            }
        }
        for (AbstractCard c : p.exhaustPile.group) {
            if (FindFather.findFather(c) == root) {
                allGroupCards.add(c);
            }
        }

        // 2. 统一解绑：清除当前集合中所有卡牌在并查集中的记录，并移除 Modifier
        for (AbstractCard c : allGroupCards) {
            FindFather.removeCard(c);
            addToTop(new RemoveConjugateModifierAction(c));
        }

        // 3. 依次加入打出队列（注意：使用 addToTop 时后加入的先执行，按需倒序或顺序加入）
        for (AbstractCard c : handToPlay) {
            AbstractCreature randomTarget = AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true,
                    AbstractDungeon.cardRandomRng);
            addToTop(new PlaySpecificCardInHand(randomTarget, c, false));
        }

        for (AbstractCard c : drawPileToPlay) {
            AbstractCreature randomTarget = AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true,
                    AbstractDungeon.cardRandomRng);
            addToTop(new PlaySpecificCardInDrawPile(randomTarget, c, false));
        }
    }

    @Override
    public String identifier(AbstractCard card) {
        return ID;
    }
}