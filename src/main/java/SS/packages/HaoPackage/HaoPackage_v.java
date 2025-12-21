package SS.packages.HaoPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class HaoPackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("HaoPackage_v");

    public HaoPackage_v() {
        super(ID, TYPE, PackageEnum.Hao, "Double:Hao_Green_option", "Double:GreenApple");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Double:Dishaovery");
        cards.add("Double:GreatDisciple");
        cards.add("Double:HaoBludgeon");
        cards.add("Double:HaoTap");
        cards.add("Double:MaximizeHaoCard");

        cards.add("Double:HaoStab");
        cards.add("Double:Haoggernaut");
        cards.add("Double:Prism");
        cards.add("Double:margropeR");
        cards.add("Double:ScrapeBy");
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
        return new HaoPackage_v();
    }

}
