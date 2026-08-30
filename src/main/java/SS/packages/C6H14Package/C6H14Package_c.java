package SS.packages.C6H14Package;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class C6H14Package_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CONSISTENCY;
    public static String ID = ModHelper.makePath("C6H14Package_c");

    public C6H14Package_c() {
        super(ID, TYPE, PackageEnum.C6H14, "Double:C6H14_Cyan_option", "Double:JarOfWisps", "Double:BookOfVirtue");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : C6H14Package.COMMON_CARDS) {
            cards.add(c);
        }

        cards.add("Double:ExactSequence");
        cards.add("Double:KernelMapping");
        cards.add("Double:Eraser");
        cards.add("Double:SoulStorage");
        cards.add("Double:Sunbow");
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
        return "Double:SpiritualCompanionship";
    }

    public AbstractPackage makeCopy() {
        return new C6H14Package_c();
    }

}
