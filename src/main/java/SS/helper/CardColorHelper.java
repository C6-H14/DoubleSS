package SS.helper;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.cards.AbstractDoubleCard;
import SS.path.AbstractCardEnum;

/**
 * 卡牌"同色"判定。
 *
 * <p>二硫键的所有卡都挂在同一个 {@link AbstractCardEnum#SS_Yellow} 颜色下，真正区分
 * 卡包的是 {@link AbstractDoubleCard#packagetype}。所以"同色"不能只比 {@code color}：
 * 两张 SS_Yellow 牌必须卡包类型也相同才算同色，否则就是原版那种直接比 {@code color}。
 *
 * <p>原先这逻辑内联在 {@code SelfReliantAction} 里，SermonPower 也要用，抽出来共用。
 */
public class CardColorHelper {

    /** 两张牌是否同色。任一为 null 返回 false。 */
    public static boolean isSameColor(AbstractCard a, AbstractCard b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.color == AbstractCardEnum.SS_Yellow && b.color == AbstractCardEnum.SS_Yellow) {
            if (a instanceof AbstractDoubleCard && b instanceof AbstractDoubleCard) {
                return ((AbstractDoubleCard) a).packagetype == ((AbstractDoubleCard) b).packagetype;
            }
        }
        return a.color == b.color;
    }

    /**
     * 取一张牌所属的"色组"标识，用于哈希/去重。
     *
     * <p>SS_Yellow 牌返回其卡包类型（每个枚举常量唯一），其余返回 {@code color} 的序号。
     * 这两类值的取值范围不重叠（枚举序号都很小，卡包类型用负数偏移），所以在同一个
     * 集合里混放也不会误判相等。
     */
    public static int colorKey(AbstractCard c) {
        if (c == null) {
            return Integer.MIN_VALUE;
        }
        if (c.color == AbstractCardEnum.SS_Yellow && c instanceof AbstractDoubleCard) {
            return -1 - ((AbstractDoubleCard) c).packagetype.ordinal();
        }
        return c.color.ordinal();
    }
}
