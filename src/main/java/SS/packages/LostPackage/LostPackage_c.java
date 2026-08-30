package SS.packages.LostPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class LostPackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CONSISTENCY;
    public static String ID = ModHelper.makePath("LostPackage_c");

    public LostPackage_c() {
        super(ID, TYPE, PackageEnum.Lost, "Double:Lost_Black_option", "Double:WoodenCross", "Double:HolyMantle");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : LostPackage.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("Double:SpectreEcho");
        cards.add("Double:Incorporeal");
        cards.add("Double:Lifeline");
        cards.add("Double:PopIn");
        cards.add("Double:Unbreakable");
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
        return "Double:Ferry";
    }

    public AbstractPackage makeCopy() {
        return new LostPackage_c();
    }

}
