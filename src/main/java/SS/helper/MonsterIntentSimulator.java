package SS.helper;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

import SS.monster.AbstractCardMonster;
import SS.patches.ActionSimulationPatches;

public class MonsterIntentSimulator {

    public static class SimulationResult {
        public int totalDamage = 0;
        public int totalBlock = 0;
        public ArrayList<AbstractCard> cardsToPlay = new ArrayList<>();
        public boolean hasUnknown = false;
    }

    public static SimulationResult simulate(AbstractCardMonster monster) {
        SimulationResult result = new SimulationResult();

        // 1. 初始化
        int currentEnergy = monster.energy;
        ActionSimulationPatches.clear(); // 清空拦截器

        // 获取手牌副本 (因为 use() 可能会改变卡牌状态，必须用副本)
        // 注意：这里简单的引用 group 还是不够，我们在循环里 copy
        ArrayList<AbstractCard> hand = monster.hand.group;

        try {
            for (AbstractCard c : hand) {
                // 必须为每张卡创建全新的副本进行模拟
                AbstractCard tempCard = c.makeStatEquivalentCopy();

                // 2. 能量检查 (支持虚拟能量，例如《加速》给的能量支持打出下一张)
                // 注意：这里简单的加法可能不够精确（因为 energy 是回合总值），
                // 但对于意图显示来说，假设能量是累积使用的即可。
                if (currentEnergy < tempCard.costForTurn) {
                    continue; // 没能量，跳过
                }

                // 扣除能量
                currentEnergy -= tempCard.costForTurn;
                result.cardsToPlay.add(c); // 记录原始卡牌用于 UI 显示

                // =========================================================
                // 步骤 A: 数值计算 (Calculate)
                // 利用 Phase 2 的 Context 和 Patch，带入虚拟 Buff 计算伤害/格挡
                // =========================================================

                // 准备虚拟 Buff
                int simStr = 0;
                int simDex = 0;
                for (AbstractPower p : ActionSimulationPatches.virtualPowers) {
                    if (p instanceof StrengthPower)
                        simStr += p.amount;
                    if (p instanceof DexterityPower)
                        simDex += p.amount;
                }

                final int finalSimStr = simStr;
                final int finalSimDex = simDex;

                MonsterCardContext.run(monster, () -> {
                    int powerCountBefore = monster.powers.size();

                    try {
                        // 1. 临时注入虚拟 Buff
                        if (finalSimStr != 0)
                            monster.powers.add(new StrengthPower(monster, finalSimStr));
                        if (finalSimDex != 0)
                            monster.powers.add(new DexterityPower(monster, finalSimDex));

                        // 2. 刷新数值
                        tempCard.applyPowers();

                        // 3. 针对目标计算 (如果有)
                        if (monster instanceof SS.monster.ally.AbstractAlly) {
                            AbstractMonster target = ((SS.monster.ally.AbstractAlly) monster).getTarget();
                            if (target != null)
                                tempCard.calculateCardDamage(target);
                        }

                        // 4. 累加结果
                        if (tempCard.type == AbstractCard.CardType.ATTACK) {
                            result.totalDamage += tempCard.damage;
                        }
                        if (tempCard.baseBlock > 0) {
                            result.totalBlock += tempCard.block;
                        }

                        // 5. 移除临时 Buff (恢复现场)
                        if (finalSimDex != 0)
                            monster.powers.remove(monster.powers.size() - 1);
                        if (finalSimStr != 0)
                            monster.powers.remove(monster.powers.size() - 1);

                    } finally {
                        // 【关键】使用 finally 块确保 Power 一定被移除
                        // 恢复到之前的数量
                        while (monster.powers.size() > powerCountBefore) {
                            monster.powers.remove(monster.powers.size() - 1);
                        }
                    }
                });

                // =========================================================
                // 步骤 B: 虚拟打出 (Intercept)
                // 执行 use() 以产生后续效果
                // =========================================================

                // 开启拦截
                ActionSimulationPatches.isSimulating = true;

                // 确定目标
                AbstractMonster target = null;
                if (monster instanceof SS.monster.ally.AbstractAlly) {
                    target = ((SS.monster.ally.AbstractAlly) monster).getTarget();
                }

                // 执行 use (所有的 Action 会被 Patch 拦截并存入 ActionSimulationPatches)
                try {
                    tempCard.use(AbstractDungeon.player, target);
                } catch (Exception e) {
                    // 忽略模拟过程中的报错
                }

                // 关闭拦截 (虽然循环会继续，但显式关闭是好习惯)
                ActionSimulationPatches.isSimulating = false; // 暂时关闭，等下次循环再开

                // =========================================================
                // 步骤 C: 处理拦截结果
                // =========================================================

                // 1. 检查抽牌 -> 中断模拟
                if (ActionSimulationPatches.virtualDrawFound) {
                    result.hasUnknown = true;
                    break;
                }

                // 2. 累加拦截到的直接格挡 (如《硬化》)
                if (ActionSimulationPatches.virtualBlock > 0) {
                    result.totalBlock += ActionSimulationPatches.virtualBlock;
                    ActionSimulationPatches.virtualBlock = 0; // 归零，防止累加给下一张牌
                }

                // 3. 累加拦截到的能量 (用于支持后续卡牌)
                if (ActionSimulationPatches.virtualEnergy > 0) {
                    currentEnergy += ActionSimulationPatches.virtualEnergy;
                    ActionSimulationPatches.virtualEnergy = 0;
                }

                // 注意：virtualPowers 不需要归零，因为力量是持续整回合的
            }
        } finally {
            // 最终清理
            ActionSimulationPatches.clear();
        }

        return result;
    }
}