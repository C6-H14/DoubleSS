package SS.packages.BluePackage;

import java.util.ArrayList;
import SS.path.PackageEnumList.PackageEnum;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class BluePackage_e extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("BluePackage_e");

    public BluePackage_e() {
        super(ID, TYPE, PackageEnum.BLUE, "Double:BLUE_option", "Double:CorePieces");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Turbo");
        cards.add("Undo");
        cards.add("Skim");
        cards.add("Self Repair");
        cards.add("Buffer");

        cards.add("Barrage");
        cards.add("Recycle");
        cards.add("Reprogram");
        cards.add("Echo Form");
        cards.add("Creative AI");
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
        return new BluePackage_e();
    }

}
