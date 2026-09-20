package SS.stats;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件牌触发率注册表（spec 模块 1：门槛/条件触发率）。
 * 每张"有条件才生效"的牌注册一个谓词：onPlay 时 total++，谓词成立 met++。
 * 新增条件牌只需 register 一条，不改 CardStats。
 */
public class CardConditionRegistry {

    public interface Condition {
        /** 显示在导出行里的条件名。 */
        String label();

        /** 打出瞬间判定条件是否满足（与牌 use() 的门槛同式）。 */
        boolean met(AbstractCard card, AbstractMonster target);
    }

    private static final Map<String, Condition> CONDITIONS = new HashMap<>();

    public static void register(String cardId, Condition c) {
        CONDITIONS.put(cardId, c);
    }

    public static Condition lookup(String cardId) {
        return cardId == null ? null : CONDITIONS.get(cardId);
    }

    static {
        // 三圣颂：use() 门槛 getVirtue() < magicNumber 时直接 return（Trisagion.java:49）。
        // getVirtue() = -countPower("Double:SinsPower")（AbstractC6H14Card:79，protected 跨包不可调），
        // 此处单行复刻保持同式——若 getVirtue 改逻辑需同步。magicNumber 固定 10（setMagic(10)）。
        register("Double:Trisagion", new Condition() {
            @Override
            public String label() {
                return "美德≥10";
            }

            @Override
            public boolean met(AbstractCard card, AbstractMonster target) {
                AbstractPower sins = AbstractDungeon.player.getPower("Double:SinsPower");
                int virtue = (sins == null) ? 0 : -sins.amount;
                return virtue >= card.magicNumber;
            }
        });
        // 巨浪：目标带易伤时每段伤害返 1 能量并叠 1 虚弱（GreatWave.java:53-58）
        register("Double:GreatWave", new Condition() {
            @Override
            public String label() {
                return "目标带易伤";
            }

            @Override
            public boolean met(AbstractCard card, AbstractMonster target) {
                return target != null && target.hasPower("Vulnerable");
            }
        });
    }
}
