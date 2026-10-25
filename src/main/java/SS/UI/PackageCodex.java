package SS.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;

import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.packages.AbstractPackage.PackageType;
import SS.path.PackageEnumList.PackageEnum;

/** Read-only character-select catalog. All card identities come from live package definitions. */
public final class PackageCodex {
    private static final PackageType[] TYPES = {
            PackageType.VALUE, PackageType.CONSISTENCY, PackageType.CEILING };
    private static final Color PANEL = new Color(0.055F, 0.075F, 0.10F, 1F);
    private static final Color TILE = new Color(0.10F, 0.13F, 0.17F, 1F);
    private static final Color ACTIVE = new Color(0.19F, 0.25F, 0.29F, 1F);
    private static final Color MUTED = new Color(0.66F, 0.72F, 0.77F, 1F);
    private static final int ROWS = 5;
    private static final List<AbstractPackage> packages = new ArrayList<>();
    private static final List<Row> rows = new ArrayList<>();
    private static final List<List<String>> columns = new ArrayList<>();
    private static final ArrayList<PowerTip> relicTips = new ArrayList<>();
    private static final Hitbox entry = new Hitbox(1, 1);
    private static final Hitbox previous = new Hitbox(1, 1);
    private static final Hitbox next = new Hitbox(1, 1);
    private static final Hitbox close = new Hitbox(1, 1);
    private static final Hitbox tabCards = new Hitbox(1, 1);
    private static final Hitbox tabPairs = new Hitbox(1, 1);
    private static final Hitbox upgrade = new Hitbox(1, 1);
    private static final Hitbox pagePrevious = new Hitbox(1, 1);
    private static final Hitbox pageNext = new Hitbox(1, 1);
    private static boolean open;
    private static boolean pairs;
    private static boolean upgraded;
    private static boolean popupWasOpen;
    private static boolean libraryEntry;
    private static int selectedPackage;
    private static int page;
    private static int pageCount = 1;
    private static String[] text;
    private static AbstractCard preview;
    private static String previewId;
    private static float scale;
    private static float offsetX;
    private static float offsetY;

    private PackageCodex() { }

    public static boolean isOpen() { return open; }

    private static String t(int index) {
        if (text == null) {
            text = CardCrawlGame.languagePack.getUIString("Double:PackageCodex").TEXT;
        }
        return text[index];
    }

    private static void layout() {
        scale = Math.min(Settings.WIDTH / 1920F, Settings.HEIGHT / 1080F);
        offsetX = (Settings.WIDTH - 1920F * scale) / 2;
        offsetY = (Settings.HEIGHT - 1080F * scale) / 2;
        place(entry, 1680, libraryEntry ? 65 : 290, 280, 52);
        place(previous, 140, 924, 80, 54);
        place(next, 1230, 924, 80, 54);
        place(close, 1710, 962, 140, 50);
        place(tabCards, 210, 838, 240, 52);
        place(tabPairs, 460, 838, 240, 52);
        place(upgrade, 1595, 236, 270, 52);
        place(pagePrevious, 640, 210, 110, 46);
        place(pageNext, 940, 210, 110, 46);
    }

    private static float x(float x) { return offsetX + x * scale; }
    private static float y(float y) { return offsetY + y * scale; }

    private static void place(Hitbox hb, float x, float y, float width, float height) {
        hb.resize(width * scale, height * scale);
        hb.move(x(x), y(y));
    }

    public static void renderEntry(SpriteBatch sb) {
        libraryEntry = false;
        layout();
        button(sb, entry, t(0), false);
    }

    public static void renderLibraryEntry(SpriteBatch sb) {
        libraryEntry = true;
        layout();
        button(sb, entry, t(0), false);
    }

    public static boolean updateLibraryEntry() {
        libraryEntry = true;
        return updateEntryAtCurrentPosition();
    }

    /** Called before the underlying character screen consumes mouse input. */
    public static boolean updateEntry() {
        libraryEntry = false;
        return updateEntryAtCurrentPosition();
    }

    private static boolean updateEntryAtCurrentPosition() {
        layout();
        if (CardCrawlGame.isPopupOpen) { return false; }
        if (clicked(entry)) {
            modcore.ensurePackages();
            packages.clear();
            for (AbstractPackage pack : modcore.mainPackageList) {
                if (hasVariants(pack)) { packages.add(pack); }
            }
            if (!packages.isEmpty()) {
                selectedPackage = Math.min(selectedPackage, packages.size() - 1);
                pairs = false;
                page = 0;
                open = true;
                rebuild();
            }
            consumeInput();
            return true;
        }
        return false;
    }

    private static boolean hasVariants(AbstractPackage pack) {
        for (PackageType type : TYPES) {
            if (pack.SubPackages.get(type) == null) { return false; }
        }
        return true;
    }

    public static void close() {
        open = false;
        popupWasOpen = false;
        rows.clear();
        columns.clear();
        preview = null;
        previewId = null;
        InputHelper.pressedEscape = false;
        consumeInput();
    }

    private static String packageName(AbstractPackage pack) {
        return pack.OptionCard == null ? pack.ID : pack.OptionCard.name;
    }

    private static String partnerName(PackageEnum color) {
        for (AbstractPackage pack : packages) {
            if (pack.PackageColor == color) { return packageName(pack); }
        }
        return color.toString();
    }

    private static void rebuild() {
        rows.clear();
        columns.clear();
        preview = null;
        previewId = null;
        AbstractPackage pack = packages.get(selectedPackage);
        if (pairs) {
            List<String> ids = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            // Both directed declarations are visible, with the declaring pack named.
            for (AbstractPackage owner : packages) {
                for (PackageEnum partner : PackageEnum.values()) {
                    String id = owner.pairCards.get(partner);
                    if (id != null && (owner == pack || partner == pack.PackageColor)) {
                        ids.add(id);
                        labels.add(packageName(owner) + " > " + partnerName(partner));
                    }
                }
            }
            pageCount = Math.max(1, (ids.size() + 11) / 12);
            page = Math.min(page, pageCount - 1);
            for (int i = page * 12; i < Math.min(ids.size(), (page + 1) * 12); ++i) {
                int local = i - page * 12;
                addRow(ids.get(i), labels.get(i), local / 4, local % 4, true);
            }
        } else {
            PackageCardGroups groups = new PackageCardGroups(
                    pack.SubPackages.get(TYPES[0]).getCards(),
                    pack.SubPackages.get(TYPES[1]).getCards(),
                    pack.SubPackages.get(TYPES[2]).getCards());
            columns.add(groups.common);
            columns.addAll(groups.exclusive);
            int max = 0;
            for (List<String> ids : columns) { max = Math.max(max, ids.size()); }
            pageCount = Math.max(1, (max + ROWS - 1) / ROWS);
            page = Math.min(page, pageCount - 1);
            for (int col = 0; col < columns.size(); ++col) {
                List<String> ids = columns.get(col);
                for (int i = page * ROWS; i < Math.min(ids.size(), (page + 1) * ROWS); ++i) {
                    addRow(ids.get(i), null, col, i - page * ROWS, false);
                }
            }
        }
        if (!rows.isEmpty()) { select(rows.get(0).id); }
    }

    private static void addRow(String id, String label, int col, int row, boolean pair) {
        Row item = new Row(id, label);
        float width = pair ? 390 : 285;
        float cx = pair ? 310 + col * 405 : 255 + col * 300;
        float cy = 669 - row * (pair ? 105 : 78);
        place(item.hb, cx, cy, width, pair ? 86 : 66);
        rows.add(item);
    }

    private static void select(String id) {
        if (id.equals(previewId)) { return; }
        previewId = id;
        AbstractCard card = CardLibrary.getCard(id);
        preview = card == null ? null : card.makeCopy();
        if (preview != null) {
            preview.isSeen = true;
            preview.isLocked = false;
            if (upgraded && preview.canUpgrade()) {
                preview.upgrade();
                preview.displayUpgrades();
            }
        }
    }

    public static void update() {
        layout();
        if (CardCrawlGame.isPopupOpen) { popupWasOpen = true; return; }
        if (popupWasOpen) {
            popupWasOpen = false;
            InputHelper.pressedEscape = false;
            consumeInput();
            return;
        }
        if (InputHelper.pressedEscape || CInputActionSet.cancel.isJustPressed() || clicked(close)) {
            close();
            CInputActionSet.cancel.unpress();
            return;
        }
        if (clicked(previous) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            selectedPackage = (selectedPackage + packages.size() - 1) % packages.size();
            page = 0; rebuild();
        } else if (clicked(next) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selectedPackage = (selectedPackage + 1) % packages.size();
            page = 0; rebuild();
        }
        if (clicked(tabCards) && pairs) { pairs = false; page = 0; rebuild(); }
        if (clicked(tabPairs) && !pairs) { pairs = true; page = 0; rebuild(); }
        if (pageCount > 1) {
            if (clicked(pagePrevious)) { page = (page + pageCount - 1) % pageCount; rebuild(); }
            if (clicked(pageNext)) { page = (page + 1) % pageCount; rebuild(); }
        }
        if (clicked(upgrade)) {
            upgraded = !upgraded;
            String id = previewId;
            rebuild();
            if (id != null) { select(id); }
        }
        for (Row row : rows) {
            boolean clicked = clicked(row.hb);
            if (row.hb.hovered) { select(row.id); }
            if (clicked) {
                AbstractCard original = CardLibrary.getCard(row.id);
                if (original != null) {
                    AbstractCard copy = original.makeCopy();
                    copy.isSeen = true;
                    copy.isLocked = false;
                    CardCrawlGame.cardPopup.open(copy);
                    SingleCardViewPopup.isViewingUpgrade = upgraded;
                    popupWasOpen = true;
                }
                consumeInput();
                break;
            }
        }
        consumeInput();
    }

    private static boolean clicked(Hitbox hb) {
        hb.update();
        if (hb.hovered && InputHelper.justClickedLeft) { hb.clickStarted = true; }
        if (hb.clicked) {
            hb.clicked = false;
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.4F);
            return true;
        }
        return false;
    }

    private static void consumeInput() {
        InputHelper.justClickedLeft = false;
        InputHelper.justReleasedClickLeft = false;
        InputHelper.justClickedRight = false;
        CInputActionSet.select.unpress();
        CInputActionSet.proceed.unpress();
    }

    public static void render(SpriteBatch sb) {
        if (!open) { return; }
        layout();
        sb.setColor(0, 0, 0, 0.86F);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0, 0, Settings.WIDTH, Settings.HEIGHT);
        rect(sb, 60, 125, 1800, 890, PANEL);
        rect(sb, 1415, 265, 390, 610, TILE);
        centered(sb, t(1), 960, 980, Settings.GOLD_COLOR);
        button(sb, close, t(2), false);
        button(sb, previous, "<", false);
        button(sb, next, ">", false);
        centered(sb, packageName(packages.get(selectedPackage)) + "  "
                + (selectedPackage + 1) + "/" + packages.size(), 690, 925, Settings.CREAM_COLOR);
        button(sb, tabCards, t(3), !pairs);
        button(sb, tabPairs, t(4), pairs);
        drawMetadata(sb);
        if (!pairs) {
            for (int col = 0; col < 4; ++col) {
                centered(sb, t(5 + col) + " (" + columns.get(col).size() + ")",
                        255 + col * 300, 737, Settings.GOLD_COLOR);
            }
        } else {
            left(sb, t(9), 115, 745, 1260, MUTED);
            if (rows.isEmpty()) { centered(sb, t(10), 730, 560, MUTED); }
        }
        for (Row row : rows) {
            rect(sb, row.hb, row.id.equals(previewId) ? ACTIVE : TILE);
            AbstractCard card = row.displayCard;
            String name = card == null ? row.id : card.name;
            float localX = (row.hb.x - offsetX) / scale + 14;
            float localY = (row.hb.cY - offsetY) / scale;
            if (row.label != null) {
                left(sb, row.label, localX, localY + 22, row.hb.width / scale - 28, MUTED);
                localY -= 15;
            }
            Color color = card == null ? Settings.RED_TEXT_COLOR : rarityColor(card);
            left(sb, name, localX, localY + 8, row.hb.width / scale - 58, color);
            if (card != null) {
                String cost = card.cost == -1 ? "X" : card.cost < 0 ? "-" : String.valueOf(card.cost);
                centered(sb, cost, (row.hb.x + row.hb.width - offsetX) / scale - 20, localY, MUTED);
            }
        }
        if (preview != null) {
            preview.current_x = preview.target_x = x(1610);
            preview.current_y = preview.target_y = y(560);
            preview.drawScale = preview.targetDrawScale = 0.94F * scale / Settings.scale;
            preview.angle = preview.targetAngle = 0;
            preview.render(sb);
            if (InputHelper.mX > x(1415) && InputHelper.mY > y(265)) {
                preview.renderCardTip(sb);
            }
        }
        button(sb, upgrade, upgraded ? t(12) : t(11), upgraded);
        if (pageCount > 1) {
            button(sb, pagePrevious, "<", false);
            button(sb, pageNext, ">", false);
            centered(sb, (page + 1) + "/" + pageCount, 790, 210, MUTED);
        }
        left(sb, t(13), 120, 160, 1580, MUTED);
        sb.setColor(Color.WHITE);
    }

    private static void drawMetadata(SpriteBatch sb) {
        AbstractPackage pack = packages.get(selectedPackage);
        AbstractCard starter = CardLibrary.getCard(pack.getStarterCard());
        String starterName = starter == null ? pack.getStarterCard() : starter.name;
        String relic = pack.StartRelic == null ? "-" : pack.StartRelic.name;
        String boss = pack.BossRelic == null ? "-" : pack.BossRelic.name;
        left(sb, t(14) + starterName + "    " + t(15) + relic + "    " + t(16) + boss,
                120, 785, 1250, Settings.CREAM_COLOR);
        float[] positions = {1480, 1585, 1690};
        AbstractRelic[] relics = {pack.StartRelic, pack.BossRelic};
        for (int i = 0; i < relics.length; ++i) {
            AbstractRelic item = relics[i];
            if (item == null || item.img == null) { continue; }
            float cx = x(positions[i + 1]), cy = y(818);
            sb.setColor(Color.WHITE);
            sb.draw(item.img, cx - 28 * scale, cy - 28 * scale, 56 * scale, 56 * scale);
            if (!CardCrawlGame.isPopupOpen && Math.abs(InputHelper.mX - cx) < 30 * scale
                    && Math.abs(InputHelper.mY - cy) < 30 * scale) {
                relicTips.clear();
                relicTips.add(new PowerTip(item.name, item.description));
                TipHelper.queuePowerTips(x(1400), y(760), relicTips);
            }
        }
    }

    private static Color rarityColor(AbstractCard card) {
        switch (card.rarity) {
            case RARE: return Settings.GOLD_COLOR;
            case UNCOMMON: return Settings.BLUE_TEXT_COLOR;
            default: return Settings.CREAM_COLOR;
        }
    }

    private static void button(SpriteBatch sb, Hitbox hb, String label, boolean active) {
        rect(sb, hb, active || hb.hovered ? ACTIVE : TILE);
        FontHelper.renderFontCentered(sb, FontHelper.tipHeaderFont, label, hb.cX, hb.cY,
                active || hb.hovered ? Settings.GOLD_COLOR : Settings.CREAM_COLOR);
    }

    private static void rect(SpriteBatch sb, Hitbox hb, Color color) {
        sb.setColor(color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, hb.x, hb.y, hb.width, hb.height);
    }

    private static void rect(SpriteBatch sb, float x, float y, float width, float height, Color color) {
        sb.setColor(color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x(x), y(y), width * scale, height * scale);
    }

    private static void centered(SpriteBatch sb, String value, float x, float y, Color color) {
        FontHelper.renderFontCentered(sb, FontHelper.tipHeaderFont, value, x(x), y(y), color);
    }

    private static void left(SpriteBatch sb, String value, float x, float y, float width, Color color) {
        BitmapFont font = FontHelper.tipBodyFont;
        float oldX = font.getData().scaleX, oldY = font.getData().scaleY;
        float actual = FontHelper.getWidth(font, value, 1);
        float factor = actual > width * scale ? width * scale / actual : 1;
        font.getData().setScale(oldX * factor, oldY * factor);
        FontHelper.renderFontLeftTopAligned(sb, font, value, x(x), y(y), color);
        font.getData().setScale(oldX, oldY);
    }

    private static final class Row {
        final String id;
        final String label;
        final AbstractCard displayCard;
        final Hitbox hb = new Hitbox(1, 1);
        Row(String id, String label) {
            this.id = id;
            this.label = label;
            AbstractCard original = CardLibrary.getCard(id);
            displayCard = original == null ? null : original.makeCopy();
            if (displayCard != null && upgraded && displayCard.canUpgrade()) { displayCard.upgrade(); }
        }
    }
}
