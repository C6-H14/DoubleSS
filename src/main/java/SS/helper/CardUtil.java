package SS.helper;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class CardUtil {

    /**
     * 获取当前卡牌的使用者（源头）。
     * 如果处于友军上下文，返回友军；否则返回玩家。
     */
    public static AbstractCreature getCardSource() {
        // 1. 检查是否有怪物/友军正在打牌或计算数值
        if (MonsterCardContext.activeMonster != null) {
            return MonsterCardContext.activeMonster;
        }

        // 2. 否则默认为玩家
        return AbstractDungeon.player;
    }
}