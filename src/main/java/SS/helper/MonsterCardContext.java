package SS.helper;

import SS.monster.AbstractCardMonster;

public class MonsterCardContext {
    public static AbstractCardMonster activeMonster = null;

    public static void run(AbstractCardMonster monster, Runnable action) {
        AbstractCardMonster prev = activeMonster;
        activeMonster = monster;
        try {
            action.run();
        } finally {
            activeMonster = prev;
        }
    }
}