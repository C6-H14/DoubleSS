package SS.modcore;

import basemod.AutoAdd;
import basemod.BaseMod;
import basemod.abstracts.CustomRelic;
import basemod.interfaces.EditCardsSubscriber;
import basemod.interfaces.EditCharactersSubscriber;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.ISubscriber;
import basemod.interfaces.OnCardUseSubscriber;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostExhaustSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import basemod.interfaces.RenderSubscriber;
import basemod.interfaces.StartGameSubscriber;

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
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.Keyword;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rewards.RewardSave;

import SS.UI.Sinsbar;
import SS.cards.AbstractDoubleCard;
import SS.cards.BlessCard.BlessStrike;
import SS.cards.Haohao.AbstractHaoCard;
import SS.characters.MyCharacter;
import SS.helper.PermanentBlockVariable;
import SS.helper.PermanentDamageVariable;
import SS.helper.PermanentMagicNumberVariable;
import SS.packages.AbstractPackage;
import SS.packages.NullPackage;
import SS.packages.AbstractPackage.PackageType;
import SS.packages.BluePackage.BluePackage;
import SS.packages.GreenPackage.GreenPackage;
import SS.packages.HaoPackage.HaoPackage;
import SS.packages.LostPackage.LostPackage;
import SS.packages.PurplePackage.PurplePackage;
import SS.packages.RedPackage.RedPackage;
import SS.packages.RedPackage.RedPackage_v;
import SS.patches.CenterGridCardSelectScreen;
import SS.path.AbstractCardEnum;
import SS.path.RewardEnum;
import SS.path.ThmodClassEnum;
import SS.relic.Merit;
import SS.relic.SS.LCysteine;
import SS.rewards.HaoReward;
import SS.rewards.RewardManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

@SpireInitializer
public class modcore implements EditCardsSubscriber, EditRelicsSubscriber, EditCharactersSubscriber,
        EditStringsSubscriber, EditKeywordsSubscriber, StartGameSubscriber, PostUpdateSubscriber,
        PostInitializeSubscriber, OnStartBattleSubscriber, OnCardUseSubscriber, RenderSubscriber,
        PostExhaustSubscriber {
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
        BaseMod.addColor(AbstractCardEnum.SS_Yellow, COL, COL, COL, COL, COL, COL, COL, BG_ATTACK_512, BG_SKILL_512,
                BG_POWER_512, ENEYGY_ORB, BG_ATTACK_1024, BG_SKILL_1024, BG_POWER_1024, BIG_ORB, SMALL_ORB);
        addCardColor(AbstractCardEnum.Hao_Green, "hao");
        addCardColor(AbstractCardEnum.Lost_Black, "lost");
    }

    private static int needPackage = 2;
    public static SpireConfig config = null;

    public static void initialize() {
        new modcore();
    }

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
        // BaseMod.addRelicToCustomPool(new BoilingBlood(), AbstractCardEnum.SS_Yellow);
        // BaseMod.addRelicToCustomPool(new HalfRingOfTheSnake(),
        // AbstractCardEnum.SS_Yellow);
        // BaseMod.addRelicToCustomPool(new CorePieces(), AbstractCardEnum.SS_Yellow);
        // BaseMod.addRelicToCustomPool(new BathWater(), AbstractCardEnum.SS_Yellow);
        // BaseMod.addRelicToCustomPool(new LCysteine(), AbstractCardEnum.SS_Yellow);
        // BaseMod.addRelicToCustomPool(new GreenApple(), AbstractCardEnum.SS_Yellow);
        // BaseMod.addRelicToCustomPool(new WoodenCross(), AbstractCardEnum.SS_Yellow);
        BaseMod.addRelic(new Merit(), basemod.helpers.RelicType.SHARED);
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

    // 以下为卡包相关
    public static ArrayList<AbstractPackage> packageList = new ArrayList<AbstractPackage>();
    public static ArrayList<AbstractPackage> mainPackageList = new ArrayList<AbstractPackage>();
    public static HashMap<String, AbstractPackage> packageID;
    public static HashMap<String, String> cardParentMap = new HashMap<>();
    public static HashMap<Class<? extends AbstractCard>, String> cardClassParentMap = new HashMap<>();
    public static HashMap<String, AbstractPackage> packageColorMap = new HashMap<>();

    public static ArrayList<AbstractCard.CardColor> validColors = new ArrayList<>();
    public static ArrayList<AbstractPackage> validPackage = new ArrayList<>();;
    public static HashMap<AbstractCard.CardColor, AbstractPackage> colorToPackage = new HashMap<>();
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

    public static class CardTags {
        @SpireEnum
        public static AbstractCard.CardTags Separateble;
    }

    public static void addClassChoice() {
        Collections.shuffle(colorChoices, new Random(AbstractDungeon.cardRng.randomLong()));
        CardGroup charChoices = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        colorToPackage.clear();
        for (int i = 0; i < 3; ++i) {
            charChoices.addToTop(colorChoices.get(i).OptionCard);
            if (i == 0) {
                colorToPackage.put(colorChoices.get(i).OptionCard.color,
                        colorChoices.get(i).SubPackages.get(PackageType.VALUE));
            } else if (i == 1) {
                colorToPackage.put(colorChoices.get(i).OptionCard.color,
                        colorChoices.get(i).SubPackages.get(PackageType.CONSISTENCY));
            } else {
                colorToPackage.put(colorChoices.get(i).OptionCard.color,
                        colorChoices.get(i).SubPackages.get(PackageType.CEILING));
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
        if (!CardCrawlGame.loadingSave) {
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
        combatReward.update();
        if (arg0 instanceof com.megacrit.cardcrawl.rooms.MonsterRoom &&
                !(arg0 instanceof com.megacrit.cardcrawl.rooms.MonsterRoomElite) &&
                !(arg0 instanceof com.megacrit.cardcrawl.rooms.MonsterRoomBoss)) {
            combatReward.commomCardReward = new RewardItem();
        }
        Hao_chance = 0;
        combatExhausts = 0;
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
            if (AbstractDungeon.player.getRelic(p.StartRelic.relicId) == null) {
                AbstractRelic s = RelicLibrary.getRelic(p.StartRelic.relicId);
                RelicLibrary.getRelic(s.relicId).makeCopy().instantObtain();
            }
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
        if (AbstractDungeon.player instanceof MyCharacter) {
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
            for (AbstractPackage p : mainPackageList) {
                if (p.ID.equals("Double:NullPackage")) {
                    continue;
                }
                colorChoices.add(p);
            }
            CenterGridCardSelectScreen.centerGridSelect = true;
            addClassChoice();
        } else {
            System.out.println("unknown error");
        }
    }

    public void receivePostInitialize() {
        initializePackage();
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
            AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
            Iterator<AbstractPackage> iterator = colorChoices.iterator();
            while (iterator.hasNext()) {
                AbstractPackage item = iterator.next();
                if (item.PackageColor == c.color) {
                    iterator.remove();
                }
            }

            validColors.add(c.color);
            validPackage.add(colorToPackage.get(c.color));
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            if (choosingCharacters == needPackage - 1) {// 选完了
                choosingCharacters = needPackage;
                CenterGridCardSelectScreen.centerGridSelect = false;
                if (!validColors.contains(AbstractDungeon.player.getCardColor())) {
                    validColors.add(AbstractDungeon.player.getCardColor());
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
                }
                try {
                    SpireConfig config = new SpireConfig("Double", "Common");
                    int i = 0;
                    for (AbstractCard.CardColor col : validColors) {
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
                for (int i = 1; i <= needPackage + 1; ++i) {
                    defaults.setProperty("validColor" + i, "SS_Yellow");
                }
                for (int i = 1; i <= needPackage; ++i) {
                    defaults.setProperty("validPackage" + i, "Double:NullPackage");
                }
                config = new SpireConfig("Double", "Common", defaults);
                for (int i = 1; i <= needPackage + 1; ++i) {
                    validColors.add(AbstractCard.CardColor.valueOf(config.getString("validColor" + i)));
                }
                for (int i = 1; i <= needPackage; ++i) {
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
        if (sinBar != null) {
            sinBar.render(arg0);
        }
    }
}
