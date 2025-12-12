package SS.cards;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.unique.ss.YinYangAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class YinYang extends AbstractDoubleCard {
    public static final String ID = ModHelper.makePath("YinYang");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/YinYang.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final String[] EXTENDED_DESCRIPTION = CARD_STRINGS.EXTENDED_DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.SS_Yellow;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public YinYang() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION, true, false);
        this.tags.add(AbstractCardEnum.Fiend);
        if (needFiend()) {
            updateFiend();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            UpdateDescription();
            initializeDescription();
        }
    }

    private float rotationTimer;
    private int previewIndex;
    private ArrayList<AbstractCard> dupeListForPrev = new ArrayList<>();

    private ArrayList<AbstractCard> getList() {
        ArrayList<AbstractCard> myList = new ArrayList<>();
        for (AbstractCard q : CardLibrary.getAllCards()) {
            if (q.hasTag(AbstractCardEnum.Virtues)) {
                AbstractCard r = q.makeCopy();
                System.out.println(r + ":" + r.hasTag(AbstractCardEnum.Virtues));
                if (this.upgraded) {
                    r.upgrade();
                }
                myList.add(r);
            }
        }
        return myList;
    }

    public void update() {
        super.update();
        if (this.dupeListForPrev.isEmpty()) {
            this.dupeListForPrev.addAll(getList());
        }
        if (this.hb.hovered) {
            if (this.rotationTimer <= 0.0F) {
                this.rotationTimer = 3.0F;
                if (this.dupeListForPrev.size() == 0) {
                    this.cardsToPreview = (AbstractCard) CardLibrary.cards.get("Madness");
                } else {
                    this.cardsToPreview = this.dupeListForPrev.get(this.previewIndex);
                }
                if (this.previewIndex == this.dupeListForPrev.size() - 1) {
                    this.previewIndex = 0;
                } else {
                    this.previewIndex++;
                }
            } else {
                this.rotationTimer -= Gdx.graphics.getDeltaTime();
            }
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new YinYangAction(p, 1));
        AbstractCard c = new Indulgence();
        if (this.upgraded)
            c.upgrade();
        addToBot(new MakeTempCardInHandAction(c));
    }

    public void triggerOnGlowCheck() {
        triggerOnGlowCheck_Fiend();
    }

    public void updateFiend() {
        this.selfRetain = true;
        UpdateDescription();
        initializeDescription();
    }

    public void exitFiend() {
        this.selfRetain = false;
        UpdateDescription();
        initializeDescription();
    }

    public AbstractDoubleCard makeCopy() {
        return new YinYang();
    }
}