package SS.packages.GreenPackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class GreenPackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("GreenPackage_v");

    public GreenPackage_v() {
        super(ID, TYPE, AbstractCard.CardColor.GREEN, "Double:GREEN_option", "Double:HalfRingOfTheSnake");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Acrobatics");
        cards.add("Reflex");
        cards.add("PiercingWail");
        cards.add("Eviscerate");
        cards.add("Prepared");

        cards.add("Dash");
        cards.add("Phantasmal Killer");
        cards.add("All Out Attack");
        cards.add("Dagger Throw");
        cards.add("Footwork");
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
        return new GreenPackage_v();
    }

}
