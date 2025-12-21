package SS.packages.GreenPackage;

import java.util.ArrayList;
import SS.path.PackageEnumList.PackageEnum;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class GreenPackage_e extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("GreenPackage_e");

    public GreenPackage_e() {
        super(ID, TYPE, PackageEnum.GREEN, "Double:GREEN_option", "Double:HalfRingOfTheSnake");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Acrobatics");
        cards.add("Reflex");
        cards.add("PiercingWail");
        cards.add("Eviscerate");
        cards.add("Prepared");

        cards.add("Grand Finale");
        cards.add("Finisher");
        cards.add("Wraith Form v2");
        cards.add("Catalyst");
        cards.add("Night Terror");
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

    public AbstractPackage makeCopy() {
        return new GreenPackage_e();
    }

}
