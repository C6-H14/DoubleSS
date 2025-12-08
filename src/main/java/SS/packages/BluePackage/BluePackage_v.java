package SS.packages.BluePackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class BluePackage_v extends AbstractPackage {
    public static PackageType TYPE = PackageType.VALUE;
    public static String ID = ModHelper.makePath("BluePackage_v");

    public BluePackage_v() {
        super(ID, TYPE, AbstractCard.CardColor.RED, "Double:BLUE_option", "Double:CorePieces");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("Turbo");
        cards.add("Undo");
        cards.add("Skim");
        cards.add("Self Repair");
        cards.add("Buffer");

        cards.add("Beam Cell");
        cards.add("Streamline");
        cards.add("Reinforced Body");
        cards.add("Sunder");
        cards.add("Hyperbeam");
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
        return new BluePackage_v();
    }

}
