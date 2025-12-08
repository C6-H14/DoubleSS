package SS.power;

import SS.action.EchoACardAction;
import SS.helper.ModHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;

public class BlankCardPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("BlankCardPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public BlankCardPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.DEBUFF;

        this.amount = amount;

        String path128 = "img/power/BlankCardPower84.png";
        String path48 = "img/power/BlankCardPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    public void atStartOfTurn() {
        for (int i = 0; i < this.amount; ++i) {
            AbstractCard c = getRandomCard();
            if (c == null) {
                break;
            }
            addToBot(new EchoACardAction(c));
        }
    }

    private AbstractCard getRandomCard() {
        ArrayList<AbstractCard> tmp = new ArrayList();
        Iterator var4 = AbstractDungeon.player.exhaustPile.group.iterator();

        while (var4.hasNext()) {
            AbstractCard c = (AbstractCard) var4.next();
            if (c.type != CardType.STATUS && c.type != CardType.CURSE && c.isEthereal) {
                tmp.add(c);
            }
        }

        if (tmp.isEmpty()) {
            return null;
        } else {
            Collections.sort(tmp);
            return (AbstractCard) tmp.get(AbstractDungeon.cardRng.random(tmp.size() - 1));
        }
    }

}
