package SS.interfaces; // 【修改为你自己的包名】

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import SS.monster.ally.AbstractAlly;

public interface IAllyDamageModifier {

    // 【加算阶段】基础伤害增加 (类似力量、活力)
    default float onAllyModifyDamageGive(AbstractAlly ally, AbstractCreature target, float damage,
            DamageInfo.DamageType type) {
        return damage;
    }

    // 【乘算阶段】最终伤害倍率 (类似易伤、双倍增伤、斩杀加成)
    default float onAllyModifyDamageFinal(AbstractAlly ally, AbstractCreature target, float damage,
            DamageInfo.DamageType type) {
        return damage;
    }
}