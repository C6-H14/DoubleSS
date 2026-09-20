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
        // 激发归属覆盖：Seething/Blitzkrieg 这类「额外激发场上已有骰子」的牌，
        // 激发时把当前牌设为覆盖牌——被激发的骰子 sources 指向的是当初**产**它的牌，
        // 但这一遍伤害是因本次激发而生的，应记在激发牌头上（用户已定口径）。
        // 覆盖只作用于本次 onEvoke 调用期间，见 CardStats.evokeAttribution。
        return new DiceAttribution(dice.ID, dice.name, evokers(dice));
    }

    /** 取 sources：有激发覆盖牌时用覆盖牌（单元素），否则拷贝骰子自身的 sources。 */
    private static ArrayList<AbstractCard> evokers(AbstractDice dice) {
        AbstractCard evoker = CardStats.currentEvoker();
        if (evoker != null) {
            ArrayList<AbstractCard> one = new ArrayList<>();
            one.add(evoker);
            return one;
        }
        return dice.sources != null ? new ArrayList<>(dice.sources) : new ArrayList<>();
    }
}
