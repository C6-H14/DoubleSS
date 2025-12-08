package SS.power;

import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;

public class PhoneItInPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("PhoneItInPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private int count = 0;

    public PhoneItInPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/PhoneItInPower84.png";
        String path48 = "img/power/PhoneItInPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atStartOfTurn() {
        this.count = 0;
    }

    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (!card.exhaust && this.count < this.amount && card.type != AbstractCard.CardType.POWER) {
            flash();
            action.exhaustCard = true;
            ++this.count;
            addToBot(new MakeTempCardInHandAction(CardLibrary.getCard(card.cardID)));
            addToBot(new GainEnergyAction(1));
            if (!card.upgraded) {
                addToBot(new ApplyPowerAction(this.owner, this.owner, new CrimePower(this.owner, 2), 2));
            }
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    public boolean CanPlayCardFree() {
        return this.count <= this.amount;
    }

}
