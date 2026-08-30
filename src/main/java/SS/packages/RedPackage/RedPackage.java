package SS.packages.RedPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class RedPackage extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("RedPackage");

    // 三个子卡包(_v/_c/_e)共同的前 5 张牌，修改共同牌只需改这里
    public static final String[] COMMON_CARDS = {
        "Pommel Strike",
        "Iron Wave",
        "Dark Embrace",
        "Inflame",
        "True Grit",
    };

    public RedPackage() {
        super(ID, TYPE, PackageEnum.RED, "Double:RED_option", "Double:BoilingBlood", "Black Blood");
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
        return "Bash";
    }

    protected void initializeSubPackage() {
        SubPackages.put(PackageType.VALUE, new RedPackage_v());
        SubPackages.put(PackageType.CONSISTENCY, new RedPackage_c());
        SubPackages.put(PackageType.CEILING, new RedPackage_e());
    }

    public AbstractPackage makeCopy() {
        return new RedPackage();
    }

}
