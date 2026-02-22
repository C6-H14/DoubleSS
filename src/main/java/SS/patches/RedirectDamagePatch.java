package SS.patches;

import java.util.ArrayList;
import java.util.Iterator;

import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import javassist.CtBehavior;

import SS.monster.ally.AbstractAlly;
import SS.monster.ally.AllyManager;

@SpirePatch(clz = AbstractPlayer.class, method = "damage")
public class RedirectDamagePatch {

    // 【核心修复】拦截本地变量 damageAmount (它是一个长度为1的数组)
    @SpireInsertPatch(locator = Locator.class, localvars = { "damageAmount" })
    public static void Insert(AbstractPlayer __instance, DamageInfo info, @ByRef int[] damageAmount) {

        // 1. 如果伤害来源是友军自己，不拦截
        if (info.owner instanceof AbstractAlly) {
            return;
        }

        // 2. 如果是生命流失，不拦截
        if (info.type == DamageInfo.DamageType.HP_LOSS) {
            return;
        }

        // 3. 【关键逻辑】此时的 damageAmount 是玩家扣除格挡后的真实剩余伤害！
        // 如果玩家用格挡扛下了所有伤害，直接跳过，友军绝对安全。
        if (damageAmount[0] <= 0) {
            return;
        }

        // 4. 收集嘲讽友军
        ArrayList<AbstractAlly> solidTanks = new ArrayList<>();
        ArrayList<AbstractAlly> overflowTanks = new ArrayList<>();

        for (AbstractMonster m : AllyManager.allies.monsters) {
            if (!(m instanceof AbstractAlly))
                continue;
            AbstractAlly ma = (AbstractAlly) m;
            if (!ma.isDeadOrEscaped() && !m.isDying) {
                if (ma.tauntType == AbstractAlly.TauntType.SOLID) {
                    solidTanks.add(ma);
                } else if (ma.tauntType == AbstractAlly.TauntType.OVERFLOW) {
                    overflowTanks.add(ma);
                }
            }
        }

        if (solidTanks.isEmpty() && overflowTanks.isEmpty()) {
            return; // 没人抗伤害，继续原版逻辑，玩家自己掉血
        }

        // =================================================================
        // 类型 2：SOLID (硬嘲讽 - 无限抗伤，不溢出)
        // =================================================================
        if (!solidTanks.isEmpty()) {
            AbstractAlly tank = solidTanks.get(0);
            // 友军承受"剩余的全部伤害"
            tank.damage(new DamageInfo(info.owner, damageAmount[0], info.type));

            // 将要对玩家造成的伤害清零，保住玩家的命
            damageAmount[0] = 0;
            return;
        }

        // =================================================================
        // 类型 3：OVERFLOW (软嘲讽 - 溢出传导)
        // =================================================================
        if (!overflowTanks.isEmpty()) {
            Iterator<AbstractAlly> iterator = overflowTanks.iterator();

            while (iterator.hasNext() && damageAmount[0] > 0) {
                AbstractAlly tank = iterator.next();
                int effectiveHealth = tank.currentHealth + tank.currentBlock;

                if (damageAmount[0] <= effectiveHealth) {
                    // 情况 A：友军能扛住剩余的所有伤害
                    tank.damage(new DamageInfo(info.owner, damageAmount[0], info.type));
                    damageAmount[0] = 0; // 玩家安全
                } else {
                    // 情况 B：伤害溢出，友军死亡
                    tank.damage(new DamageInfo(info.owner, damageAmount[0], info.type));
                    // 扣除这名友军吸收的量，剩下的伤害继续交给循环处理
                    damageAmount[0] -= effectiveHealth;
                }
            }
        }
    }

    // =================================================================
    // 【精准定位器】确保我们插在原版代码的正确位置
    // =================================================================
    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            // 找到玩家扣除格挡的那一行： damageAmount = this.decrementBlock(info, damageAmount);
            Matcher finalMatcher = new Matcher.MethodCallMatcher(AbstractPlayer.class, "decrementBlock");
            int[] lines = LineFinder.findInOrder(ctMethodToPatch, finalMatcher);

            // 必须 +1，表示在扣除格挡【之后】进行拦截
            lines[0] += 1;
            return lines;
        }
    }
}