package SS.packages.RedPackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class RedPackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CONSISTENCY;
    public static String ID = ModHelper.makePath("RedPackage_c");

    public RedPackage_c() {
        super(ID, TYPE, AbstractCard.CardColor.RED, "Double:RED_option", "Double:BoilingBlood");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Pommel Strike");
        cards.add("Iron Wave");
        cards.add("Dark Embrace");
        cards.add("Inflame");
        cards.add("True Grit");

        cards.add("Battle Trance");
        cards.add("Corruption");
        cards.add("Offering");
        cards.add("Burning Pact");
        cards.add("Havoc");
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
        return new RedPackage_c();
    }

}
