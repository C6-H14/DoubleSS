package SS.packages.HaoPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.AbstractCardEnum;

public class HaoPackage_e extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("HaoPackage_e");

    public HaoPackage_e() {
        super(ID, TYPE, AbstractCardEnum.Hao_Green, "Double:Hao_Green_option", "Double:GreenApple");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Double:Dishaovery");
        cards.add("Double:GreatDisciple");
        cards.add("Double:HaoBludgeon");
        cards.add("Double:HaoTap");
        cards.add("Double:MaximizeHaoCard");

        cards.add("Double:Haoflex");
        cards.add("Double:HaoLive");
        cards.add("Double:YourAttack");
        cards.add("Double:DiverseLover");
        cards.add("Double:BossSwapGod");
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
        return new HaoPackage_e();
    }

}
