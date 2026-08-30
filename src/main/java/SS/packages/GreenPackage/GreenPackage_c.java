package SS.packages.GreenPackage;

import java.util.ArrayList;
import SS.path.PackageEnumList.PackageEnum;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class GreenPackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CONSISTENCY;
    public static String ID = ModHelper.makePath("GreenPackage_c");

    public GreenPackage_c() {
        super(ID, TYPE, PackageEnum.GREEN, "Double:GREEN_option", "Double:HalfRingOfTheSnake", "Ring of the Serpent");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : GreenPackage.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("Tactician");
        cards.add("Well Laid Plans");
        cards.add("Calculated Gamble");
        cards.add("Adrenaline");
        cards.add("Expertise");
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
        return "Neutralize";
    }

    public AbstractPackage makeCopy() {
        return new GreenPackage_c();
    }

}
