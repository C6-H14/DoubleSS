package SS.packages.C6H14Package;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class C6H14Package_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CONSISTENCY;
    public static String ID = ModHelper.makePath("C6H14Package_c");

    public C6H14Package_c() {
        super(ID, TYPE, PackageEnum.C6H14, "Double:C6H14_Cyan_option", "Double:JarOfWisps");
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
        cards.add("Double:Lifeline");
        cards.add("Double:PopIn");
        cards.add("Double:Unbreakable");
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
        return new C6H14Package_c();
    }

}
