package SS.power;

import com.evacipated.cardcrawl.mod.stslib.powers.interfaces.InvisiblePower; // 引入 StSLib 隐形能力接口
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;

// 【修改】实现 InvisiblePower 接口，使其隐形
public class VolcanoPower extends AbstractPower implements InvisiblePower {
    public static final String POWER_ID = "SS:VolcanoPower";

    private int turnEndDamage;

    public VolcanoPower(AbstractCreature owner, int turnEndDamage) {
        this.name = "环境：火山结界"; // 虽然看不见，但保留名字方便后台调试
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = turnEndDamage;
        this.turnEndDamage = turnEndDamage;
        this.type = PowerType.BUFF;

        // 【极简】因为是隐形能力，不需要加载任何图标 (无需 loadRegion)
        // 也不需要设置 description，因为玩家在 Buff 栏和 Tip 框里都看不到它
    }

    // 【处于该环境时的效果】：回合结束时对全体造成伤害
    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            // 因为没有图标，所以不需要闪烁
            // this.flash();

            int[] damageArray = DamageInfo.createDamageMatrix(this.turnEndDamage, true);

            addToBot(new DamageAllEnemiesAction(
                    this.owner,
                    damageArray,
                    DamageInfo.DamageType.THORNS,
                    AbstractGameAction.AttackEffect.FIRE));
        }
    }
}