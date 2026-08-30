package SS.packages.LostPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class LostPackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("LostPackage_v");

    public LostPackage_v() {
        super(ID, TYPE, PackageEnum.Lost, "Double:Lost_Black_option", "Double:WoodenCross", "Double:HolyMantle");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : LostPackage.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("Double:Altar");
        cards.add("Double:GhostBomb");
        cards.add("Double:StratifiedStorm");
        cards.add("Double:TrinityShield");
        cards.add("Double:FallenPact");
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
        return new LostPackage_v();
    }

}
