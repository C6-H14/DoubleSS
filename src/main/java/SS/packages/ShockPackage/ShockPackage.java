package SS.packages.ShockPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.helper.SynergismGraph;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class ShockPackage extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("ShockPackage");

    // 三个子卡包(_v/_c/_e)共同的前 5 张牌，修改共同牌只需改这里
    public static final String[] COMMON_CARDS = {
        "Double:UnstableShockwave",
        "Double:Blitzkrieg",
        "Double:Resonance",
        "Double:OffenseAsDefense",
        "Double:TA",
    };

    public ShockPackage() {
        super(ID, TYPE, PackageEnum.Shock, "Double:Shock_Blue_option", "Double:MassSpring", "Double:Pendulum");
        addSyng(PackageEnum.Hao, SynergismGraph.SynTag.Student);
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

    public String getStarterCard() {
        return "Double:YieldPoint";
    }

    protected void initializeSubPackage() {
        SubPackages.put(PackageType.VALUE, new ShockPackage_v());
        SubPackages.put(PackageType.CONSISTENCY, new ShockPackage_c());
        SubPackages.put(PackageType.CEILING, new ShockPackage_e());
    }

    public AbstractPackage makeCopy() {
        return new ShockPackage();
    }

}
