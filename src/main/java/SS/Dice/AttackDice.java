package SS.Dice;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.dice.DiceDamageEnemyAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class AttackDice extends AbstractDice {
    public static final String ORB_ID = ModHelper.makePath("Attack");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    private static final float PI_DIV_16 = 0.19634955F;
    private static final float ORB_WAVY_DIST = 0.05F;
    private static final float PI_4 = 12.566371F;
    private static final float ORB_BORDER_SCALE = 1.2F;

    public AttackDice(int damage, AbstractMonster m) {
        this.ID = ORB_ID;
        this.img = ImageMaster.loadImage("img/dice/AttackDice.png");
        this.name = orbString.NAME;
        this.baseEvokeAmount = damage;
        this.evokeAmount = this.baseEvokeAmount;
        this.channelAnimTimer = 0.5F;
        this.angle = MathUtils.random(360.0F);
        this.myColor = CardHelper.getColor(249, 0, 0);
        this.target = m;
        this.faces = 6;
        this.tags.add(AbstractCardEnum.AggresiveDice);
        updateDescription();
    }

    public void updateDescription() {
        this.description = orbString.DESCRIPTION[0] + this.evokeAmount + orbString.DESCRIPTION[1];
    }

    public void myEvoke() {
        boolean flag = false;
        int temp = result, damage = this.evokeAmount;
        // System.out.println(temp);
        if (temp == 1) {
            damage--;
        }
        if (temp == 5) {
            flag = true;
        }
        if (temp == 6) {
            damage = damage * 3 / 2;
        }
        AbstractDungeon.actionManager
                .addToBottom(new DiceDamageEnemyAction(damage, (AbstractMonster) this.target, flag));
    }

    public AbstractDice makeCopy() {
        return new AttackDice(this.baseEvokeAmount, (AbstractMonster) this.target);
    }
}