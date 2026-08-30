package SS.cardmodifiers;

import java.util.HashMap;
import java.util.Map;

import basemod.helpers.CardModifierManager;
import basemod.interfaces.PreStartGameSubscriber;
import basemod.interfaces.StartGameSubscriber;
import com.megacrit.cardcrawl.cards.AbstractCard;

public class FindFather implements StartGameSubscriber, PreStartGameSubscriber {
    public static Map<AbstractCard, AbstractCard> fatherList = new HashMap<AbstractCard, AbstractCard>();

    public FindFather() {
        fatherList.clear();
    }

    @Override
    public void receiveStartGame() {
        fatherList.clear();
    }

    @Override
    public void receivePreStartGame() {
        fatherList.clear();
    }

    /**
     * 将两张牌进行并查集合并
     */
    public static void ConjugateCard(AbstractCard c1, AbstractCard c2) {
        if (!fatherList.containsKey(c1)) {
            fatherList.put(c1, c1);
        }
        if (!fatherList.containsKey(c2)) {
            fatherList.put(c2, c2);
        }

        AbstractCard root1 = findFather(c1);
        AbstractCard root2 = findFather(c2);

        if (root1 != root2) {
            setFather(root1, root2);
        }

        // 【关键修复】：每次独立 new 一个 Modifier，严禁使用 static 单例
        if (!CardModifierManager.hasModifier(c1, ConjugateModifier.ID)) {
            CardModifierManager.addModifier(c1, new ConjugateModifier());
        }
        if (!CardModifierManager.hasModifier(c2, ConjugateModifier.ID)) {
            CardModifierManager.addModifier(c2, new ConjugateModifier());
        }
    }

    /**
     * 带路径压缩的标准查根操作
     */
    public static AbstractCard findFather(AbstractCard c) {
        if (!fatherList.containsKey(c)) {
            fatherList.put(c, c);
            return c;
        }
        if (fatherList.get(c) == c) {
            return c;
        }
        AbstractCard root = findFather(fatherList.get(c));
        fatherList.put(c, root); // 路径压缩
        return root;
    }

    public static void setFather(AbstractCard card, AbstractCard father) {
        fatherList.put(card, father);
    }

    /**
     * 解绑并从并查集中彻底移除指定卡牌
     */
    public static void removeCard(AbstractCard card) {
        fatherList.remove(card);
    }
}