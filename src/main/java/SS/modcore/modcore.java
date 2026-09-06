package SS.modcore;

import basemod.AutoAdd;
import basemod.BaseMod;
import basemod.abstracts.CustomRelic;
import basemod.abstracts.CustomSavable;
import basemod.interfaces.EditCardsSubscriber;
import basemod.interfaces.EditCharactersSubscriber;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.ISubscriber;
import basemod.interfaces.OnCardUseSubscriber;
import basemod.interfaces.OnPowersModifiedSubscriber;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostExhaustSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import basemod.interfaces.RenderSubscriber;
import basemod.interfaces.StartGameSubscriber;
import javafx.util.Pair;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardColor;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.Keyword;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rewards.RewardSave;

import SS.UI.Sinsbar;
import SS.cards.AbstractDoubleCard;
import SS.cards.BlessCard.BlessStrike;
import SS.cards.Haohao.AbstractHaoCard;
import SS.characters.AbstractSSCharacter;
import SS.characters.MyCharacter;
import SS.helper.PermanentBlockVariable;
import SS.helper.PermanentDamageVariable;
import SS.helper.PermanentMagicNumberVariable;
import SS.helper.SynergismGraph;
import SS.helper.TempRelicManager;
import SS.packages.AbstractPackage;
import SS.packages.NullPackage;
import SS.packages.AbstractPackage.PackageType;
import SS.packages.BluePackage.BluePackage;
import SS.packages.C6H14Package.C6H14Package;
import SS.packages.GreenPackage.GreenPackage;
import SS.packages.HaoPackage.HaoPackage;
import SS.packages.LostPackage.LostPackage;
import SS.packages.PurplePackage.PurplePackage;
import SS.packages.RedPackage.RedPackage;
import SS.packages.RedPackage.RedPackage_v;
import SS.packages.ShockPackage.ShockPackage;
import SS.patches.CharacterSelectPackPatch;
import SS.patches.CenterGridCardSelectScreen;
import SS.stats.CardStats;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;
import SS.path.RewardEnum;
import SS.path.ThmodClassEnum;
import SS.relic.SS.LCysteine;
import SS.rewards.HaoReward;
import SS.rewards.RewardManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

@SpireInitializer
public class modcore implements EditCardsSubscriber, EditRelicsSubscriber, EditCharactersSubscriber,
        EditStringsSubscriber, EditKeywordsSubscriber, StartGameSubscriber, PostUpdateSubscriber,
        PostInitializeSubscriber, OnStartBattleSubscriber, OnCardUseSubscriber, RenderSubscriber,
        PostExhaustSubscriber, OnPowersModifiedSubscriber, CustomSavable<Integer> {
    private static final String BG_ATTACK_512 = "img/512/bg_attack.png";
    private static final String BG_POWER_512 = "img/512/bg_power.png";
    private static final String BG_SKILL_512 = "img/512/bg_skill.png";
    private static final String SMALL_ORB = "img/512/card_small_orb.png";
    private static final String BG_ATTACK_1024 = "img/1024/bg_attack.png";
    private static final String BG_POWER_1024 = "img/1024/bg_power.png";
    private static final String BG_SKILL_1024 = "img/1024/bg_skill.png";
    private static final String BIG_ORB = "img/1024/card_orb.png";
    private static final String ENEYGY_ORB = "img/512/card_orb.png";
    public static final Color COL = CardHelper.getColor(252, 235, 43);
    public static int Hao_chance = 0;
    public static int combatExhausts = 0;
    public static int orbitMisc = 0;
    public static int orbitMiscAtCombatStart = 0; // 本场战斗开始时的 orbitMisc 快照（不存档，战斗中 SL 时用于回滚）
    public static Sinsbar sinBar;

    private void addCardColor(CardColor c, String s) {
        BaseMod.addColor(c, COL, COL, COL, COL, COL, COL, COL, "img/512/" + s + "_attack.png",
                "img/512/" + s + "_skill.png",
                "img/512/" + s + "_power.png", "img/512/" + s + "_orb.png", "img/1024/" + s + "_attack.png",
                "img/1024/" + s + "_skill.png", "img/1024/" + s + "_power.png", "img/1024/" + s + "_orb.png",
                "img/512/" + s + "_small_orb.png");
    }

    public modcore() {
        BaseMod.subscribe((ISubscriber) this);
        BaseMod.addSaveField("Double:orbitMisc", this);
        BaseMod.addColor(AbstractCardEnum.SS_Yellow, COL, COL, COL, COL, COL, COL, COL, BG_ATTACK_512, BG_SKILL_512,
                BG_POWER_512, ENEYGY_ORB, BG_ATTACK_1024, BG_SKILL_1024, BG_POWER_1024, BIG_ORB, SMALL_ORB);
        new TempRelicManager();
        // addCardColor(AbstractCardEnum.Hao_Green, "hao");
        // addCardColor(AbstractCardEnum.Lost_Black, "lost");
        // addCardColor(AbstractCardEnum.Shock_Blue, "shock");
    }

    @Override
    public Integer onSave() {
        // 战斗进行中（phase==COMBAT 且未结束）被存档时——例如 QuickRestart 在战斗中 SL——
        // 必须存"本场战斗开始时"的快照值，保证读档重开战斗后 orbitMisc 回到进入房间时的状态，
        // 无法靠"战斗中打 Orbit + SL"刷高本局计数；
        // 房间边界（ENTER_ROOM，此刻 currRoom 还是上一房间）与战斗胜利后（POST_COMBAT，phase 已 COMPLETE）存当前值。
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room != null && room.phase == AbstractRoom.RoomPhase.COMBAT && !room.isBattleOver) {
            return orbitMiscAtCombatStart;
        }
        return orbitMisc;
    }

    @Override
    public void onLoad(Integer savedBonus) {
        // 读档时恢复数值。注意防空指针 (如果是第一次玩这个Mod，存档里没有这个字段)
        if (savedBonus != null) {
            orbitMisc = savedBonus;
        } else {
            orbitMisc = 0;
        }
    }

    /** 本局固定 3 个卡包槽位；人物选择界面可预置其中若干个，其余进游戏后选择。 */
    public static final int PACK_SLOTS = 3;
    private static int needPackage = PACK_SLOTS;
    public static SpireConfig config = null;

    public static void initialize() {
        new modcore();
    }

    // 注册
    @Override
    public void receiveEditStrings() {
        String lang = "ZHS";
        if (Settings.language == Settings.GameLanguage.ZHS) {
            lang = "ZHS";
        } else {
            lang = "ZHS";
        }
        BaseMod.loadCustomStringsFile(CardStrings.class, "localization/" + lang + "/cards.json");
        BaseMod.loadCustomStringsFile(OrbStrings.class, "localization/" + lang + "/orb.json");
        // BaseMod.loadCustomStrings(TutorialStrings.class, "localization/" + lang +
        // "/tutorial.json");
        BaseMod.loadCustomStringsFile(PowerStrings.class, "localization/" + lang + "/powers.json");
        BaseMod.loadCustomStringsFile(CharacterStrings.class, "localization/" + lang + "/characters.json");
        BaseMod.loadCustomStringsFile(RelicStrings.class, "localization/" + lang + "/relic.json");
        BaseMod.loadCustomStringsFile(UIStrings.class, "localization/" + lang + "/ui.json");
        BaseMod.loadCustomStringsFile(MonsterStrings.class, "localization/" + lang + "/monsters.json");
        BaseMod.loadCustomStringsFile(TutorialStrings.class, "localization/" + lang + "/tutorial.json");
    }

    @Override
    public void receiveEditKeywords() {
        Gson gson = new Gson();
        String lang = "ZHS";
        if (Settings.language == Settings.GameLanguage.ZHS) {
            lang = "ZHS";
        }
        String json = Gdx.files.internal("localization/" + lang + "/keywords.json")
                .readString(String.valueOf(StandardCharsets.UTF_8));
        Keyword[] keywords = (Keyword[]) gson.fromJson(json, Keyword[].class);
        if (keywords != null) {
            for (Keyword keyword : keywords) {
                BaseMod.addKeyword("double", keyword.NAMES[0], keyword.NAMES, keyword.DESCRIPTION);
            }
        }
    }

    @Override
    public void receiveEditCharacters() {
        BaseMod.addCharacter(new MyCharacter("SS"), "img/char/Character_Button.png", "img/char/Portrait.png",
                ThmodClassEnum.SS_CLASS);
    }

    @Override
    public void receiveEditRelics() {
        new AutoAdd("Double")
                .packageFilter(LCysteine.class)
                .any(CustomRelic.class, (info, relic) -> {
                    BaseMod.addRelicToCustomPool(relic, AbstractCardEnum.SS_Yellow);
                    if (info.seen) {
                        UnlockTracker.markRelicAsSeen(relic.relicId);
                    }
                });
    }

    @Override
    public void receiveEditCards() {
        new AutoAdd("Double")
                .packageFilter(AbstractDoubleCard.class)
                .setDefaultSeen(true)
                .cards();
        BaseMod.addDynamicVariable(new PermanentDamageVariable());
        BaseMod.addDynamicVariable(new PermanentBlockVariable());
        BaseMod.addDynamicVariable(new PermanentMagicNumberVariable());
    }

    // 以下为祝福机制相关
    public static HashMap<String, String> blessMap = new HashMap<>();

    private static void initializeBlessMap() {
        for (AbstractCard c : CardLibrary.getAllCards()) {
            if (c.hasTag(AbstractCard.CardTags.STARTER_STRIKE)) {
                blessMap.put(c.cardID, (new BlessStrike()).cardID);
            }
        }
    }

    // 以下为协同效应管理
    public static SynergismGraph synGraph = new SynergismGraph();

    private static void initializeSynergismGraph() {
        for (AbstractPackage p : mainPackageList) {// 加入点和协同tag
            synGraph.vert.add(p.PackageColor.toString());
            for (Pair<PackageEnum, SynergismGraph.SynTag> t : p.syng) {
                // syng为当前卡包和第一关键字卡包的关系为第二关键字，比如后者是前者的学生，就用student
                synGraph.add(p.PackageColor.toString(), t.getKey().toString(), t.getValue());
            }
        }
    }

    // 以下为卡包相关
    public static ArrayList<AbstractPackage> packageList = new ArrayList<AbstractPackage>();
    public static ArrayList<AbstractPackage> mainPackageList = new ArrayList<AbstractPackage>();
    public static HashMap<String, AbstractPackage> packageID;
    public static HashMap<String, String> cardParentMap = new HashMap<>();
    public static HashMap<Class<? extends AbstractCard>, String> cardClassParentMap = new HashMap<>();
    public static HashMap<String, AbstractPackage> packageColorMap = new HashMap<>();

    public static ArrayList<PackageEnum> validColors = new ArrayList<>();
    public static ArrayList<AbstractPackage> validPackage = new ArrayList<>();;
    public static HashMap<PackageEnum, AbstractPackage> colorToPackage = new HashMap<>();
    public static ArrayList<AbstractPackage> colorChoices = new ArrayList<>();
    public static ArrayList<String> allowedColors = new ArrayList<String>();
    public static boolean openedStarterScreen = true;
    private static boolean packageLoaded = false;
    public static RewardManager combatReward = new RewardManager();

    private static void initializePackage() {
        mainPackageList.clear();
        packageList.clear();
        packageColorMap.clear();

        mainPackageList.add(new RedPackage());
        mainPackageList.add(new GreenPackage());
        mainPackageList.add(new BluePackage());
        mainPackageList.add(new PurplePackage());
        mainPackageList.add(new HaoPackage());
        mainPackageList.add(new LostPackage());
        mainPackageList.add(new ShockPackage());
        mainPackageList.add(new C6H14Package());
        for (AbstractPackage p : mainPackageList) {// 添加子卡包
            packageList.add(p);
            allowedColors.add(p.PackageColor.toString());
            packageColorMap.put(p.PackageColor.toString(), p);
            packageList.add(p.SubPackages.get(PackageType.VALUE).makeCopy());
            packageList.add(p.SubPackages.get(PackageType.CONSISTENCY).makeCopy());
            packageList.add(p.SubPackages.get(PackageType.CEILING).makeCopy());
        }
        mainPackageList.add(new NullPackage());
        packageList.add(new NullPackage());
        packageLoaded = true;
    }

    public static AbstractPackage getPackageByColor(String s) {
        return packageColorMap.get(s);
    }

    /** 保证卡包已初始化（人物选择界面早于开局流程，新进程此时 mainPackageList 还是空的）。 */
    public static void ensurePackages() {
        if (!packageLoaded) {
            initializePackage();
        }
    }

    public static class CardTags {
        @SpireEnum
        public static AbstractCard.CardTags Separateble;
    }

    public static void addClassChoice() {
        Collections.shuffle(colorChoices, new Random(AbstractDungeon.cardRng.randomLong()));
        CardGroup charChoices = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        colorToPackage.clear();
        // 左中右 = _v/_e/_c（value/consistency/ceiling），后缀见 ui.json "Double:OptionCardSuffix"
        String[] suffixes = null;
        try {
            suffixes = CardCrawlGame.languagePack.getUIString("Double:OptionCardSuffix").TEXT;
        } catch (Exception e) {
            suffixes = null;
        }
        // 每轮固定展示 3 个选择（3 个随机颜色的 数值/运转/上限）；轮数由 needPackage 决定
        for (int i = 0; i < 3; ++i) {
            // 每轮用副本改标题，不污染共享的 OptionCard 实例（否则下轮会变成"牌:数值:运转"）
            AbstractDoubleCard shown = ((AbstractDoubleCard) colorChoices.get(i).OptionCard).makeCopy();
            if (suffixes != null && suffixes.length > i) {
                shown.name = shown.name + ":" + suffixes[i];
            }
            charChoices.addToTop(shown);
            PackageEnum packageenum = ((AbstractDoubleCard) colorChoices.get(i).OptionCard).packagetype;
            if (i == 0) {
                colorToPackage.put(packageenum, colorChoices.get(i).SubPackages.get(PackageType.VALUE));
            } else if (i == 1) {
                colorToPackage.put(packageenum, colorChoices.get(i).SubPackages.get(PackageType.CONSISTENCY));
            } else {
                colorToPackage.put(packageenum, colorChoices.get(i).SubPackages.get(PackageType.CEILING));
            }
        }
        AbstractDungeon.gridSelectScreen.open(charChoices, 1, false,
                (CardCrawlGame.languagePack.getUIString("Double:AtGameStart")).TEXT[1]);
    }

    public static AbstractCard getSpecificClassCard(AbstractCard.CardColor color) {
        ArrayList<AbstractCard> possList = new ArrayList<>(CardLibrary.getAllCards());
        possList.removeIf(c -> (c.hasTag(AbstractCard.CardTags.STARTER_STRIKE)
                || c.hasTag(AbstractCard.CardTags.STARTER_DEFEND) || c.color != color
                || c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS
                || c.rarity == AbstractCard.CardRarity.SPECIAL));

        if ((AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.COMBAT) {
            possList.removeIf(c -> c.hasTag(AbstractCard.CardTags.HEALING));
        }

        if (possList.size() == 0) {
            possList.add(new Madness());
        }

        return ((AbstractCard) possList.get(AbstractDungeon.cardRandomRng.random(possList.size() - 1)))
                .makeCopy();
    }

    public void receiveStartGame() {
        initializeBlessMap();
        // 战斗统计：读取开关。BaseMod 的 StartGame 事件有两个触发点（ActChangeHooks）：
        // 4 参 dungeon 构造器（仅真新开局：levelId=="Exordium" && floorNum==0）与
        // 3 参存档构造器（每次 SL 读档无条件触发；此刻 loadingSave 仍为 true，
        // CardCrawlGame:654 在 getDungeon 返回后才置 false）。
        // 幕切换走 4 参构造器但 levelId 非 Exordium → 不发事件，不会清数据。
        try {
            Properties statsDefaults = new Properties();
            statsDefaults.setProperty("StatsExport", "true");
            SpireConfig statsConfig = new SpireConfig("Double", "Common", statsDefaults);
            CardStats.enabled = "true".equals(statsConfig.getString("StatsExport"));
        } catch (IOException e) {
            CardStats.enabled = true;
        }
        if (com.megacrit.cardcrawl.core.CardCrawlGame.loadingSave) {
            // SL 读档：保留已固化的局内数据——回滚未固化的当前战斗临时数据，
            // 从 analysis/_cardstats_state.json（每场战斗胜利时的快照）恢复。
            // 否则每次 SL 都会清掉之前所有幕的统计（"只剩最后一幕"bug 的根因）。
            CardStats.onLoadSave();
        } else {
            CardStats.reset();
        }
        if (!CardCrawlGame.loadingSave) {
            orbitMisc = 0;
            openedStarterScreen = false;
            validColors = new ArrayList<>();
            validPackage = new ArrayList<>();
            try {
                SpireConfig config = new SpireConfig("Double", "Common");
                config.setString("Initialized", "false");
                for (int i = 0; i <= 100; ++i) {
                    config.setString("validColor" + i, "SS_Yellow");
                    config.setString("validPackage" + i, "Double:NullPackage");
                }
                config.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Properties defaults = new Properties();
                defaults.setProperty("Initialized", "false");
                config = new SpireConfig("Double", "Common", defaults);
                if (config.getString("Initialized").equals("false")) {
                    openedStarterScreen = false;
                    validColors = new ArrayList<>();
                    validPackage = new ArrayList<>();
                } else {
                    LoadData();
                }
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }
    }

    @Override
    public void receiveOnBattleStart(AbstractRoom arg0) {// 计算一些局内数据
        CardStats.onCombatStart(arg0); // 战斗统计：记录房间类型/敌人数/起始HP/回合基线
        combatReward.update();
        if (arg0 instanceof com.megacrit.cardcrawl.rooms.MonsterRoom &&
                !(arg0 instanceof com.megacrit.cardcrawl.rooms.MonsterRoomElite) &&
                !(arg0 instanceof com.megacrit.cardcrawl.rooms.MonsterRoomBoss)) {
            combatReward.commomCardReward = new RewardItem();
        }
        Hao_chance = 0;
        combatExhausts = 0;
        orbitMiscAtCombatStart = orbitMisc; // 快照：本场战斗开始（= 进房时）的 orbitMisc
    }

    @Override
    public void receivePostExhaust(AbstractCard arg0) {
        ++combatExhausts;
    }

    public void receiveCardUsed(final AbstractCard card) {
        if (card instanceof AbstractHaoCard) {
            ++this.Hao_chance;
            this.Hao_chance = Math.min(this.Hao_chance, 5);
        }
    }

    private static void LoadData() {
        ArrayList<AbstractCard> allowedCards = new ArrayList<>();
        for (AbstractPackage p : validPackage) {
            if (p.ID.equals("Double:NullPackage")) {
                continue;
            }
            allowedCards.addAll(p.CardLists);
        }
        for (AbstractCard card : CardLibrary.getAllCards()) {
            if (card.color == AbstractDungeon.player.getCardColor()) {
                allowedCards.add(card.makeStatEquivalentCopy());
            }
        }
        AbstractDungeon.commonCardPool.group.removeIf(ii -> (!allowedCards.contains(ii)));
        AbstractDungeon.srcCommonCardPool.group.removeIf(ii -> (!allowedCards.contains(ii)));
        CardCrawlGame.dungeon.initializeCardPools();
    }

    public static int choosingCharacters = -1;

    public static AbstractCard findCardFromColor(AbstractCard.CardColor c) {
        return CardLibrary.getCard("Double:" + c.toString() + "_option");
    }

    public static void TriggerAtGameStart() {
        if (AbstractDungeon.player instanceof AbstractSSCharacter) {
            validColors.clear();
            validPackage.clear();
            choosingCharacters = 0;
            colorChoices.clear();
            for (AbstractCard.CardColor r : AbstractCard.CardColor.values()) {
                if (r != AbstractCard.CardColor.CURSE && r != AbstractDungeon.player.getCardColor()
                        && r != AbstractCard.CardColor.COLORLESS && allowedColors.contains(r.name())) {
                    AbstractCard q = findCardFromColor(r);
                    if (q == null) {
                        System.out.println(r.toString());
                        System.out.println("null card!");
                    }
                    if (!q.isSeen) {
                        UnlockTracker.markCardAsSeen(q.cardID);
                    }
                }
            }
            // 人物选择界面预置的卡包：直接写入结果，其主卡包不再出现在游戏内选择中
            HashSet<PackageEnum> preApplied = new HashSet<>();
            for (int i = 0; i < PACK_SLOTS; i++) {
                AbstractPackage sub = CharacterSelectPackPatch.resolveSelection(i);
                if (sub == null || preApplied.contains(sub.PackageColor)) {
                    continue;
                }
                preApplied.add(sub.PackageColor);
                validColors.add(sub.PackageColor);
                validPackage.add(sub);
            }
            // 需要游戏内选择的槽位数 = 总槽位 - 已预置数（UI 防重复已保证同一主卡包只占一个槽位）
            needPackage = PACK_SLOTS - preApplied.size();
            for (AbstractPackage p : mainPackageList) {
                if (p.ID.equals("Double:NullPackage") || preApplied.contains(p.PackageColor)) {
                    continue;
                }
                colorChoices.add(p);
            }
            if (needPackage <= 0) {// 三个槽位全部预置完成，跳过游戏内选择直接收尾
                choosingCharacters = needPackage;
                finalizePackageSelection();
            } else {
                CenterGridCardSelectScreen.centerGridSelect = true;
                addClassChoice();
            }
        } else {
            System.out.println("unknown error");
        }
    }

    // 收尾卡包选择结果：加载卡池、发放起始圣物 + 起始卡、存盘。
    // 「游戏内选满」与「人物选择界面预置满三个槽位」两条路径共用。
    private static void finalizePackageSelection() {
        choosingCharacters = needPackage;
        CenterGridCardSelectScreen.centerGridSelect = false;
        if (!validColors.contains(PackageEnum.Default)) {
            validColors.add(PackageEnum.Default);
        }
        LoadData();
        for (AbstractPackage p : validPackage) {
            if (p.ID.equals("Double:NullPackage")) {
                continue;
            }
            if (AbstractDungeon.player.getRelic(p.StartRelic.relicId) == null) {
                AbstractRelic s = RelicLibrary.getRelic(p.StartRelic.relicId);
                RelicLibrary.getRelic(s.relicId).makeCopy().instantObtain();
            }
            if (!p.getStarterCard().equals("Madness")) {
                AbstractCard ccc = CardLibrary.getCard(p.getStarterCard());
                if (ccc != null) {
                    AbstractDungeon.player.masterDeck.addToTop(ccc.makeCopy());
                }
            }
        }
        try {
            SpireConfig config = new SpireConfig("Double", "Common");
            int i = 0;
            for (PackageEnum col : validColors) {
                ++i;
                config.setString("validColor" + i, col.toString());
            }
            i = 0;
            for (AbstractPackage p : validPackage) {
                if (p.ID.equals("Double:NullPackage")) {
                    continue;
                }
                ++i;
                config.setString("validPackage" + i, p.ID);
            }
            config.setString("Initialized", "true");
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
    }

    public void receivePostInitialize() {
        initializePackage();
        initializeSynergismGraph();
        sinBar = new Sinsbar();
        BaseMod.registerCustomReward(
                RewardEnum.HaoCardReward,
                (rewardSave) -> { // this handles what to do when this quest type is loaded.
                    return new HaoReward(rewardSave.id);
                },
                (customReward) -> { // this handles what to do when this quest type is saved.
                    if (((HaoReward) customReward).cards.isEmpty()) {
                        return new RewardSave(((HaoReward) customReward).type.toString(), null);
                    }
                    return new RewardSave(((HaoReward) customReward).type.toString(),
                            ((HaoReward) customReward).cards.get(0).cardID);
                });
    }

    public AbstractPackage getPackageByID(String s) {
        for (AbstractPackage p : packageList) {
            if (s.equals(p.ID)) {
                return p;
            }
        }
        return new RedPackage_v();
    }

    public void receivePostUpdate() {
        if (!packageLoaded) {
            initializePackage();
        }
        CardStats.update(); // 战斗统计：回合计数/卡牌栈排水/终局导出检测
        if (sinBar != null) {
            sinBar.update();
        }
        if (!openedStarterScreen && CardCrawlGame.isInARun()) {
            TriggerAtGameStart();
            openedStarterScreen = true;
            System.out.println("Chosen Characters:" + choosingCharacters);
            System.out.println("Valid Color Chosen:" + validColors.size());
        }
        if (choosingCharacters > -1 && choosingCharacters < needPackage
                && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {// 选了卡包
            AbstractDoubleCard c = (AbstractDoubleCard) AbstractDungeon.gridSelectScreen.selectedCards.get(0);
            Iterator<AbstractPackage> iterator = colorChoices.iterator();
            while (iterator.hasNext()) {
                AbstractPackage item = iterator.next();
                if (item.PackageColor == c.packagetype) {
                    iterator.remove();
                }
            }

            validColors.add(c.packagetype);
            validPackage.add(colorToPackage.get(c.packagetype));
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            if (choosingCharacters == needPackage - 1) {// 选完了
                finalizePackageSelection();
            } else if (choosingCharacters < needPackage - 1) {
                choosingCharacters++;
                addClassChoice();
            }
        }
        if (validColors.isEmpty() && this.choosingCharacters == -1) {
            // 尝试读取保存数据
            try {
                validColors.clear();
                Properties defaults = new Properties();
                defaults.setProperty("Initialized", "false");
                for (int i = 1; i <= PACK_SLOTS + 1; ++i) {
                    defaults.setProperty("validColor" + i, "Default");
                }
                for (int i = 1; i <= PACK_SLOTS; ++i) {
                    defaults.setProperty("validPackage" + i, "Double:NullPackage");
                }
                config = new SpireConfig("Double", "Common", defaults);
                for (int i = 1; i <= PACK_SLOTS + 1; ++i) {
                    if (config.getString("validColor" + i) == null)
                        continue;
                    String sss = config.getString("validColor" + i);
                    boolean inEnum = false;
                    for (PackageEnum pe : PackageEnum.values()) {
                        if (pe.toString().equals(sss)) {
                            inEnum = true;
                            break;
                        }
                    }
                    if (!inEnum)
                        continue;
                    if (PackageEnum.valueOf(config.getString("validColor" + i)) != null)
                        validColors.add(PackageEnum.valueOf(config.getString("validColor" + i)));
                }
                for (int i = 1; i <= PACK_SLOTS; ++i) {
                    String s = config.getString("validPackage" + i);
                    validPackage.add(getPackageByID(s));
                }

            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }
    }

    @Override
    public void receiveRender(SpriteBatch arg0) {
        if (sinBar != null && AbstractDungeon.player != null
                && AbstractDungeon.player instanceof AbstractSSCharacter) {
            sinBar.render(arg0);
        }
    }

    @Override
    public void receivePowersModified() {
        if (AbstractDungeon.player != null) {
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                if (c instanceof AbstractDoubleCard) {
                    ((AbstractDoubleCard) c).refreshPower();
                }
            }
            for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
                if (c instanceof AbstractDoubleCard) {
                    ((AbstractDoubleCard) c).refreshPower();
                }
            }
            for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
                if (c instanceof AbstractDoubleCard) {
                    ((AbstractDoubleCard) c).refreshPower();
                }
            }
            for (AbstractCard c : AbstractDungeon.player.exhaustPile.group) {
                if (c instanceof AbstractDoubleCard) {
                    ((AbstractDoubleCard) c).refreshPower();
                }
            }
        }
    }
}
