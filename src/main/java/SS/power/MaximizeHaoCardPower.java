package SS.power;

import SS.action.common.DrawSpecificCardAction;
import SS.action.common.EchoACardAction;
import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;

public class MaximizeHaoCardPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("MaximizeHaoCardPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private int cnt = 0;
    private AbstractCard last = null;

    public MaximizeHaoCardPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = -1;

        String path128 = "img/power/MaximizeHaoCardPower84.png";
        String path48 = "img/power/MaximizeHaoCardPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + last + DESCRIPTIONS[1] + this.cnt + DESCRIPTIONS[2];
    }

    public void onUseCard(AbstractCard card, UseCardAction action) {
        addToBot(new DrawSpecificCardAction(card, 1));
        if (last == null) {
            cnt = 1;
            last = card;
        } else {
            if (card.cardID.equals(last.cardID))
                ++cnt;
            else {
                cnt = 1;
                last = card;
            }
        }
        if (cnt == 3) {
            flash();
            action.exhaustCard = true;
            addToBot(new EchoACardAction(card, true));
        }
        updateDescription();
    }

}
