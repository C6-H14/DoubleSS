package SS.packages.LostPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.AbstractCardEnum;

public class LostPackage_e extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("LostPackage_e");

    public LostPackage_e() {
        super(ID, TYPE, AbstractCardEnum.Lost_Black, "Double:Lost_Black_option", "Double:WoodenCross");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Double:BlankCard");
        cards.add("Double:TwoChargeVoid");
        cards.add("Double:SoulGuard");
        cards.add("Double:LastBreath");
        cards.add("Double:CounterBlow");

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

    public AbstractPackage makeCopy() {
        return new LostPackage_e();
    }

}
