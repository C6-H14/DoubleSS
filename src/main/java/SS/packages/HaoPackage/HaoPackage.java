package SS.packages.HaoPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.helper.SynergismGraph;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class HaoPackage extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("HaoPackage");

    public HaoPackage() {
        super(ID, TYPE, PackageEnum.Hao, "Double:Hao_Green_option", "Double:GreenApple");
        addSyng(PackageEnum.Shock, SynergismGraph.SynTag.Teacher);
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
        SubPackages.put(PackageType.VALUE, new HaoPackage_v());
        SubPackages.put(PackageType.CONSISTENCY, new HaoPackage_c());
        SubPackages.put(PackageType.CEILING, new HaoPackage_e());
    }

    public AbstractPackage makeCopy() {
        return new HaoPackage();
    }

}
