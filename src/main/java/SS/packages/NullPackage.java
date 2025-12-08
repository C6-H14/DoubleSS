package SS.packages;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;

public class NullPackage extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("NullPackage");

    public NullPackage() {
        super(ID, TYPE, AbstractCard.CardColor.RED, "Double:RED_option", "Double:BoilingBlood");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
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

    public AbstractPackage makeCopy() {
        return new NullPackage();
    }

    protected void initializeSubPackage() {
    }

}
