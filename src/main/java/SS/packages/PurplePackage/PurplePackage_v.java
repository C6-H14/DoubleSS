package SS.packages.PurplePackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class PurplePackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("PurplePackage_v");

    public PurplePackage_v() {
        super(ID, TYPE, AbstractCard.CardColor.PURPLE, "Double:PURPLE_option", "Double:BathWater");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Tantrum");
        cards.add("FearNoEvil");
        cards.add("EmptyMind");
        cards.add("MasterReality");
        cards.add("Vault");

        cards.add("BowlingBash");
        cards.add("Wallop");
        cards.add("Fasting2");
        cards.add("Conclude");
        cards.add("Blasphemy");
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
        return new PurplePackage_v();
    }

}
