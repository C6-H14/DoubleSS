package SS.power;

import SS.action.common.PlayCardAction;
import SS.helper.ModHelper;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.cards.AbstractCard;

public class InscribeCardPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("InscribeCardPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    public AbstractCard card;

    public InscribeCardPower(AbstractCreature owner, final AbstractCard card) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;
        this.card = card;
        this.amount = -1;

        String path128 = "img/power/InscribeCardPower84.png";
        String path48 = "img/power/InscribeCardPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + (this.card == null ? 114514 : this.card.name) + DESCRIPTIONS[1];
    }

    public void setCard(AbstractCard card) {
        this.card = card;
        // 可以在这里更新 Power 的 description，显示当前存了什么牌
        this.updateDescription();
    }

    public void playCard() {
        if (this.card != null) {
            addToBot(new PlayCardAction((AbstractCreature) (AbstractDungeon.getCurrRoom()).monsters
                    .getRandomMonster(null, true, AbstractDungeon.cardRandomRng), this.card, false));
        }
        this.card = null;
        this.updateDescription();
    }

    @Override
    public void renderAmount(SpriteBatch sb, float x, float y, Color c) {
        return;
    }
}
