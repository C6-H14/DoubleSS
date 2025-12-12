package SS.Dice;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class DefendDice extends AbstractDice {
    public static final String ORB_ID = ModHelper.makePath("Defend");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    private static final float PI_DIV_16 = 0.19634955F;
    private static final float ORB_WAVY_DIST = 0.05F;
    private static final float PI_4 = 12.566371F;
    private static final float ORB_BORDER_SCALE = 1.2F;

    public DefendDice(int damage, AbstractPlayer p) {
        this.ID = ORB_ID;
        this.img = ImageMaster.loadImage("img/dice/DefendDice.png");
        this.name = orbString.NAME;
        this.baseEvokeAmount = damage;
        this.evokeAmount = this.baseEvokeAmount;
        this.channelAnimTimer = 0.5F;
        this.angle = MathUtils.random(360.0F);
        this.myColor = CardHelper.getColor(249, 0, 0);
        this.target = p;
        this.faces = 6;
        this.result = getDiceResult();
        this.tags.add(AbstractCardEnum.DefensiveDice);
        updateDescription();
    }

    public void updateDescription() {
        this.description = orbString.DESCRIPTION[0] + this.evokeAmount + orbString.DESCRIPTION[1];
    }

    public void myEvoke() {
        int block = this.evokeAmount;
        // System.out.println(temp);
        if (result == 1) {
            block--;
            /*
             * AbstractDungeon.actionManager
             * .addToBottom(new ApplyPowerAction(this.target, this.target, new
             * DexterityPower(this.target, 1), 1));
             * AbstractDungeon.actionManager.addToBottom(
             * new ApplyPowerAction(this.target, this.target, new
             * LoseDexterityPower(this.target, 1), 1));
             */
        }
        if (result == 6) {
            block = block * 3 / 2;
        }
        AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractPlayer) this.target, block));
    }

    public AbstractDice makeCopy() {
        return new DefendDice(this.baseEvokeAmount, (AbstractPlayer) this.target);
    }
}