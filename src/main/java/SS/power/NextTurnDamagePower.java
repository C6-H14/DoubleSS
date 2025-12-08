package SS.power;

import SS.helper.ModHelper;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class NextTurnDamagePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("NextTurnDamagePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public NextTurnDamagePower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/NextTurnDamagePower84.png";
        String path48 = "img/power/NextTurnDamagePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atStartOfTurn() {
        this.flash();
        addToBot(new DamageAllEnemiesAction((AbstractPlayer) owner, this.amount, DamageInfo.DamageType.THORNS,
                GetEffect(amount)));
        addToBot(new RemoveSpecificPowerAction(owner, owner, this));
    }

    private AbstractGameAction.AttackEffect GetEffect(int dmg) {
        if (dmg <= 2)
            return AbstractGameAction.AttackEffect.NONE;
        if (dmg <= 6)
            return AbstractGameAction.AttackEffect.BLUNT_LIGHT;
        return AbstractGameAction.AttackEffect.BLUNT_HEAVY;
    }

    public int onAttacked(final DamageInfo info, final int damageAmount) {
        if (damageAmount <= 0)
            return 0;
        addToBot(new RemoveSpecificPowerAction(owner, owner, this));
        return damageAmount;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}