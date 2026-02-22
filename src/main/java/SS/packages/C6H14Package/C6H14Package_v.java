package SS.packages.C6H14Package;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class C6H14Package_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("C6H14Package_v");

    public C6H14Package_v() {
        super(ID, TYPE, PackageEnum.C6H14, "Double:C6H14_Cyan_option", "Double:JarOfWisps");
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
        return new C6H14Package_v();
    }

}
