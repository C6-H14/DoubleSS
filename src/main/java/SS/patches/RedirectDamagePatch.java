package SS.patches;

import java.util.ArrayList;
import java.util.Iterator;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.monster.ally.AbstractAlly;
import SS.monster.ally.AllyManager;

@SpirePatch(clz = AbstractPlayer.class, method = "damage")
public class RedirectDamagePatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractPlayer player, DamageInfo info) {
        // 1. 如果伤害来源是友军自己（比如自残），不拦截
        if (info.owner instanceof AbstractAlly) {
            return SpireReturn.Continue();
        }

        // 2. 如果是生命流失（HP_LOSS），通常穿透护盾和嘲讽，建议不拦截
        if (info.type == DamageInfo.DamageType.HP_LOSS) {
            return SpireReturn.Continue();
        }

        // 3. 收集场上所有具有嘲讽能力的友军
        // 我们需要把 SOLID (类型2) 和 OVERFLOW (类型3) 分开处理
        // 逻辑：优先让 SOLID 扛，如果没有 SOLID，再让 OVERFLOW 扛
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

        // 如果没有坦克，玩家正常承受伤害
        if (solidTanks.isEmpty() && overflowTanks.isEmpty()) {
            return SpireReturn.Continue();
        }

        // 获取原始伤害值
        int damageAmount = info.output;
        if (damageAmount <= 0)
            return SpireReturn.Continue();

        // =================================================================
        // 处理类型 2：SOLID (硬嘲讽 - 不溢出)
        // =================================================================
        // 规则：只要有这种怪，它就吃下全部伤害，哪怕它只有1血。
        // 如果有多个，默认让第一个扛（或者你可以写逻辑让血最少的扛）
        if (!solidTanks.isEmpty()) {
            AbstractAlly tank = solidTanks.get(0);

            // 让友军受伤
            tank.damage(new DamageInfo(info.owner, damageAmount, info.type));

            // 拦截玩家收到的伤害，强制 return
            // 此时玩家受到的伤害被完全抵消
            return SpireReturn.Return();
        }

        // =================================================================
        // 处理类型 3：OVERFLOW (软嘲讽 - 溢出传导)
        // =================================================================
        // 规则：层层抵扣。如果有剩余伤害，最后打到玩家身上。
        if (!overflowTanks.isEmpty()) {

            // 使用迭代器遍历，方便处理
            Iterator<AbstractAlly> iterator = overflowTanks.iterator();

            while (iterator.hasNext() && damageAmount > 0) {
                AbstractAlly tank = iterator.next();

                // 计算友军的"有效生命值" (格挡 + 血量)
                int effectiveHealth = tank.currentHealth + tank.currentBlock;

                if (damageAmount <= effectiveHealth) {
                    // 情况 A：友军能扛住所有伤害
                    tank.damage(new DamageInfo(info.owner, damageAmount, info.type));
                    damageAmount = 0;
                    // 伤害被吸收殆尽，玩家安全，直接返回
                    return SpireReturn.Return();
                } else {
                    // 情况 B：伤害溢出，友军死亡
                    // 先对友军造成足以致死的伤害 (为了触发 damage 相关的特效和逻辑)
                    // 注意：这里为了视觉效果，可以直接传 effectiveHealth 或者 raw damageAmount
                    // 建议传 effectiveHealth 让他正好死，或者传 damageAmount 让它显示大数字然后死

                    // 这里我们选择对他造成全部伤害，让他死掉
                    tank.damage(new DamageInfo(info.owner, damageAmount, info.type));

                    // 扣除它的有效生命值
                    damageAmount -= effectiveHealth;

                    // 继续循环，寻找下一个受害者
                }
            }

            // 循环结束后，如果还有 damageAmount > 0，说明所有坦克都死光了还有剩余伤害
            // 此时，修改 info.output 让玩家承受剩余伤害
            if (damageAmount > 0) {
                info.output = damageAmount;
                // Continue 会继续执行原版 player.damage，使用修改后的数值
                return SpireReturn.Continue();
            } else {
                return SpireReturn.Return();
            }
        }

        return SpireReturn.Continue();
    }

}
