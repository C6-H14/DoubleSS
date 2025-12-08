package SS.cardmodifiers;

import java.util.HashMap;
import java.util.Map;

import basemod.helpers.CardModifierManager;
import basemod.interfaces.PreStartGameSubscriber;
import basemod.interfaces.StartGameSubscriber;
import com.megacrit.cardcrawl.cards.AbstractCard;

public class FindFather implements SaveLoadSubscriber, StartGameSubscriber, PreStartGameSubscriber {
    public static Map<AbstractCard, AbstractCard> fatherList = new HashMap<AbstractCard, AbstractCard>();
    static ConjugateModifier newConjugate = new ConjugateModifier();

    public FindFather() {
        fatherList.clear();
    }

    @Override
    public void receiveStartGame() {
        fatherList.clear();
    }

    @Override
    public void onLoadGame() {
        fatherList.clear();
    }

    public static void ConjugateCard(AbstractCard c1, AbstractCard c2) {
        if (!fatherList.containsKey(c1)) {
            fatherList.put(c1, c1);
        }
        if (!fatherList.containsKey(c2)) {
            fatherList.put(c2, c2);
        }
        if (findFather(c1) == findFather(c2)) {
            return;
        }
        setFather(findFather(c1), findFather(c2));
        CardModifierManager.addModifier(c1, newConjugate);
        CardModifierManager.addModifier(c2, newConjugate);
    }

    public static AbstractCard findFather(AbstractCard c) {
        if (!fatherList.containsKey(c)) {
            fatherList.put(c, c);
        }
        if (fatherList.get(c) == c) {
            return c;
        }
        setFather(c, findFather(fatherList.get(c)));
        return fatherList.get(c);
    }

    public static void setFather(AbstractCard card, AbstractCard father) {
        if (!fatherList.containsKey(card)) {
            fatherList.put(card, father);
        } else {
            fatherList.replace(card, father);
        }
    }

    @Override
    public void receivePreStartGame() {
        fatherList.clear();
    }
}
