package SS.packages.LostPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class LostPackage_e extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("LostPackage_e");

    public LostPackage_e() {
        super(ID, TYPE, PackageEnum.Lost, "Double:Lost_Black_option", "Double:WoodenCross", "Double:HolyMantle");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : LostPackage.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("Double:SpectreEcho");
        cards.add("Double:Incorporeal");
        cards.add("Double:Resurrection");
        cards.add("Double:Perseverance");
        cards.add("Double:Godhead");
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
        return new LostPackage_e();
    }

}
