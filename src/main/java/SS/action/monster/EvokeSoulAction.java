package SS.action.monster;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.common.SpawnAllyAction;
import SS.action.unique.GainVirtueAction;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.PaintingPower;

public class EvokeSoulAction extends AbstractGameAction {

    public EvokeSoulAction() {
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = 1;
    }

    public EvokeSoulAction(int amount) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
    }

    @Override
    public void update() {
        // 1. 获得 1 点美德
        addToTop(new GainVirtueAction(AbstractDungeon.player, this.amount));
        if (this.amount >= 1) {
            if (AbstractDungeon.player.hasPower("Double:SermonPower")) {
                addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player,
                        new PaintingPower(AbstractDungeon.player, 1)));
            }
        }

        // 2. 查找场上是否有 SoulAlly
        boolean hasSoulFire = false;
        // 遍历你自己的 AllyManager (或者 AbstractDungeon.getMonsters())
        for (AbstractMonster m : AllyManager.allies.monsters) {
            if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                hasSoulFire = true;

                // 【升级魂火】：血量上限+3，并且回复3点血
                // 因为我们在 SoulAlly 里写了 syncSoulFire()
                // increaseMaxHp 内部会触发 heal，自动调用 syncSoulFire，层数自动+1！
                m.increaseMaxHp(3 * this.amount, true);
            }
        }

        // 3. 如果没有，生成一个（0 号槽：玩家右前方首发位）
        if (!hasSoulFire) {
            addToBot(new SpawnAllyAction(new SoulAlly(0)));
        }

        // 4. 战斗统计：记本次唤魂贡献（召唤 1 只 = 1 点魂火值；否则 = amount 点）。
        //    魂火后续的伤害/格挡按各唤魂牌的累计贡献加权均分（用户定口径）。
        //    栈顶即打出本牌的卡（EvokeSoulAction 在 card.use() 内入队）。
        SS.stats.CardStats.onSoulContribute(SS.stats.CardStats.stackTopCard(),
                hasSoulFire ? this.amount : 1);

        this.isDone = true;
    }
}