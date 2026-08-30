package SS.packages.LostPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class LostPackage extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("LostPackage");

    // 三个子卡包(_v/_c/_e)共同的前 5 张牌，修改共同牌只需改这里
    public static final String[] COMMON_CARDS = {
        "Double:BlankCard",
        "Double:TwoChargeVoid",
        "Double:SoulGuard",
        "Double:LastBreath",
        "Double:CounterBlow",
    };

    public LostPackage() {
        super(ID, TYPE, PackageEnum.Lost, "Double:Lost_Black_option", "Double:WoodenCross", "Double:HolyMantle");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            cards.addAll(pack.getCards());
        }
        return cards;
    }

    public ArrayList<String> getRelics() {
        ArrayList<String> relics = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            relics.addAll(pack.getRelics());
        }
        return relics;
    }

    public ArrayList<String> getMonsters() {
        ArrayList<String> monsters = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            monsters.addAll(pack.getMonsters());
        }
        return monsters;
    }

    public String getStarterCard() {
        return "Double:Ferry";
    }

    protected void initializeSubPackage() {
        SubPackages.put(PackageType.VALUE, new LostPackage_v());
        SubPackages.put(PackageType.CONSISTENCY, new LostPackage_c());
        SubPackages.put(PackageType.CEILING, new LostPackage_e());
    }

    public AbstractPackage makeCopy() {
        return new LostPackage();
    }

}
