package SS.Dice;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import SS.action.DiceDamageEnemyAction;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class DefendHaoDice extends AbstractDice {
    public static final String ORB_ID = ModHelper.makePath("DefendHaoDice");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    private static final float PI_DIV_16 = 0.19634955F;
    private static final float ORB_WAVY_DIST = 0.05F;
    private static final float PI_4 = 12.566371F;
    private static final float ORB_BORDER_SCALE = 1.2F;

    public DefendHaoDice(int damage, AbstractCreature m) {
        this.ID = ORB_ID;
        this.img = ImageMaster.loadImage("img/dice/DefendHaoDice.png");
        this.name = orbString.NAME;
        this.baseEvokeAmount = damage;
        this.evokeAmount = this.baseEvokeAmount;
        this.channelAnimTimer = 0.5F;
        this.angle = MathUtils.random(360.0F);
        this.myColor = CardHelper.getColor(249, 0, 0);
        this.target = m;
        this.faces = 6;
        this.tags.add(AbstractCardEnum.DefensiveDice);
        updateDescription();
    }

    public void updateDescription() {
        this.description = orbString.DESCRIPTION[0] + this.evokeAmount + orbString.DESCRIPTION[1];
    }

    public void myEvoke() {
        int block = this.evokeAmount;
        for (AbstractOrb o : AbstractDungeon.player.orbs) {
            if (o instanceof AttackHaoDice || o instanceof DefendHaoDice) {
                ++block;
            }
        }
        // System.out.println(temp);
        if (result == 1) {
            block--;
        }
        if (result == 6) {
            block = block * 3 / 2;
        }
        AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractPlayer) this.target, block));
    }

    public AbstractDice makeCopy() {
        return new DefendHaoDice(this.baseEvokeAmount, this.target);
    }
}