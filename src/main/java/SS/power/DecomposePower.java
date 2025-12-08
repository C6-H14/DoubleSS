package SS.power;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.helper.ModHelper;

public class DecomposePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("DecomposePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private AbstractPlayer p;

    public DecomposePower(AbstractPlayer owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.p = owner;
        this.owner = (AbstractCreature) owner;
        this.type = AbstractPower.PowerType.BUFF;
        this.priority = 6;

        this.amount = -1;

        String path128 = "img/power/DecomposePower84.png";
        String path48 = "img/power/DecomposePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atStartOfTurn() {
        for (AbstractCard c : p.drawPile.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToBot(new ExhaustSpecificCardAction(c, p.drawPile));
            }
        }
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToBot(new ExhaustSpecificCardAction(c, p.hand));
            }
        }
        for (AbstractCard c : p.discardPile.group) {
            if (c.type == AbstractCard.CardType.STATUS) {
                addToBot(new ExhaustSpecificCardAction(c, p.discardPile));
            }
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

}
