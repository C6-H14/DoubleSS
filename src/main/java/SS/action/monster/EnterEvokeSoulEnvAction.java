package SS.action.monster;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;

public class EnterEvokeSoulEnvAction extends AbstractGameAction {
    // 【核心修改：3跟班三角形精细阵法】
    private static final float[][] SLOTS = {
            { -240.0F, 30.0F }, // 插槽 0 (左侧 - 完美躲开翅膀、珠子孔和下方卡牌)
            { 300.0F, 280.0F - 50.0F * Settings.scale }, // 插槽 1 (右上 - 玩家身前浮空)
            { 300.0F, 25.0F } // 插槽 2 (右下 - 玩家身前浮空，已抬高避开玩家手牌)
    };

    @Override
    public void update() {
        ArrayList<SoulAlly> aliveSouls = new ArrayList<>();
        for (AbstractMonster m : AllyManager.allies.monsters) {
            if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                aliveSouls.add((SoulAlly) m);
            }
        }

        int index = 0;
        // 1. 移动现有的魂火并将其牌组变形为《圣约》
        for (SoulAlly s : aliveSouls) {
            if (index < 3) {
                s.slotIndex = index;
                s.drawX = AbstractDungeon.player.drawX + SLOTS[index][0] * Settings.scale;
                s.drawY = AbstractDungeon.player.drawY + (SLOTS[index][1] - 50.0F) * Settings.scale;
                s.hb.move(s.drawX, s.drawY + s.hb.height / 2.0F + s.healthBarOffsetY * Settings.scale);

                // 【核心新增】变形牌组
                s.transformToGenesisDeck();

                s.syncSoulFire();
                index++;
            }
        }

        // 2. 生成新的魂火，并直接使其牌组变为《圣约》
        while (index < 3) {
            SoulAlly newSoul = new SoulAlly(SLOTS[index][0], SLOTS[index][1]);
            newSoul.slotIndex = index;
            AllyManager.addMinion(newSoul);

            // 【核心新增】新生成的也立刻变形牌组
            newSoul.transformToGenesisDeck();

            index++;
        }

        this.isDone = true;
    }
}