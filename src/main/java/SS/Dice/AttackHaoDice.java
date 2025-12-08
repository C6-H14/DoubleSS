package SS.Dice;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.PoisonPower;

import SS.action.dice.DiceDamageEnemyAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class AttackHaoDice extends AbstractDice {
    public static final String ORB_ID = ModHelper.makePath("AttackHaoDice");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    private static final float PI_DIV_16 = 0.19634955F;
    private static final float ORB_WAVY_DIST = 0.05F;
    private static final float PI_4 = 12.566371F;
    private static final float ORB_BORDER_SCALE = 1.2F;

    public AttackHaoDice(int damage, AbstractMonster m) {
        this.ID = ORB_ID;
        this.img = ImageMaster.loadImage("img/dice/AttackHaoDice.png");
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
        this.description = orbString.DESCRIPTION[0] + this.evokeAmount + orbString.DESCRIPTION[1] + this.evokeAmount
                + orbString.DESCRIPTION[2];
    }

    public void myEvoke() {
        int temp = result, damage = this.evokeAmount, poi = this.evokeAmount;
        for (AbstractOrb o : AbstractDungeon.player.orbs) {
            if (o instanceof AttackHaoDice || o instanceof DefendHaoDice) {
                ++damage;
                ++poi;
            }
        }
        if (temp == 1) {
            damage++;
            poi--;
        }
        if (temp == 6) {
            damage++;
            poi++;
        }
        AbstractDungeon.actionManager
                .addToBottom(new DiceDamageEnemyAction(damage, (AbstractMonster) this.target, false));
        AbstractDungeon.actionManager
                .addToBottom(new ApplyPowerAction((AbstractMonster) this.target, AbstractDungeon.player,
                        new PoisonPower((AbstractMonster) this.target, AbstractDungeon.player, poi), poi));
    }

    public AbstractDice makeCopy() {
        return new AttackHaoDice(this.baseEvokeAmount, (AbstractMonster) this.target);
    }
}