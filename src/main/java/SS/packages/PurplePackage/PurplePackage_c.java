package SS.packages.PurplePackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class PurplePackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("PurplePackage_c");

    public PurplePackage_c() {
        super(ID, TYPE, AbstractCard.CardColor.RED, "Double:PURPLE_option", "Double:BathWater");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Tantrum");
        cards.add("FearNoEvil");
        cards.add("EmptyMind");
        cards.add("MasterReality");
        cards.add("Vault");

        cards.add("ThirdEye");
        cards.add("InnerPeace");
        cards.add("Vengeance");
        cards.add("Adaptation");
        cards.add("Scrawl");
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
        return new PurplePackage_c();
    }

}
