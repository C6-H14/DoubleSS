package SS.packages;

import java.util.ArrayList;
import java.util.HashMap;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardColor;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import SS.modcore.modcore;

public abstract class AbstractPackage {
    public static final Logger logger = LogManager.getLogger(AbstractPackage.class);
    public HashMap<PackageType, AbstractPackage> SubPackages = new HashMap<PackageType, AbstractPackage>();
    public ArrayList<AbstractCard> CardLists = new ArrayList<AbstractCard>();
    public ArrayList<AbstractRelic> RelicLists = new ArrayList<AbstractRelic>();
    public ArrayList<AbstractMonster> MonsterLists = new ArrayList<AbstractMonster>();
    public AbstractRelic StartRelic;
    public AbstractCard OptionCard;
    public PackageType TYPE;
    public String ID;
    public CardColor PackageColor;

    public AbstractPackage(String id, PackageType type, CardColor col, String optioncard, String startRelic) {
        this.ID = id;
        this.TYPE = type;
        this.PackageColor = col;
        AbstractCard c = CardLibrary.getCard(optioncard);
        this.OptionCard = c.makeStatEquivalentCopy();
        modcore.cardParentMap.put(c.cardID, this.ID);
        modcore.cardClassParentMap.put(c.getClass(), this.ID);
        this.StartRelic = RelicLibrary.getRelic(startRelic).makeCopy();
        initializePack();
    }

    public abstract ArrayList<String> getCards();

    public abstract ArrayList<String> getRelics();

    public abstract ArrayList<String> getMonsters();

    public abstract AbstractPackage makeCopy();

    public void initializePack() {
        for (String s : getCards()) {
            AbstractCard c = CardLibrary.getCard(s);
            if (c == null) {
                logger.info("Can't find card for package(" + this.ID + "):" + s);
            }
            this.CardLists.add(c.makeStatEquivalentCopy());
            modcore.cardParentMap.put(c.cardID, this.ID);
            modcore.cardClassParentMap.put(c.getClass(), this.ID);
        }
        for (String s : getRelics()) {
            AbstractRelic r = RelicLibrary.getRelic(s);
            if (r == null) {
                logger.info("Can't find relic for package(" + this.ID + "):" + s);
            }
            this.RelicLists.add(r.makeCopy());
        }
        if (this.TYPE == PackageType.MAIN) {
            initializeSubPackage();
        }
    }

    protected abstract void initializeSubPackage();

    public enum PackageType {
        MAIN, VALUE, CONSISTENCY, CEILING;
    }
}
