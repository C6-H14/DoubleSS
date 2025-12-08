package SS.packages.LostPackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;
import SS.path.AbstractCardEnum;

public class LostPackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CONSISTENCY;
    public static String ID = ModHelper.makePath("LostPackage_c");

    public LostPackage_c() {
        super(ID, TYPE, AbstractCardEnum.Lost_Black, "Double:Lost_Black_option", "Double:WoodenCross");
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
        return new LostPackage_c();
    }

}
