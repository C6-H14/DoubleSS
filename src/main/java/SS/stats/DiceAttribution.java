package SS.stats;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.Dice.AbstractDice;

import java.util.ArrayList;

/**
 * 战斗统计用：一次骰子结算的归属快照。
 *
 * 骰子激发后被销毁（槽位换成 EmptyDiceSlot），而伤害/格挡几帧后才结算，
 * 因此结算载体（DamageInfo / DamageAllEnemiesAction / GainBlockAction）
 * 上挂的是本快照而非骰子活引用：
 * - diceId/diceName → 骰子统计行（orbId × 幕 聚合）
 * - sources        → 来源牌均分（充能瞬间标注：卡牌打出→栈顶牌；
 *                    power 产骰→施加该 power 的牌列表；遗物→空→无归属桶）
 *
 * EternalAttack/EternalDefendDice 的 NextTurnDamage/BlockPower 也携带本快照
 * （构造 power 时从骰子拷贝），回合开始的延迟伤害/格挡同样归到骰子行与来源牌。
 */
public class DiceAttribution {
    public final String diceId;
    public final String diceName;
    public final ArrayList<AbstractCard> sources;

    public DiceAttribution(String diceId, String diceName, ArrayList<AbstractCard> sources) {
        this.diceId = diceId;
        this.diceName = diceName;
        this.sources = sources != null ? sources : new ArrayList<>();
    }

    /** 从活骰子构造快照（拷贝 sources，防后续改动）。 */
    public static DiceAttribution of(AbstractDice dice) {
        return new DiceAttribution(dice.ID, dice.name,
                dice.sources != null ? new ArrayList<>(dice.sources) : new ArrayList<>());
    }
}
