package SS.packages.LostPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class LostPackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("LostPackage_v");

    public LostPackage_v() {
        super(ID, TYPE, PackageEnum.Lost, "Double:Lost_Black_option", "Double:WoodenCross");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Double:BlankCard");
        cards.add("Double:TwoChargeVoid");
        cards.add("Double:SoulGuard");
        cards.add("Double:LastBreath");
        cards.add("Double:CounterBlow");

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

    public AbstractPackage makeCopy() {
        return new LostPackage_v();
    }

}
