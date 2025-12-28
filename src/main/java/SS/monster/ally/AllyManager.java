package SS.monster.ally;

import java.util.Iterator;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class AllyManager {
    // 【核心修改】使用 MonsterGroup 替代 ArrayList
    // 这样我们就可以直接调用 allies.update() 和 allies.render(sb)
    public static MonsterGroup allies = new MonsterGroup(new AbstractMonster[0]);

    public static void addMinion(AbstractAlly m) {
        // MonsterGroup 自带 add 方法
        allies.add(m);

        m.usePreBattleAction();
        m.showHealthBar();
        m.rollMove();
        m.createIntent();
    }

    public static void update() {
        // 1. 利用 MonsterGroup 自带的 update
        // 它会自动调用所有怪物的 update()，处理 hover 逻辑等
        allies.update();

        // 2. 清理死亡友军
        // MonsterGroup 内部是一个 ArrayList<AbstractMonster>，名为 monsters
        Iterator<AbstractMonster> i = allies.monsters.iterator();
        while (i.hasNext()) {
            AbstractMonster m = i.next();
            if (m.isDead || m.isDying) {
                if (m.isDead) {
                    i.remove();
                }
            }
        }
    }

    // 强制刷新所有友军的意图数值
    public static void refreshIntents() {
        for (AbstractMonster m : allies.monsters) {
            if (m instanceof AbstractAlly && !m.isDeadOrEscaped()) {
                // 调用我们在 AbstractAlly 中重写的 applyPowers
                // 它会读取基础伤害 -> 调用 calculateDamage -> 更新 intentDmg
                m.applyPowers();
            }
        }
    }

    public static void render(SpriteBatch sb) {
        // MonsterGroup 自带 render
        // 它会自动处理渲染循环
        allies.render(sb);
    }

    public static void clear() {
        allies.monsters.clear();
    }

    // =================================================================
    // 回合逻辑 (需要强转，因为 MonsterGroup 存的是 AbstractMonster)
    // =================================================================

    public static void onPlayerStartTurn() {
        for (AbstractMonster m : allies.monsters) {
            if (m instanceof AbstractAlly && !m.isDead && !m.isDying) {
                // 触发友军逻辑
                ((AbstractAlly) m).atStartOfTurn();

                // 触发 Power 逻辑
                if (m.powers != null) {
                    for (com.megacrit.cardcrawl.powers.AbstractPower p : m.powers) {
                        p.atStartOfTurn();
                    }
                }
            }
        }
    }

    public static void onPlayerEndTurn() {
        // 1. 执行行动
        for (AbstractMonster m : allies.monsters) {
            if (m instanceof AbstractAlly && !m.isDead && !m.isDying) {
                ((AbstractAlly) m).atEndOfTurn();

                if (m.powers != null) {
                    for (com.megacrit.cardcrawl.powers.AbstractPower p : m.powers) {
                        p.atEndOfTurn(true);
                        p.atEndOfTurnPreEndTurnCards(true);
                    }
                }
            }
        }

        // 2. 规划下回合意图
        for (AbstractMonster m : allies.monsters) {
            if (m instanceof AbstractAlly && !m.isDead && !m.isDying) {
                m.rollMove();
                m.createIntent();
            }
        }
    }
}