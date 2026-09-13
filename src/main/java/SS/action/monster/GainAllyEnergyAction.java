package SS.action.monster;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.monster.AbstractCardMonster;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;

public class GainAllyEnergyAction extends AbstractGameAction {
    private AbstractCardMonster targetAlly;
    private int energyAmount;
    private boolean forAllSoulAllies = false;

    // 构造函数 1：给【指定的一个】友军增加费用
    public GainAllyEnergyAction(AbstractCardMonster targetAlly, int energyAmount) {
        this.targetAlly = targetAlly;
        this.energyAmount = energyAmount;
        this.forAllSoulAllies = false;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    // 构造函数 2：给【场上所有活着的魂火】群体增加费用
    public GainAllyEnergyAction(int energyAmount) {
        this.energyAmount = energyAmount;
        this.forAllSoulAllies = true;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (forAllSoulAllies) {
            // 模式 A：群体加费
            boolean playedSound = false;
            for (AbstractMonster m : AllyManager.allies.monsters) {
                if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                    applyEnergy((SoulAlly) m);
                    playedSound = true;
                }
            }
            if (playedSound) {
                CardCrawlGame.sound.play("ENERGY_BUFF"); // 播放原版充能音效
            }
        } else {
            // 模式 B：单体加费
            if (targetAlly != null && !targetAlly.isDeadOrEscaped()) {
                applyEnergy(targetAlly);
                CardCrawlGame.sound.play("ENERGY_BUFF");
            }
        }

        this.isDone = true;
    }

    // 执行加费与意图刷新核心逻辑
    private void applyEnergy(AbstractCardMonster ally) {
        // 1. 增加当前可用能量
        ally.energy += this.energyAmount;

        // (可选) 如果你想连同每回合的基础能量上限一起加，解除下面这行的注释：
        // ally.energyBase += this.energyAmount;

        // 2. 【核心步骤】立刻重新计算意图！
        // 能量变多后，原本因为卡费打不出来的手牌现在可以打了
        // 这行代码会立刻刷新头顶的大剑伤害数值和卡牌意图队列
        ally.refreshIntentCalculation();
    }
}