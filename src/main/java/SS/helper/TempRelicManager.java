package SS.helper;

import java.util.ArrayList;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import basemod.BaseMod;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.StartGameSubscriber;

public class TempRelicManager implements PostBattleSubscriber, StartGameSubscriber {
    // 保存本场战斗被临时移除的遗物
    public static final ArrayList<AbstractRelic> removedRelics = new ArrayList<>();

    public TempRelicManager() {
        BaseMod.subscribe(this);
    }

    /**
     * 临时移除遗物
     */
    public static void removeRelicForCombat(AbstractRelic r) {
        if (AbstractDungeon.player != null && AbstractDungeon.player.relics.contains(r)) {
            r.flash();
            removedRelics.add(r);
            AbstractDungeon.player.relics.remove(r);
            AbstractDungeon.player.reorganizeRelics(); // 重新排列顶部遗物栏UI
        }
    }

    /**
     * 归还所有遗物
     */
    public static void restoreAllRelics() {
        if (AbstractDungeon.player != null && !removedRelics.isEmpty()) {
            for (AbstractRelic r : removedRelics) {
                if (!AbstractDungeon.player.relics.contains(r)) {
                    AbstractDungeon.player.relics.add(r);
                }
            }
            AbstractDungeon.player.reorganizeRelics(); // 恢复排版
            removedRelics.clear();
        }
    }

    @Override
    public void receivePostBattle(AbstractRoom room) {
        // 战斗胜利或逃跑结算瞬间归还，确保后续存档包含该遗物
        restoreAllRelics();
    }

    @Override
    public void receiveStartGame() {
        // 重新进游戏或读档时清空
        removedRelics.clear();
    }
}