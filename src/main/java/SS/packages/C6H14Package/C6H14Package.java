package SS.packages.C6H14Package;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class C6H14Package extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("C6H14Package");

    // 三个子卡包(_v/_c/_e)共同的前 5 张牌，修改共同牌只需改这里
    public static final String[] COMMON_CARDS = {
        "Double:VirtualChoir",
        "Double:PaperPenance",
        "Double:Guidance",
        "Double:Kaleidoscope",
        "Double:BrainStorming",
    };

    public C6H14Package() {
        super(ID, TYPE, PackageEnum.C6H14, "Double:C6H14_Cyan_option", "Double:JarOfWisps", "Double:BookOfVirtue");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            cards.addAll(pack.getCards());
        }
        return cards;
    }

    public ArrayList<String> getRelics() {
        ArrayList<String> relics = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            relics.addAll(pack.getRelics());
        }
        return relics;
    }

    public ArrayList<String> getMonsters() {
        ArrayList<String> monsters = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            monsters.addAll(pack.getMonsters());
        }
        return monsters;
    }

    protected void initializeSubPackage() {
        SubPackages.put(PackageType.VALUE, new C6H14Package_v());
        SubPackages.put(PackageType.CONSISTENCY, new C6H14Package_c());
        SubPackages.put(PackageType.CEILING, new C6H14Package_e());
    }

    public String getStarterCard() {
        return "Double:SpiritualCompanionship";
    }

    public AbstractPackage makeCopy() {
        return new C6H14Package();
    }

}
