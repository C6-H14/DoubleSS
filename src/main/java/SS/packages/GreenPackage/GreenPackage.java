package SS.packages.GreenPackage;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;

import SS.helper.ModHelper;
import SS.packages.AbstractPackage;

public class GreenPackage extends AbstractPackage {
    public static PackageType TYPE = PackageType.MAIN;
    public static String ID = ModHelper.makePath("GreenPackage");

    public GreenPackage() {
        super(ID, TYPE, AbstractCard.CardColor.GREEN, "Double:GREEN_option", "Double:HalfRingOfTheSnake");
    }

    public ArrayList<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            cards.addAll(pack.getCards());
        }
        return cards;
    }

    public ArrayList<String> getRelics() {
        ArrayList<String> relics = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            relics.addAll(pack.getRelics());
        }
        return relics;
    }

    public ArrayList<String> getMonsters() {
        ArrayList<String> monsters = new ArrayList<>();
        for (AbstractPackage pack : SubPackages.values()) {
            monsters.addAll(pack.getMonsters());
        }
        return monsters;
    }

    protected void initializeSubPackage() {
        SubPackages.put(PackageType.VALUE, new GreenPackage_v());
        SubPackages.put(PackageType.CONSISTENCY, new GreenPackage_c());
        SubPackages.put(PackageType.CEILING, new GreenPackage_e());
    }

    public AbstractPackage makeCopy() {
        return new GreenPackage();
    }

}
