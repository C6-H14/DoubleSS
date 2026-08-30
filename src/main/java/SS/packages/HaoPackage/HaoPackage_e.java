package SS.packages.HaoPackage;

import java.util.ArrayList;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

public class HaoPackage_e extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("HaoPackage_e");

    public HaoPackage_e() {
        super(ID, TYPE, PackageEnum.Hao, "Double:Hao_Green_option", "Double:GreenApple", "Double:GoldenApple");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (String c : HaoPackage.COMMON_CARDS) {
            cards.add(c);
        }

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

    public String getStarterCard() {
        return "Double:BossSwap";
    }

    public AbstractPackage makeCopy() {
        return new HaoPackage_e();
    }

}
