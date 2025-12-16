package SS.packages.ShockPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.AbstractCardEnum;

public class ShockPackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("ShockPackage_v");

    public ShockPackage_v() {
        super(ID, TYPE, AbstractCardEnum.Shock_Blue, "Double:Shock_Blue_option", "Double:MassSpring");
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
        return new ShockPackage_v();
    }

}
