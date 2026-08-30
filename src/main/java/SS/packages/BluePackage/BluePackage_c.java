package SS.packages.BluePackage;

import java.util.ArrayList;
import SS.path.PackageEnumList.PackageEnum;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class BluePackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CONSISTENCY;
    public static String ID = ModHelper.makePath("BluePackage_c");

    public BluePackage_c() {
        super(ID, TYPE, PackageEnum.BLUE, "Double:BLUE_option", "Double:CorePieces", "Double:CorePieces");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : BluePackage.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("Hologram");
        cards.add("Heatsinks");
        cards.add("Double Energy");
        cards.add("All For One");
        cards.add("Seek");
        return cards;
    }

    public ArrayList<String> getRelics() {
        ArrayList<String> relics = new ArrayList<>();
        return relics;
    }

    public ArrayList<String> getMonsters() {
        ArrayList<String> monsters = new ArrayList<>();
        return monsters;
    }

    protected void initializeSubPackage() {
    }

    public String getStarterCard() {
        return "Charge Battery";
    }

    public AbstractPackage makeCopy() {
        return new BluePackage_c();
    }

}
