package SS.packages.ShockPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class ShockPackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("ShockPackage_v");

    public ShockPackage_v() {
        super(ID, TYPE, PackageEnum.Shock, "Double:Shock_Blue_option", "Double:MassSpring", "Double:Pendulum");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : ShockPackage.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("Double:ShootingStar");
        cards.add("Double:Absorb");
        cards.add("Double:FeelNoShame");
        cards.add("Double:EchoOfShock");
        cards.add("Double:DonQuixoticCharge");
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
        return "Double:YieldPoint";
    }

    public AbstractPackage makeCopy() {
        return new ShockPackage_v();
    }

}
