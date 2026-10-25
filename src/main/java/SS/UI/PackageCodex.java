package SS.UI;

import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.packages.AbstractPackage.PackageType;
import SS.path.PackageEnumList.PackageEnum;

/** Vanilla-size cards, four labeled columns, and independent scrolling pack navigation. */
public final class PackageCodex {
    private static final PackageType[] TYPES = {PackageType.VALUE, PackageType.CONSISTENCY, PackageType.CEILING};
    private static final float CARD_SCALE = .75F, TOP = 825, BOTTOM = 125, ROW_STEP = 355;
    private static final float LIST_TOP = 935, LIST_BOTTOM = 140, LIST_STEP = 76;
    private static final Color MUTED = new Color(.68F, .69F, .66F, 1);
    private static final List<AbstractPackage> packages = new ArrayList<>();
    private static final List<Hitbox> packButtons = new ArrayList<>();
    private static final List<CardSlot> cards = new ArrayList<>();
    private static final List<List<String>> columns = new ArrayList<>();
    private static final Hitbox entry = new Hitbox(1,1), close = new Hitbox(1,1);
    private static final Hitbox tabCards = new Hitbox(1,1), tabPairs = new Hitbox(1,1), upgrade = new Hitbox(1,1);
    private static ScrollBar listBar, cardBar;
    private static boolean open, pairs, upgraded, popupWasOpen, libraryEntry;
    private static int selected;
    private static float listScroll, listTarget, listMax, cardScroll, cardTarget, cardMax;
    private static float scale, offsetX, offsetY;
    private static String[] text;
    private static CardSlot hovered;

    private PackageCodex() { }
    public static boolean isOpen() { return open; }
    private static String t(int i) {
        if (text == null) text = CardCrawlGame.languagePack.getUIString("Double:PackageCodex").TEXT;
        return text[i];
    }
    private static float x(float v) { return offsetX + v * scale; }
    private static float y(float v) { return offsetY + v * scale; }
    private static float mx() { return (InputHelper.mX - offsetX) / scale; }
    private static float my() { return (InputHelper.mY - offsetY) / scale; }
    private static float clamp(float v, float max) { return Math.max(0, Math.min(v, max)); }
    private static void place(Hitbox hb, float cx, float cy, float w, float h) {
        hb.resize(w * scale, h * scale); hb.move(x(cx), y(cy));
    }
    private static void layout() {
        scale = Math.min(Settings.WIDTH / 1920F, Settings.HEIGHT / 1080F);
        offsetX = (Settings.WIDTH - 1920 * scale) / 2; offsetY = (Settings.HEIGHT - 1080 * scale) / 2;
        place(entry,1680,libraryEntry ? 65 : 290,280,52); place(close,1740,1010,170,52);
        place(tabCards,570,935,250,50); place(tabPairs,850,935,230,50); place(upgrade,1655,935,285,50);
        for (int i=0;i<packButtons.size();i++) place(packButtons.get(i),202,888-i*LIST_STEP+listScroll,265,62);
        if (listBar != null) {
            listBar.setCenter(x(365),y((LIST_TOP+LIST_BOTTOM)/2)); listBar.changeHeight((LIST_TOP-LIST_BOTTOM)*scale);
            cardBar.setCenter(x(1860),y((TOP+BOTTOM)/2)); cardBar.changeHeight((TOP-BOTTOM)*scale);
        }
        for (CardSlot slot:cards) position(slot);
    }
    private static void position(CardSlot slot) {
        place(slot.hb,585+slot.col*345,645-slot.row*ROW_STEP+cardScroll,300*CARD_SCALE,420*CARD_SCALE);
        if (slot.card == null) return;
        AbstractCard c=slot.card;
        c.current_x=c.target_x=slot.hb.cX; c.current_y=c.target_y=slot.hb.cY;
        c.drawScale=c.targetDrawScale=CARD_SCALE*scale/Settings.scale; c.angle=c.targetAngle=0;
        c.hb.resize(slot.hb.width,slot.hb.height); c.hb.move(slot.hb.cX,slot.hb.cY);
    }
    public static void renderEntry(SpriteBatch sb) { libraryEntry=false; renderEntryButton(sb); }
    public static void renderLibraryEntry(SpriteBatch sb) { libraryEntry=true; renderEntryButton(sb); }
    private static void renderEntryButton(SpriteBatch sb) { layout(); button(sb,entry,t(0),false); }
    public static boolean updateEntry() { libraryEntry=false; return updateEntryButton(); }
    public static boolean updateLibraryEntry() { libraryEntry=true; return updateEntryButton(); }
    private static boolean updateEntryButton() {
        layout();
        if (CardCrawlGame.isPopupOpen || !clicked(entry)) return false;
        modcore.ensurePackages(); packages.clear(); packButtons.clear();
        for (AbstractPackage p:modcore.mainPackageList) {
            boolean complete=true;
            for (PackageType type:TYPES) complete &= p.SubPackages.get(type)!=null;
            if (complete) { packages.add(p); packButtons.add(new Hitbox(1,1)); }
        }
        if (!packages.isEmpty()) {
            selected=Math.min(selected,packages.size()-1); pairs=false; open=true;
            listMax=Math.max(0,packages.size()*LIST_STEP-(LIST_TOP-LIST_BOTTOM)+20);
            listScroll=listTarget=clamp(selected*LIST_STEP-2*LIST_STEP,listMax);
            listBar=new ScrollBar(p -> {listScroll=listTarget=p*listMax;},x(365),y(537),795*scale);
            cardBar=new ScrollBar(p -> {cardScroll=cardTarget=p*cardMax;},x(1860),y(475),700*scale);
            rebuild(); layout();
        }
        consumeInput(); return true;
    }
    public static void close() {
        open=false; popupWasOpen=false; hovered=null; cards.clear(); columns.clear();
        InputHelper.pressedEscape=false; consumeInput();
    }
    private static String name(AbstractPackage p) {
        String value=p.OptionCard==null?p.ID:p.OptionCard.name;
        if (value.endsWith("卡包")) return value;
        if (value.endsWith("牌")) value=value.substring(0,value.length()-1);
        return value+t(18);
    }
    private static String partnerName(PackageEnum color) {
        for (AbstractPackage p:packages) if (p.PackageColor==color) return name(p);
        return color.toString();
    }
    private static void rebuild() {
        cards.clear(); columns.clear(); hovered=null; cardScroll=cardTarget=0;
        int rowCount=0; AbstractPackage pack=packages.get(selected);
        if (pairs) {
            for (AbstractPackage owner:packages) for (PackageEnum partner:PackageEnum.values()) {
                String id=owner.pairCards.get(partner);
                if (id!=null && (owner==pack || partner==pack.PackageColor)) {
                    int i=cards.size(); cards.add(new CardSlot(id,name(owner)+" > "+partnerName(partner),i%4,i/4));
                }
            }
            rowCount=(cards.size()+3)/4;
        } else {
            PackageCardGroups groups=new PackageCardGroups(pack.SubPackages.get(TYPES[0]).getCards(),
                    pack.SubPackages.get(TYPES[1]).getCards(),pack.SubPackages.get(TYPES[2]).getCards());
            columns.add(groups.common); columns.addAll(groups.exclusive);
            for (int col=0;col<columns.size();col++) {
                List<String> ids=columns.get(col); rowCount=Math.max(rowCount,ids.size());
                for (int row=0;row<ids.size();row++) cards.add(new CardSlot(ids.get(row),null,col,row));
            }
        }
        cardMax=Math.max(0,rowCount*ROW_STEP-5-(TOP-BOTTOM));
        if (cardBar!=null) cardBar.reset();
        layout();
    }
    private static void choose(int index) {
        selected=(index+packages.size())%packages.size();
        float cy=888-selected*LIST_STEP+listTarget;
        if (cy<LIST_BOTTOM+40) listTarget+=LIST_BOTTOM+40-cy;
        if (cy>LIST_TOP-40) listTarget-=cy-LIST_TOP+40;
        listTarget=clamp(listTarget,listMax); rebuild();
    }
    public static void update() {
        layout();
        if (CardCrawlGame.isPopupOpen) {popupWasOpen=true;return;}
        if (popupWasOpen) {popupWasOpen=false;InputHelper.pressedEscape=false;consumeInput();return;}
        if (InputHelper.pressedEscape || CInputActionSet.cancel.isJustPressed() || clicked(close)) {
            close(); CInputActionSet.cancel.unpress(); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) choose(selected-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) choose(selected+1);
        if (clicked(tabCards) && pairs) {pairs=false;rebuild();}
        if (clicked(tabPairs) && !pairs) {pairs=true;rebuild();}
        if (clicked(upgrade)) {
            float previous=cardScroll; upgraded=!upgraded; rebuild(); cardScroll=cardTarget=clamp(previous,cardMax);
        }
        float wheel=InputHelper.scrolledDown?90:InputHelper.scrolledUp?-90:0;
        if (mx()<390 && my()>LIST_BOTTOM && my()<LIST_TOP) listTarget=clamp(listTarget+wheel,listMax);
        else if (mx()>=410 && my()>BOTTOM && my()<TOP) cardTarget=clamp(cardTarget+wheel,cardMax);
        if (listMax>0) listBar.update();
        boolean dragging=cardMax>0 && cardBar.update();
        listScroll=MathHelper.scrollSnapLerpSpeed(listScroll,listTarget);
        cardScroll=MathHelper.scrollSnapLerpSpeed(cardScroll,cardTarget);
        listBar.parentScrolledToPercent(listMax==0?0:listScroll/listMax);
        cardBar.parentScrolledToPercent(cardMax==0?0:cardScroll/cardMax);
        layout();
        if (my()>LIST_BOTTOM && my()<LIST_TOP) for (int i=0;i<packButtons.size();i++) {
            if (clicked(packButtons.get(i))) {choose(i);consumeInput();return;}
        }
        hovered=null;
        boolean inside=mx()>410 && mx()<1835 && my()>BOTTOM && my()<TOP;
        for (CardSlot slot:cards) {
            if (inside && !dragging && slot.hb.cY+slot.hb.height/2>y(BOTTOM) && slot.hb.cY-slot.hb.height/2<y(TOP)) {
                if (clicked(slot.hb) && slot.card!=null) {
                    AbstractCard original=CardLibrary.getCard(slot.id).makeCopy();original.isSeen=true;original.isLocked=false;
                    CardCrawlGame.cardPopup.open(original);SingleCardViewPopup.isViewingUpgrade=upgraded;
                    popupWasOpen=true;consumeInput();return;
                }
                if (slot.card!=null) {slot.card.updateHoverLogic();if(slot.hb.hovered) hovered=slot;}
            } else if (slot.card!=null) {slot.card.unhover();slot.hb.clickStarted=false;}
        }
        consumeInput();
    }
    private static boolean clicked(Hitbox hb) {
        hb.update();if(hb.hovered && InputHelper.justClickedLeft) hb.clickStarted=true;
        if(!hb.clicked) return false;
        hb.clicked=false;CardCrawlGame.sound.playA("UI_CLICK_1",-.4F);return true;
    }
    private static void consumeInput() {
        InputHelper.justClickedLeft=false;InputHelper.justReleasedClickLeft=false;InputHelper.justClickedRight=false;
        InputHelper.scrolledDown=InputHelper.scrolledUp=false;CInputActionSet.select.unpress();CInputActionSet.proceed.unpress();
    }
    public static void render(SpriteBatch sb) {
        if(!open) return;layout();
        sb.setColor(.025F,.022F,.02F,.96F);sb.draw(ImageMaster.WHITE_SQUARE_IMG,0,0,Settings.WIDTH,Settings.HEIGHT);
        label(sb,t(0),200,1010,Settings.GOLD_COLOR);label(sb,name(packages.get(selected)),1110,1010,Settings.GOLD_COLOR);
        button(sb,close,t(2),false);line(sb,390,115,1,865);line(sb,425,835,1400,1);label(sb,t(17),200,963,MUTED);
        if(clip(sb,60,LIST_BOTTOM,285,LIST_TOP-LIST_BOTTOM)) {
            for(int i=0;i<packages.size();i++) {
                Hitbox hb=packButtons.get(i);boolean active=i==selected;
                fit(sb,(active?"> ":"")+name(packages.get(i)),hb.cX,hb.cY,255*scale,
                        active||hb.hovered?Settings.GOLD_COLOR:Settings.CREAM_COLOR);
                if(active) line(sb,82,(hb.cY-offsetY)/scale-28,240,1);
            }
            unclip(sb);
        }
        if(listMax>0) listBar.render(sb);
        button(sb,tabCards,t(3),!pairs);button(sb,tabPairs,t(4),pairs);button(sb,upgrade,upgraded?t(12):t(11),upgraded);
        AbstractPackage pack=packages.get(selected);AbstractCard starter=CardLibrary.getCard(pack.getStarterCard());
        String info=t(14)+(starter==null?pack.getStarterCard():starter.name)+"    "+t(15)
                +(pack.StartRelic==null?"-":pack.StartRelic.name)+"    "+t(16)+(pack.BossRelic==null?"-":pack.BossRelic.name);
        fit(sb,info,x(1110),y(885),1350*scale,MUTED);
        if(!pairs) for(int col=0;col<4;col++) label(sb,t(5+col)+" ("+columns.get(col).size()+")",585+col*345,855,Settings.GOLD_COLOR);
        else fit(sb,t(9),x(1110),y(855),1300*scale,MUTED);
        if(clip(sb,410,BOTTOM,1420,TOP-BOTTOM)) {
            for(CardSlot slot:cards) {
                if(slot.hb.cY+slot.hb.height/2<y(BOTTOM)||slot.hb.cY-slot.hb.height/2>y(TOP)) continue;
                position(slot);
                if(slot.label!=null) fit(sb,slot.label,slot.hb.cX,slot.hb.cY+170*scale,320*scale,Settings.GOLD_COLOR);
                if(slot.card!=null) slot.card.render(sb);else fit(sb,slot.id,slot.hb.cX,slot.hb.cY,270*scale,MUTED);
            }
            unclip(sb);
        }
        // Render the hovered card last at the same enlarged size as the vanilla library.
        if(hovered!=null && !CardCrawlGame.isPopupOpen) {
            AbstractCard c=hovered.card;c.current_y=Math.max(y(BOTTOM+215),Math.min(y(TOP-215),c.current_y));
            c.drawScale=scale/Settings.scale;c.renderHoverShadow(sb);c.render(sb);c.renderCardTip(sb);position(hovered);
        }
        if(cards.isEmpty()) label(sb,t(10),1100,500,MUTED);
        if(cardMax>0) cardBar.render(sb);
        fit(sb,t(13),x(1110),y(75),1360*scale,MUTED);sb.setColor(Color.WHITE);
    }
    private static boolean clip(SpriteBatch sb,float left,float bottom,float width,float height) {
        sb.flush();return ScissorStack.pushScissors(new Rectangle(x(left),y(bottom),width*scale,height*scale));
    }
    private static void unclip(SpriteBatch sb) {sb.flush();ScissorStack.popScissors();}
    private static void line(SpriteBatch sb,float left,float bottom,float width,float height) {
        sb.setColor(.75F,.64F,.40F,.5F);sb.draw(ImageMaster.WHITE_SQUARE_IMG,x(left),y(bottom),width*scale,height*scale);
    }
    private static void label(SpriteBatch sb,String value,float cx,float cy,Color color) {
        FontHelper.renderFontCentered(sb,FontHelper.tipHeaderFont,value,x(cx),y(cy),color);
    }
    private static void button(SpriteBatch sb,Hitbox hb,String value,boolean active) {
        fit(sb,value,hb.cX,hb.cY,hb.width,active||hb.hovered?Settings.GOLD_COLOR:Settings.CREAM_COLOR);
        if(active||hb.hovered) line(sb,(hb.x-offsetX)/scale+10,(hb.y-offsetY)/scale+2,hb.width/scale-20,2);
    }
    private static void fit(SpriteBatch sb,String value,float cx,float cy,float width,Color color) {
        BitmapFont font=FontHelper.tipHeaderFont;float sx=font.getData().scaleX,sy=font.getData().scaleY;
        float actual=FontHelper.getWidth(font,value,1),factor=actual>width?width/actual:1;
        font.getData().setScale(sx*factor,sy*factor);FontHelper.renderFontCentered(sb,font,value,cx,cy,color);font.getData().setScale(sx,sy);
    }
    private static final class CardSlot {
        final String id,label;final int col,row;final Hitbox hb=new Hitbox(1,1);final AbstractCard card;
        CardSlot(String id,String label,int col,int row) {
            this.id=id;this.label=label;this.col=col;this.row=row;AbstractCard original=CardLibrary.getCard(id);
            card=original==null?null:original.makeCopy();
            if(card!=null) {card.isSeen=true;card.isLocked=false;if(upgraded && card.canUpgrade()){card.upgrade();card.displayUpgrades();}}
        }
    }
}
