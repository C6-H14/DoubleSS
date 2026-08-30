package SS.packages.BluePackage;

import java.util.ArrayList;
import SS.path.PackageEnumList.PackageEnum;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class BluePackage extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("BluePackage");

    // 三个子卡包(_v/_c/_e)共同的前 5 张牌，修改共同牌只需改这里
    public static final String[] COMMON_CARDS = {
        "Turbo",
        "Undo",
        "Skim",
        "Self Repair",
        "Buffer",
    };

    public BluePackage() {
        super(ID, TYPE, PackageEnum.BLUE, "Double:BLUE_option", "Double:CorePieces", "Double:CorePieces");
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
        return "Charge Battery";
    }

    protected void initializeSubPackage() {
        SubPackages.put(PackageType.VALUE, new BluePackage_v());
        SubPackages.put(PackageType.CONSISTENCY, new BluePackage_c());
        SubPackages.put(PackageType.CEILING, new BluePackage_e());
    }

    public AbstractPackage makeCopy() {
        return new BluePackage();
    }

}
