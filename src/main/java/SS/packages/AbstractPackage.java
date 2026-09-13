package SS.packages;

import java.util.ArrayList;
import java.util.HashMap;
import SS.path.PackageEnumList.PackageEnum;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import SS.cards.AbstractDoubleCard;
import SS.modcore.modcore;

public abstract class AbstractPackage {
    public static final Logger logger = LogManager.getLogger(AbstractPackage.class);
    public HashMap<PackageType, AbstractPackage> SubPackages = new HashMap<PackageType, AbstractPackage>();
    public ArrayList<AbstractCard> CardLists = new ArrayList<AbstractCard>();
    public ArrayList<AbstractRelic> RelicLists = new ArrayList<AbstractRelic>();
    public ArrayList<AbstractMonster> MonsterLists = new ArrayList<AbstractMonster>();
    public AbstractRelic StartRelic;
    public AbstractRelic BossRelic;
    public AbstractDoubleCard OptionCard;
    public PackageType TYPE;
    public String ID;
    public PackageEnum PackageColor;
    // 卡包协同卡声明：key=有序对的第二个元素（主卡包色），value=卡ID。方向敏感：
    // 仅当「本卡包色在前、key 色在后」这一方向被枚举到时对应卡进本局卡池
    // （选哪两个包与顺序无关——两方向都会被枚举，但只有声明过的那方向产出卡）。
    // 例：ShockPackage 里 addPairCard(PackageEnum.Hao, "Double:TA")
    // 表示 Shock 与 Hao 同选时（枚举到 (Shock, Hao)）解锁 Double:TA。
    public HashMap<PackageEnum, String> pairCards = new HashMap<>();

    public AbstractPackage(String id, PackageType type, PackageEnum col, String optioncard, String startRelic) {
        this.ID = id;
        this.TYPE = type;
        this.PackageColor = col;
        AbstractCard c = CardLibrary.getCard(optioncard);
        if (c instanceof AbstractDoubleCard) {
            this.OptionCard = ((AbstractDoubleCard) c).makeCopy();
        }
        modcore.cardParentMap.put(c.cardID, this.ID);
        modcore.cardClassParentMap.put(c.getClass(), this.ID);
        this.StartRelic = RelicLibrary.getRelic(startRelic).makeCopy();
        this.BossRelic = null;
        initializePack();
    }

    public AbstractPackage(String id, PackageType type, PackageEnum col, String optioncard, String startRelic,
            String bossRelic) {
        this.ID = id;
        this.TYPE = type;
        this.PackageColor = col;
        AbstractCard c = CardLibrary.getCard(optioncard);
        if (c instanceof AbstractDoubleCard) {
            this.OptionCard = ((AbstractDoubleCard) c).makeCopy();
        }
        modcore.cardParentMap.put(c.cardID, this.ID);
        modcore.cardClassParentMap.put(c.getClass(), this.ID);
        this.StartRelic = RelicLibrary.getRelic(startRelic).makeCopy();
        this.BossRelic = RelicLibrary.getRelic(bossRelic).makeCopy();
        initializePack();
    }

    public abstract ArrayList<String> getCards();

    public abstract ArrayList<String> getRelics();

    public abstract ArrayList<String> getMonsters();

    public abstract String getStarterCard();

    public abstract AbstractPackage makeCopy();

    /** 声明「本卡包色与 second 色同选」时解锁的卡（见 pairCards 字段注释）。 */
    public void addPairCard(PackageEnum second, String cardId) {
        pairCards.put(second, cardId);
    }

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
