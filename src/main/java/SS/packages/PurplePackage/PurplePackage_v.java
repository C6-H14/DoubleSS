package SS.packages.PurplePackage;

import java.util.ArrayList;
import SS.path.PackageEnumList.PackageEnum;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class PurplePackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("PurplePackage_v");

    public PurplePackage_v() {
        super(ID, TYPE, PackageEnum.PURPLE, "Double:PURPLE_option", "Double:BathWater", "HolyWater");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : PurplePackage.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("BowlingBash");
        cards.add("Wallop");
        cards.add("Fasting2");
        cards.add("Conclude");
        cards.add("Blasphemy");
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
        return "Vigilance";
    }

    public AbstractPackage makeCopy() {
        return new PurplePackage_v();
    }

}
