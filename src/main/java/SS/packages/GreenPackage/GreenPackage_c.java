package SS.packages.GreenPackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class GreenPackage_c extends AbstractPackage {
    public static PackageType TYPE = PackageType.CEILING;
    public static String ID = ModHelper.makePath("GreenPackage_c");

    public GreenPackage_c() {
        super(ID, TYPE, AbstractCard.CardColor.GREEN, "Double:GREEN_option", "Double:HalfRingOfTheSnake");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Acrobatics");
        cards.add("Reflex");
        cards.add("PiercingWail");
        cards.add("Eviscerate");
        cards.add("Prepared");

        cards.add("Tactician");
        cards.add("Well Laid Plans");
        cards.add("Calculated Gamble");
        cards.add("Adrenaline");
        cards.add("Expertise");
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
        return new GreenPackage_c();
    }

}
