package SS.Dice;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.powers.BufferPower;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.NextTurnDamagePower;

public class EternalAttackDice extends AbstractDice {
    public static final String ORB_ID = ModHelper.makePath("EternalAttackDice");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    private static final float PI_DIV_16 = 0.19634955F;
    private static final float ORB_WAVY_DIST = 0.05F;
    private static final float PI_4 = 12.566371F;
    private static final float ORB_BORDER_SCALE = 1.2F;

    public EternalAttackDice(int damage, AbstractPlayer p) {
        this.ID = ORB_ID;
        this.img = ImageMaster.loadImage("img/dice/EternalAttackDice.png");
        this.name = orbString.NAME;
        this.baseEvokeAmount = damage;
        this.evokeAmount = this.baseEvokeAmount;
        this.channelAnimTimer = 0.5F;
        this.angle = MathUtils.random(360.0F);
        this.myColor = CardHelper.getColor(249, 0, 0);
        this.target = p;
        this.faces = 6;
        this.result = getDiceResult();
        this.tags.add(AbstractCardEnum.AggresiveDice);
        updateDescription();
    }

    public void updateDescription() {
        this.description = orbString.DESCRIPTION[0] + this.evokeAmount + orbString.DESCRIPTION[1];
    }

    public void myEvoke() {
        int block = this.evokeAmount;
        AbstractPlayer p = AbstractDungeon.player;
        if (result == 1) {
            if (AbstractDungeon.player.currentBlock > 0) {
                AbstractDungeon.actionManager.addToBottom(
                        new DamageAction(p, new DamageInfo((AbstractCreature) p, 1, DamageInfo.DamageType.NORMAL)));
            }
        }
        if (result == 6) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new BufferPower(p, 1)));
        }
        AbstractDungeon.actionManager
                .addToBottom(new ApplyPowerAction(p, p, new NextTurnDamagePower(p, this.evokeAmount)));
    }

    public AbstractDice makeCopy() {
        return new EternalAttackDice(this.baseEvokeAmount, (AbstractPlayer) this.target);
    }
}