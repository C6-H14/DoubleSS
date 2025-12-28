package SS.packages.ShockPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class ShockPackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("ShockPackage_c");

    public ShockPackage_c() {
        super(ID, TYPE, PackageEnum.Shock, "Double:Shock_Blue_option", "Double:MassSpring");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Double:UnstableShockwave");
        cards.add("Double:Blitzkrieg");
        cards.add("Double:Resonance");
        cards.add("Double:OffenseAsDefense");
        cards.add("Double:TA");

        cards.add("Double:Nightfall");
        cards.add("Double:GreatWave");
        cards.add("Double:SlowCook");
        cards.add("Double:NightOps");
        cards.add("Double:ArmillarySphere");
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
        return new ShockPackage_c();
    }

}
