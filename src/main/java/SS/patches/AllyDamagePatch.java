package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.DamageInfo;

@SpirePatch(clz = DamageInfo.class, method = "<class>")
public class AllyDamagePatch {
    // 向原版 DamageInfo 类中注入一个成员变量，默认值为 false
    public static SpireField<Boolean> allowFriendlyFire = new SpireField<>(() -> false);
}