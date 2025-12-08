package SS.power;

import SS.cards.PeptideStrike;
import SS.helper.ModHelper;
import SS.path.DamageInfoEnum;

import java.util.Iterator;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.BerserkPower;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.DrawPower;
import com.megacrit.cardcrawl.powers.RetainCardPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;

public class PatiencePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("PatiencePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private static int postfix = 0;

    public PatiencePower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID + (postfix++);
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/PatiencePower84.png";
        String path48 = "img/power/PatiencePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atStartOfTurn() {
        if (this.amount == 3) {
            addToBot(new ApplyPowerAction(owner, owner, new BerserkPower(owner, 1)));
            addToBot(new GainEnergyAction(1));
        }
        if (this.amount == 2) {
            addToBot(new ApplyPowerAction(owner, owner, new DrawPower(owner, 1)));
        }
        if (this.amount == 1) {
            addToBot(new ApplyPowerAction(owner, owner, new RetainCardPower(owner, 1)));
        }
        addToBot(new ReducePowerAction(this.owner, this.owner, this, 1));
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}
